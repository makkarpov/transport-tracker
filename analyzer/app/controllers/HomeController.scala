package controllers

import java.io._
import java.nio.file.Files
import java.security.MessageDigest
import java.time.Instant
import javax.inject.Inject
import javax.xml.bind.DatatypeConverter

import com.vividsolutions.jts.geom.LineString
import controllers.HomeController.{GRSearchResult, USearchResult}
import play.api.mvc.{Action, Controller, Request, RequestHeader}
import play.api.libs.json.Json
import ru.makkarpov.ttanalyze.Context
import ru.makkarpov.ttanalyze.Context._
import ru.makkarpov.ttanalyze.analyze.{AnalyzedTrack, TrackPoint, XAnalyzer}
import ru.makkarpov.ttanalyze.db.models._
import ru.makkarpov.ttanalyze.utils.GeoUtils
import controllers.UserController._

import scala.concurrent.Future

/**
  * Created by user on 7/14/16.
  */
object HomeController {
  case class USearchResult(dist: Double, exit: UndergroundExit, station: UndergroundStation, line: UndergroundLine)
  case class GRSearchResult(dist: Double, route: GroundRoute)
  case class GSSearchResult(dist: Double, stop: Nothing)
}

class HomeController @Inject()(analyzer: XAnalyzer)(implicit ctx: Context) extends Controller {
  import ctx.db.api._

  private def parseCoords(s: String): Option[(Double, Double)] =
    try {
      val Array(a, b) = s.split(",", 2)
      Some((a.trim.toDouble, b.trim.toDouble))
    } catch {
      case e: Exception => None
    }

  def underground_search(q: Option[String]) = fetchUser.async { implicit rq =>
    q.flatMap(parseCoords) match {
      case None => Future.successful(Ok(views.html.search.underground(q, Nil)))
      case Some((lat, lng)) =>
        ctx.db(
          UndergroundExit.query
            .map(x => (x, x.coordinates.distance(GeoUtils.latLng(lat, lng))))
            .join(UndergroundStation.query).on {
              case ((exit, _), station) => exit.stationId === station.id
            }.map {
              case ((exit, dist), station) => (dist, exit, station)
            }.join(UndergroundLine.query).on {
              case ((_, _, station), line) => station.lineId === line.id
            }.sortBy {
              case ((dist, _, _), _) => dist.asc
            }.take(10).result.map(_.map {
              case ((dist, exit, station), line) => USearchResult(dist, exit, station, line)
            })
        ) map { res =>
          Ok(views.html.search.underground(q, res))
        }
    }
  }

  def ground_search(q: Option[String]) = fetchUser.async { implicit rq =>
    q.flatMap(parseCoords) match {
      case None => Future.successful(Ok(views.html.search.ground(q, Nil)))
      case Some((lat, lng)) =>
        val point = GeoUtils.latLng(lat, lng)
        ctx.db(
          GroundRoute.query
            .map(x => (x, x.forwardPath.distance(point) min x.backwardPath.distance(point)))
            .sortBy(_._2).take(10).result.map(_.map {
            case (route, dist) => GRSearchResult(dist, route)
            })
        ) map { res =>
          Ok(views.html.search.ground(q, res))
        }
    }
  }

  def route(id: Int) = fetchUser.async { implicit rq =>
    for {
      rt <- ctx.db(GroundRoute.query.filter(_.id === id).result.map(_.head))
      stops <- ctx.db(
        GroundRouteStop.query.join(GroundStop.query).on((x, y) => x.stopId === y.id)
          .filter { case (rs, _) => rs.routeId === id }.result
      )
    } yield {
      def path(ls: LineString) = (0 until ls.getNumPoints).map(ls.getPointN)
          .map(p => Json.obj("lat" -> p.getY, "lng" -> p.getX))

      val json = Json.obj(
        "forward" -> path(rt.forwardPath),
        "backward" -> path(rt.backwardPath),
        "stops" -> Json.arr()
      )

      Ok(views.html.search.view_route(json, rt, stops))
    }
  }

  def draw = fetchUser.apply { implicit rq => Ok(views.html.draw()) }

  def index = fetchUser.apply { implicit rq => Ok(views.html.index()) }

  def analyze = fetchUser.async(parse.multipartFormData) { implicit rq =>
    rq.body.file("points") match {
      case Some(file) =>
        analyzeTrack(file.ref.file) map { at =>
          Ok(views.html.analyzed(at))
        }
      case None => Future.successful(BadRequest("No points.bin file"))
    }
  }

  def api = fetchUser.async(parse.maxLength(128 * 1024, parse.temporaryFile)) { implicit rq => rq.body match {
    case Left(maxSizeExceeded) =>
      Future.successful(BadRequest(Json.obj(
        "success" -> false,
        "error" -> Json.obj(
          "code" -> "badRequest.maxSizeExceeded",
          "message" -> "Max file size exceeded",
          "maxLength" -> maxSizeExceeded.length
        )
      )))
    case Right(tmpFile) =>
       analyzeTrack(tmpFile.file) map { at =>
        Ok(Json.obj(
          "success" -> true,
          "result" -> at.apiJson
        ))
      }
  } }

  def submitted(idOpt: Option[Int]) = withRights(PermReadTracks).async { implicit rq => idOpt match {
    case None =>
      SubmittedTrack.findTracks map { d =>
        Ok(views.html.submitted(d))
      }

    case Some(id) =>
      SubmittedTrack.fetchTrack(id) flatMap {
        case None => Future.successful(Redirect(routes.HomeController.submitted(None)))
        case Some(r) =>
          val points = readTrack(new ByteArrayInputStream(r.track))
          analyzer.analyze(points, r.sha256) map { at =>
            Ok(views.html.analyzed(at))
          }
      }
  } }

  private def readTrack(is: InputStream): Seq[TrackPoint] = {
    val dis = new DataInputStream(is)
    val points = Vector.newBuilder[TrackPoint]
    try {
      while (true)
        points += TrackPoint.read(dis)
    } catch {
      case _: EOFException =>
    } finally is.close()

    points.result()
  }

  private def analyzeTrack(f: File)(implicit rq: RequestHeader): Future[AnalyzedTrack] = {
    val bytes = Files.readAllBytes(f.toPath)
    val hash = DatatypeConverter.printHexBinary(MessageDigest.getInstance("SHA-256").digest(bytes)).toLowerCase()
    val points = readTrack(new ByteArrayInputStream(bytes))

    ctx.db(
      sqlu"""
        INSERT INTO "submitted_tracks" VALUES (DEFAULT, ${rq.remoteAddress}::inet, 1, $hash, $bytes,
           ${Instant.now.toString}::timestamp without time zone)
        ON CONFLICT ("sha256") DO NOTHING
      """
    ) flatMap { _ =>
      analyzer.analyze(points, hash)
    }
  }
}
