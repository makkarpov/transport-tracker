package ru.makkarpov.ttanalyze.analyze

import javax.inject.{Inject, Singleton}

import ru.makkarpov.ttanalyze.Context
import XAnalyzer._
import com.vividsolutions.jts.geom.{Coordinate, GeometryFactory, Point, Polygon}
import play.api.Logger
import ru.makkarpov.ttanalyze.analyze.TrackPoint.PointType
import ru.makkarpov.ttanalyze.db.models.{GroundRouteStop, GroundStop, UndergroundExit, UndergroundStation}
import ru.makkarpov.ttanalyze.utils.GeoUtils
import ru.makkarpov.ttanalyze.Context._
import ru.makkarpov.ttanalyze.analyze.TrackFragment.FragmentMetadata
import ru.makkarpov.ttanalyze.utils.CollectionUtils._

import scala.concurrent.Future
import scala.util.Success

/**
  * Created by user on 8/3/16.
  */
object XAnalyzer {
  type StationType = StationType.Value
  object StationType extends Enumeration {
    val Ground = Value
    val Underground = Value
  }

  case class StationReference(id: Int, coord: Polygon, tpe: StationType) {
    override def toString: String = s"StationReference($id, ..., $tpe)"
  }

  case class StationCollection(refs: Set[StationReference]) {
    def nonEmpty = refs.nonEmpty
    def intersect(x: StationCollection) = StationCollection(refs.intersect(x.refs))
    def ids = refs.map(_.id)

    def hasUnderground = refs.exists(_.tpe == StationType.Underground)
    def hasGround = refs.exists(_.tpe == StationType.Ground)

    def underground = StationCollection(refs.filter(_.tpe == StationType.Underground))
    def ground = StationCollection(refs.filter(_.tpe == StationType.Ground))
  }
}

@Singleton
class XAnalyzer @Inject()(implicit ctx: Context) {
  import ctx.db.api._

  val log = Logger("app.xanalyzer")

  val StationsSearchRadius = 50.0
  val StationsFetchRadius = StationsSearchRadius * 2
  val GPSLostThreshold = 300.0
  val PedestrianWindow = 8
  val PedestrianSpeed = 15.0 / 3.6
  val PedestrianGradientThreshold = 0.15
  val PedestrianGradientEps = 0.1
  val StopWaitRadius = 70.0

  val WalkPedestrianPercent = 0.7

  def analyze(inputTrack: Seq[TrackPoint], hash: String): Future[AnalyzedTrack] = {
    val time = System.currentTimeMillis()
    log.info(s"Starting analysis of '$hash', ${inputTrack.size} points")
    for {
      stations <- fetchNearbyStations(inputTrack)
      processedTrack = mergeGpsLosses(inputTrack)
      rawFragments <- splitByPedestrian(processedTrack).flatMapFuture(handleFragment(processedTrack, _, stations))
      fragments = mergeWalks(rawFragments)
      metadata = fragments.map(calculateMetadata(processedTrack, _))
    } yield {
      log.info(s"Analysis completed in ${System.currentTimeMillis() - time} ms.")
      AnalyzedTrack(inputTrack, fragments, metadata, hash)
    }
  }

  private def mergeGpsLosses(track: Seq[TrackPoint]): Seq[TrackPoint] = {
    val ret = Seq.newBuilder[TrackPoint]

    var i = 0
    while (i < track.size)
      if ((track(i).tpe == PointType.GPSLost) && (i < track.size - 1) &&
          (track(i).distance(track(i + 1)) < GPSLostThreshold))
      {
        ret += track(i).copy(tpe = PointType.Regular)
        ret += track(i + 1).copy(tpe = PointType.Regular)

        log.info(f"Merging GPS loss at $i .. ${i + 1} (${track(i).distance(track(i + 1))}%.2f meters)")

        i += 2
      } else {
        ret += track(i)
        i += 1
      }

    ret.result()
  }

