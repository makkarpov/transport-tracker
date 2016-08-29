package ru.makkarpov.ttanalyze.utils

import com.vividsolutions.jts.geom._
import com.vividsolutions.jts.io.WKTReader
import ru.makkarpov.ttanalyze.analyze.TrackPoint

/**
  * Created by user on 7/15/16.
  */
object GeoUtils {
  val geomFactory = new GeometryFactory(new PrecisionModel(), 4326)
  val wktReader = new WKTReader(geomFactory)
  val earthRadius = 6378137
  val eps = 0.1

  def latLng(lat: Double, lng: Double): Point = geomFactory.createPoint(new Coordinate(lng, lat))

  def lineString(track: Seq[TrackPoint]): LineString =
    geomFactory.createLineString(track.map(x => new Coordinate(x.lng, x.lat)).toArray)

  // JTS extension functions for spherical geometry:
  def distance(p: Point, g: Polygon): Double = {
    if (p.intersects(g))
      return 0

    val ring = g.getExteriorRing.getCoordinates
    var min = Double.PositiveInfinity

    for {
      i <- 0 until (ring.length - 1)
      a = ring(i)
      b = ring(i + 1)
      d = V2(p.getY, p.getX).distanceSegment(V2(a.y, a.x), V2(b.y, b.x)) if d < min
    } min = d

    min
  }

  // Spherical geometry functions:

  case class V2(lat: Double, lng: Double) {
    def distance(other: V2): Double = {
      val latDist = Math.toRadians(other.lat - lat)
      val lonDist = Math.toRadians(other.lng - lng)
      val a = Math.sin(latDist / 2) * Math.sin(latDist / 2) + Math.sin(lonDist / 2) * Math.sin(lonDist / 2) *
        Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(other.lat))
      2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)) * earthRadius
    }

    def toCartesian: V3 = {
      val x = earthRadius * Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(lng))
      val y = earthRadius * Math.cos(Math.toRadians(lat)) * Math.sin(Math.toRadians(lng))
      val z = earthRadius * Math.sin(Math.toRadians(lat))
      V3(x, y, z)
    }

    def nearestPointGreatCircle(line1: V2, line2: V2): V2 = {
      val ca = line1.toCartesian
      val cb = line2.toCartesian
      val cc = toCartesian

      val g = ca * cb
      val t = g * (cc * g)
      (t.normalize * earthRadius).toPolar
    }

    def onSegment(line1: V2, line2: V2) = ((line1 distance line2) - distance(line1) - distance(line2)).abs < eps

    def nearestPointSegment(line1: V2, line2: V2): V2 = {
      val p = nearestPointGreatCircle(line1, line2)

      if (p.onSegment(line1, line2)) p
      else if ((line1 distance p) < (line2 distance p)) line1
      else line2
    }

    def distanceSegment(l1: V2, l2: V2) = distance(nearestPointSegment(l1, l2))
  }

  case class V3(x: Double, y: Double, z: Double) {
    def toPolar: V2 = {
      val lat = Math.toDegrees(Math.asin(z / earthRadius))
      val lng = Math.toDegrees(Math.atan2(y, x))

      V2(lat, lng)
    }

    def length = math.sqrt(x * x + y * y + z * z)

    def *(other: V3): V3 = {
      val px = y * other.z - z * other.y
      val py = z * other.x - x * other.z
      val pz = x * other.y - y * other.x

      V3(px, py, pz)
    }

    def normalize: V3 = {
      val l = length
      V3(x / l, y / l, z / l)
    }

    def *(k: Double): V3 = V3(x * k, y * k, z * k)
  }

  def distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double = V2(lat1, lon1) distance V2(lat2, lon2)
}
