package ru.makkarpov.ttdroid.data

import android.location.Location
import ru.makkarpov.ttdroid.accelerometer.Movement.Movement
import ru.makkarpov.ttdroid.data.TrackPoint.{PointType, Point}

/**
  * Created by user on 7/6/16.
  */
object TrackPoint {
  object Point {
    def apply(loc: Location): Point =
      Point(loc.getLatitude, loc.getLongitude, loc.getAltitude, loc.getSpeed, loc.getBearing)
  }

  case class Point(lat: Double, lng: Double, alt: Double, speed: Double, bearing: Double)

  type PointType = PointType.Value
  object PointType extends Enumeration {
    val Regular     = Value
    val GPSLost     = Value
    val GPSAcquired = Value
  }
}

case class TrackPoint(time: Long, loc: Point, movement: Movement, tpe: PointType)