  private def fetchNearbyStations(track: Seq[TrackPoint]): Future[Set[StationReference]] = {
    val KindUnderground = 0
    val KindGround = 1

    ctx.db(UndergroundExit.query.map(x => (x.coordinates, KindUnderground, x.id))
      .union(GroundStop.query.map(x => (x.coordinates, KindGround, x.id)))
      .filter(_._1.dWithin(GeoUtils.lineString(track), StationsFetchRadius))
      .result.map(_.map {
        case (p, tIdx, i) =>
          val tpe = tIdx match {
            case KindUnderground => StationType.Underground
            case KindGround => StationType.Ground
          }
          StationReference(i, p, tpe)
      }.toSet)).andThen {
        case Success(s) => log.info(s"Fetched stations: $s")
      }
  }

  private def splitByPedestrian(track: Seq[TrackPoint]): Seq[TrackFragment.Raw] = {
    def pedestrianPercent(start: Int, end: Int): Double = {
      val range = (start max 0) to (end min track.size - 1)
      range.count(x => {
        val p = track(x)
        (p.movement == Movement.Pedestrian) && (p.speed <= PedestrianSpeed)
      }).toDouble / range.size
    }

    def pedestrianGradient(idx: Int): Double = {
      val isGpsLostPoint = track(idx).tpe match {
        case PointType.GPSAcquired => idx > 0
        case PointType.GPSLost => idx < track.size - 1
        case _ => false
      }

      if (isGpsLostPoint) {
        val other = if (track(idx).tpe == PointType.GPSLost) idx + 1 else idx - 1

        if (track(idx).movement != track(other).movement) Double.PositiveInfinity
        else 0
      } else {
        val backward = pedestrianPercent(idx - PedestrianWindow, idx)
        val forward = pedestrianPercent(idx, idx + PedestrianWindow)

        (backward - forward).abs
      }
    }

    val grads = track.indices.map(pedestrianGradient)
    val ret = Vector.newBuilder[TrackFragment.Raw]

    println(s"Pedestrian gradients: ${grads.zipWithIndex.map{ case (g, i) => f"$i: $g%.2f" }.mkString(", ")}")

    var idx = 0
    for {
      i <- 1 until (track.size - 1) if grads(i).isPosInfinity || // infinite gradient == definitely split
       ((grads(i) >= grads(i + 1) || (track(i).tpe == PointType.GPSLost)) &&
        (grads(i) >  grads(i - 1) || (track(i).tpe == PointType.GPSAcquired)) &&
        (grads(i) > PedestrianGradientThreshold || (track(i).tpe != PointType.Regular)))
    } {
      // find equal values ahead
      val eqAhead =
        if (grads(i).isPosInfinity) 0 // infinite gradients does not merge with each other
        else (i until (track.size - 1)).count(j => (grads(i) - grads(j)).abs < PedestrianGradientEps)

      val j = i + eqAhead / 2
      ret += TrackFragment.Raw(idx, j)
      idx = j
    }

    if (idx < track.size - 1)
      ret += TrackFragment.Raw(idx, track.size - 1)

    ret.result()
  }

