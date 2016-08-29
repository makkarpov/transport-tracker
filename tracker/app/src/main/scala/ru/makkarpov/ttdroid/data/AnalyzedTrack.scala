package ru.makkarpov.ttdroid.data

import ru.makkarpov.ttdroid.data.AnalyzedTrack.TrackFragment
import ru.makkarpov.ttdroid.utils.Utils
import upickle.Js.Value
import upickle.default._
import upickle.{Invalid, Js}

/**
  * Created by user on 8/4/16.
  */
object AnalyzedTrack {
  trait TrackFragment {
    def startPoint: Int
    def endPoint: Int
    def metadata: FragmentMetadata
  }

  case class FragmentMetadata(time: Int = 0, distance: Double, movement: Double, avgSpeed: Double = 0,
                              avgMovementSpeed: Double = 0)

  case class UndergroundLine(name: String, index: String, fgColor: Int, bgColor: Int)
  case class UndergroundStation(station: String, line: UndergroundLine)
  case class UndergroundTrip(enter: UndergroundStation, exit: UndergroundStation)
  case class UndergroundFragment(startPoint: Int, endPoint: Int, metadata: FragmentMetadata,
                                 stations: UndergroundTrip) extends TrackFragment

  implicit val kindReader = Utils.enumerationReader(GroundKind)
  implicit val kindWriter = Utils.enumerationWriter(GroundKind)
  object GroundKind extends Enumeration {
    val Bus = Value("bus")
    val Trolleybus = Value("trolleybus")
    val Tram = Value("tram")
    val Monorail = Value("monorail")
  }

  case class GroundStop(name: String)
  case class GroundTrip(enter: GroundStop, exit: GroundStop)
  case class GroundRoute(index: String, kind: GroundKind.Value)
  case class GroundFragment(startPoint: Int, endPoint: Int, metadata: FragmentMetadata, route: GroundRoute,
                            stations: GroundTrip, waitTime: Int = 0) extends TrackFragment

  case class WalkFragment(startPoint: Int, endPoint: Int, metadata: FragmentMetadata)
    extends TrackFragment

  case class RawFragment(startPoint: Int, endPoint: Int, metadata: FragmentMetadata)
    extends TrackFragment

  implicit val fragmentReader = Reader[TrackFragment] {
    case obj: Js.Obj =>
      val tpe = obj.value.collectFirst { case ("fragmentType", Js.Str(x)) => x }
                .getOrElse(throw Invalid.Data(obj, "type field is absent"))

      tpe match {
        case "walk" => readJs[WalkFragment](obj)
        case "underground" => readJs[UndergroundFragment](obj)
        case "bus" => readJs[GroundFragment](obj)
        case "raw" => readJs[RawFragment](obj)
      }
  }

  implicit val fragmentWriter = new Writer[TrackFragment] {
    private def write[T: Writer](tpe: String, obj: T): Js.Value =
      Js.Obj((writeJs[T](obj) match {
        case o: Js.Obj => o.value :+ ("fragmentType" -> Js.Str(tpe))
        case _ => throw new IllegalArgumentException("written data is not Js.Obj")
      }):_*)

    override def write0 = {
      case w: WalkFragment => write("walk", w)
      case g: GroundFragment => write("bus", g)
      case u: UndergroundFragment => write("underground", u)
      case r: RawFragment => write("raw", r)
    }
  }

  private case class APIHeader(success: Boolean, result: AnalyzedTrack)

  def readFromAPI(s: String): AnalyzedTrack = read[APIHeader](s).result
}

case class AnalyzedTrack(fragments: Seq[TrackFragment])
