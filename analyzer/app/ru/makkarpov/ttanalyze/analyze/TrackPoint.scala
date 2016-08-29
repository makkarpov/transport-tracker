package ru.makkarpov.ttanalyze.analyze

import java.io.DataInputStream

import play.api.libs.json._
import play.api.libs.functional.syntax._
import ru.makkarpov.ttanalyze.analyze.Movement.Movement
import ru.makkarpov.ttanalyze.analyze.TrackPoint.PointType
import ru.makkarpov.ttanalyze.utils.GeoUtils

/**
  * Created by user on 7/15/16.
  */
object TrackPoint {
  type PointType = PointType.Value
  object PointType extends Enumeration {
    val Regular     = Value
    val GPSLost     = Value
    val GPSAcquired = Value
  }

  def read(is: DataInputStream): TrackPoint = {
    val time = is.readLong()
    val tpe = PointType(is.readUnsignedByte())
    val movement = Movement(is.readUnsignedByte())
    val lat = is.readDouble()
    val lng = is.readDouble()
    val alt = is.readDouble()
    val speed = is.readDouble()
    val bearing = is.readDouble()

    TrackPoint(time, tpe, movement, lat, lng, alt, speed, bearing)
  }

  implicit val jsonWrites = (
      (__ \ "time").write[Long] and
      (__ \ "type").write[PointType] and
      (__ \ "movement").write[Movement] and
      (__ \ "lat").write[Double] and
      (__ \ "lng").write[Double] and
      (__ \ "alt").write[Double] and
      (__ \ "speed").write[Double] and
      (__ \ "bearing").write[Double]
    )(unlift(TrackPoint.unapply))

  val PointSize = 8 + 1 + 1 + 8 + 8 + 8 + 8 + 8
}

case class TrackPoint(time: Long, tpe: PointType, movement: Movement, lat: Double, lng: Double, alt: Double,
                      speed: Double, bearing: Double) {
  def jtsPoint = GeoUtils.latLng(lat, lng)
  def distance(o: TrackPoint) = GeoUtils.distance(lat, lng, o.lat, o.lng)
}