  private def handleFragment(track: Seq[TrackPoint], frag: TrackFragment.Raw,
                             stations: Set[StationReference]): Future[Seq[TrackFragment]] =
  {
    val points = track.view.slice(frag.startPoint, frag.endPoint + 1)

    if (points.isEmpty)
      return Future.successful(Nil)

    log.info(s"Analyzing fragment ${frag.startPoint} .. ${frag.endPoint}:")

    val totalDistance = points.biMap((a, b) => GeoUtils.distance(a.lat, a.lng, b.lat, b.lng)).sum
    val pedestrianPercent = (points.biMap {
      case (a, b) if a.movement == Movement.Pedestrian && b.movement == Movement.Pedestrian =>
        GeoUtils.distance(a.lat, a.lng, b.lat, b.lng)
      case _ => 0.0
    }.sum / totalDistance).zeroIfNanOrInf

    if (pedestrianPercent >= WalkPedestrianPercent) {
      log.info(f"Pedestrian percent is $pedestrianPercent (threshold $WalkPedestrianPercent), treating as pedestrian.")
      return Future.successful(Seq(TrackFragment.Walk(frag.startPoint, frag.endPoint)))
    }

    val startStations = findStations(points.head, stations, "start")
    val endStations = findStations(points.last, stations, "end")

    val commonStations = startStations.intersect(endStations)
    if (commonStations.nonEmpty)
      return Future.successful(Seq(TrackFragment.SameStation(frag.startPoint, frag.endPoint, commonStations)))

    if (startStations.hasUnderground && endStations.hasUnderground)
      return UndergroundStation.findStations(startStations.underground, endStations.underground) map {
        case (enter, exit) => TrackFragment.Underground(frag.startPoint, frag.endPoint, enter, exit) :: Nil
      }

    if (startStations.hasGround && endStations.hasGround) {
      // Find all points that are within certain range of start point, and consider them as waiting.
      val idx = (frag.startPoint to 0 by -1).find(i => track(i).distance(points.head) > StopWaitRadius)
      val wait = idx.map(track(_).time).map(points.head.time - _).getOrElse(0L)

      return GroundRouteStop.findRoute(startStations.ground, endStations.ground) map {
        case Seq((route, enter, exit), _*) =>
          TrackFragment.Bus(frag.startPoint, frag.endPoint, route, enter, exit, wait) :: Nil
        case _ => frag :: Nil
      }
    }

    Future.successful(frag :: Nil)
  }

  private def findStations(point: TrackPoint, stations: Set[StationReference], lbl: String) = {
    log.info(s"Searching for $lbl stations at $point:")
    StationCollection(stations.filter(x => {
      val r = GeoUtils.distance(point.jtsPoint, x.coord)
      log.info(s" ... dist from $x to ${point.jtsPoint} is $r (threshold $StationsSearchRadius)")
      r <= StationsSearchRadius
    }))
  }

  private def mergeWalks(f: Seq[TrackFragment]): Seq[TrackFragment] = {
    val ret = Vector.newBuilder[TrackFragment]

    val iter = f.map {
      case TrackFragment.SameStation(st, sp, _) => TrackFragment.Walk(st, sp)
      case x => x
    }.iterator.buffered

    while (iter.hasNext) {
      var x = iter.next()

      if (x.isInstanceOf[TrackFragment.Walk])
        while (iter.hasNext && iter.head.isInstanceOf[TrackFragment.Walk])
          x = TrackFragment.Walk(x.startPoint, iter.next().endPoint)

      if (x.isInstanceOf[TrackFragment.Raw])
        while (iter.hasNext && iter.head.isInstanceOf[TrackFragment.Raw])
          x = TrackFragment.Raw(x.startPoint, iter.next().endPoint)

      ret += x
    }

    ret.result()
  }

  private def calculateMetadata(track: Seq[TrackPoint], frag: TrackFragment): FragmentMetadata = {
    val points = track.view.slice(frag.startPoint, frag.endPoint)

    if (points.isEmpty)
      return FragmentMetadata(0L, 0, 0, 0, 0)

    if (points.size == 1)
      return FragmentMetadata(0L, 0, 0, 0, 0)

    val time = points.last.time - points.head.time
    val totalDistance = points.biMap((a, b) => GeoUtils.distance(a.lat, a.lng, b.lat, b.lng)).sum
    val movement = GeoUtils.distance(points.head.lat, points.head.lng, points.last.lat, points.last.lng)
    val avgSpeed = points.map(_.speed).sum / points.size
    val avgMSpeed = movement / time * 1000

    FragmentMetadata(time, totalDistance, movement, avgSpeed, avgMSpeed)
  }
}
