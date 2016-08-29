package ru.makkarpov.ttdroid.stats

import android.app.Activity
import ru.makkarpov.ttdroid.{R, FileListActivity}
import ru.makkarpov.ttdroid.data.AnalyzedTrack._
import ru.makkarpov.ttdroid.data.{TrackFiles, TrackHeader}
import upickle.{Invalid, Js}
import upickle.Js.Value

import upickle.default._
import ru.makkarpov.ttdroid.data.AnalyzedTrack.{kindReader, kindWriter}

/**
  * Created by user on 8/8/16.
  */
object TrackGrouper {
  // Groups tracks with similar fragments (walks, bus with same route, underground with same stations)
  // Excludes volatile data such as point numbers, times and so on.

  sealed trait GroupEntry
  case class Ground(route: GroundRoute, trip: GroundTrip) extends GroupEntry
  case class Underground(trip: UndergroundTrip) extends GroupEntry

  // Shitty uPickle
  implicit val entryPickler = new Writer[GroupEntry] with Reader[GroupEntry] {
    private def write[T: Writer](tpe: String, obj: T): Js.Value =
      Js.Obj((writeJs[T](obj) match {
        case o: Js.Obj => o.value :+ ("fragmentType" -> Js.Str(tpe))
        case _ => throw new IllegalArgumentException("written data is not Js.Obj")
      }):_*)

    override def write0 = {
      case g: Ground => write("ground", g)
      case u: Underground => write("underground", u)
    }

    override def read0 = {
      case o: Js.Obj =>
        o.obj("type") match {
          case Js.Str("ground") => readJs[Ground](o)
          case Js.Str("underground") => readJs[Underground](o)
          case _ => throw Invalid.Data(o, "unknown type")
        }
    }
  }

  trait GroupKey extends Serializable

  case class RouteGroupKey(entries: Seq[GroupEntry]) extends GroupKey {
    def name(ctx: Activity): String = {
      val name = entries.map {
        case g: TrackGrouper.Ground =>
          val k = g.route.kind match {
            case GroundKind.Bus => ctx.getString(R.string.group_bus)
            case GroundKind.Trolleybus => ctx.getString(R.string.group_trolleybus)
            case GroundKind.Tram => ctx.getString(R.string.group_tram)
            case GroundKind.Monorail => ctx.getString(R.string.group_monorail)
          }

          k + g.route.index

        case u: TrackGrouper.Underground => ctx.getString(R.string.group_underground)
      }.mkString(", ")

      if (name.isEmpty) ctx.getString(R.string.group_walks) else name
    }
  }

  case class PlaceGroupKey(start: String, finish: String) extends GroupKey

  implicit val keyPickler = macroRW[RouteGroupKey]

  case class MyRoute(id: Int, name: String, key: RouteGroupKey)

  private def getRoute(th: TrackFiles): RouteGroupKey =
    RouteGroupKey(th.header.analyze.get.fragments.collect {
      case g: GroundFragment => Ground(g.route, g.stations)
      case u: UndergroundFragment => Underground(u.stations)
    })

  private def getPlace(th: TrackFiles): PlaceGroupKey = {
    val as = th.header.autoStart.get
    PlaceGroupKey(as.startPlace.name, as.finishPlace.get.name)
  }

  def groupRoutes(files: Set[TrackFiles]): Map[RouteGroupKey, Set[TrackFiles]] =
    files
      .filter(_.header.analyze.isDefined)
      .filter(_.header.finishTime.isDefined)
      .groupBy(getRoute)

  def groupPlaces(files: Set[TrackFiles]): Map[PlaceGroupKey, Set[TrackFiles]] =
    files
      .filter(_.header.autoStart.isDefined)
      .filter(_.header.autoStart.get.finishPlace.isDefined)
      .groupBy(getPlace)

  def groupedPlaces = groupPlaces(FileListActivity.allFiles.toSet)
  def groupedRoutes = groupRoutes(FileListActivity.allFiles.toSet)

  def getGroup(g: GroupKey): Set[TrackFiles] = g match {
    case r: RouteGroupKey => groupedRoutes(r)
    case r: PlaceGroupKey => groupedPlaces(r)
  }
}
