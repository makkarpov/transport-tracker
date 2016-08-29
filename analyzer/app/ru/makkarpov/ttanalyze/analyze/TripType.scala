package ru.makkarpov.ttanalyze.analyze

/**
  * Created by user on 7/20/16.
  */
case object TripType extends Enumeration {
  val Walk = Value
  val Bus = Value
  val Underground = Value

  def guess(vals: (Value, Double)*) = {
    val filter = vals.filter(_._2 > 0)
    val sum = vals.map(_._2).sum
    Guess(vals.map { case (k, v) => (k, v / sum) }.toMap)
  }

  case class Guess(vals: Map[Value, Double]) {
    def apply(t: Value) = vals.getOrElse(t, 0)
  }
}
