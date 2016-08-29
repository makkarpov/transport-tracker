package ru.makkarpov.ttanalyze.db

/**
  * Created by user on 7/18/16.
  */
object GroundTransport extends Enumeration {
  type GroundTransport = Value

  val Bus = Value
  val Trolleybus = Value
  val Tram = Value
  val Monorail = Value
}
