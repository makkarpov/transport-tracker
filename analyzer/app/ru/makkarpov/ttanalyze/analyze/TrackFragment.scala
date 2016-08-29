package ru.makkarpov.ttanalyze.analyze

import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json.{JsValue, Json, Writes}
import ru.makkarpov.ttanalyze.analyze.TrackFragment.{Bus, Underground}
import ru.makkarpov.ttanalyze.analyze.XAnalyzer._
import ru.makkarpov.ttanalyze.db.models._

/**
  * Created by user on 7/15/16.
  */
object TrackFragment {
  def writeFrament(f: TrackFragment, meta: FragmentMetadata): JsValue = {
    def undergroundObj(ug: (UndergroundStation, UndergroundLine)) = {
      val (st, line) = ug
      Json.obj(
        "station" -> st.name,
        "line" -> Json.obj(
          "name" -> line.name,
          "index" -> line.code,
          "fgColor" -> line.textColor,
          "bgColor" -> line.color
        )
      )
    }

    def groundObj(g: GroundStop) = Json.obj("name" -> g.name)

    val ret = Seq.newBuilder[(String, JsValueWrapper)]

    ret += "startPoint" -> f.startPoint
    ret += "endPoint" -> f.endPoint
    ret += "fragmentType" -> (f match {
      case _: Raw => "raw"
      case _: Walk => "walk"
      case _: NoGPS => "no-gps"
      case _: Underground => "underground"
      case _: Bus => "bus"
      case _ => "unknown"
    })

    ret += "metadata" -> Json.obj(
      "time" -> meta.time,
      "distance" -> meta.distance,
      "movement" -> meta.movement,
      "avgSpeed" -> meta.avgSpeed,
      "avgMovementSpeed" -> meta.avgMovementSpeed
    )

    f match {
      case Underground(_, _, enter, exit) =>
        ret += "stations" -> Json.obj(
          "enter" -> undergroundObj(enter),
          "exit" -> undergroundObj(exit)
        )

      case Bus(_, _, route, enter, exit, wait) =>
        ret += "route" -> Json.obj(
          "index" -> route.index,
          "kind" -> route.kind.toString.toLowerCase
        )

        ret += "stations" -> Json.obj(
          "enter" -> groundObj(enter),
          "exit" -> groundObj(exit)
        )

        ret += "waitTime" -> wait

      case _ =>
    }

    Json.obj(ret.result():_*)
  }

  case class FragmentMetadata(time: Long, distance: Double, movement: Double, avgSpeed: Double, avgMovementSpeed: Double)

  // Common fragments:
  case class Raw(startPoint: Int, endPoint: Int) extends TrackFragment
  case class Walk(startPoint: Int, endPoint: Int) extends TrackFragment
  case class NoGPS(startPoint: Int, endPoint: Int) extends TrackFragment

  // Transportation fragments:
  case class Underground(startPoint: Int, endPoint: Int, enter: (UndergroundStation, UndergroundLine),
                         exit: (UndergroundStation, UndergroundLine)) extends TrackFragment

  case class Bus(startPoint: Int, endPoint: Int, route: GroundRoute, enter: GroundStop, exit: GroundStop, waitTime: Long)
    extends TrackFragment

  // Internal fragments:
  case class SameStation(startPoint: Int, endPoint: Int, stations: StationCollection) extends TrackFragment
}

// All points are inclusive
sealed trait TrackFragment {
  def startPoint: Int
  def endPoint: Int
}
