package ru.makkarpov.ttdroid.data

import android.os.Parcelable.Creator
import android.os.{Parcel, Parcelable}
import ru.makkarpov.ttdroid.data.MyPlace.WiFiEntry
import ru.makkarpov.ttdroid.utils.Extensions

import Extensions._

/**
  * Created by user on 7/29/16.
  */
object MyPlace {
  case class WiFiEntry(ssid: String)
  implicit val wifiPickler = upickle.default.macroRW[WiFiEntry]
}

case class MyPlace(id: Int, name: String, networks: Set[WiFiEntry])