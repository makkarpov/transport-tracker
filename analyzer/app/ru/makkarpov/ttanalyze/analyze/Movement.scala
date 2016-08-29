package ru.makkarpov.ttanalyze.analyze

/**
  * Created by user on 7/27/16.
  */
object Movement extends Enumeration {
  type Movement = Value

  val Unclassified  = Value
  val Stationary    = Value
  val Pedestrian    = Value
  val Bus           = Value
  val Train         = Value
  val Underground   = Value
  val Tram          = Value
}
