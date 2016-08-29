package ru.makkarpov.ttdroid.utils

import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}

import android.content.Context
import android.location.Location
import ru.makkarpov.ttdroid.data.TrackPoint
import upickle.{Invalid, Js}
import upickle.Js.Value

import upickle.default.{Reader, Writer}

/**
  * Created by user on 8/1/16.
  */
object Utils {
  private val sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  sdf.setTimeZone(TimeZone.getTimeZone("UTC"))

  def rfcDateStr(ts: Long): String = sdf.format(new Date(ts))
  def dateStr(ts: Long): String = new SimpleDateFormat("HH:mm:ss, dd.MM.yyyy").format(new Date(ts))

  def intervalStr(i: Long): String = f"${i / 3600000}%d:${(i / 60000) % 60}%02d:${(i / 1000) % 60}%02d"

  def quantityFmt(ctx: Context, id: Int, n: Int): String =
    ctx.getResources.getQuantityString(id, n) format n

  def distance(a: TrackPoint, b: TrackPoint): Float = {
    val dist = Array(0F)
    Location.distanceBetween(a.loc.lat, a.loc.lng, b.loc.lat, b.loc.lng, dist)
    dist(0)
  }

  def enumerationWriter(obj: Enumeration): Writer[obj.Value] = new Writer[obj.Value] {
    override def write0: (obj.Value) => Value = x => Js.Str(x.toString)
  }

  def enumerationReader(obj: Enumeration): Reader[obj.Value] = new Reader[obj.Value] {
    override def read0: PartialFunction[Value, obj.Value] = {
      case Js.Str(x) => obj.withName(x)
      case x => throw Invalid.Data(x, "expected Js.Str with enumeration item name")
    }
  }
}
