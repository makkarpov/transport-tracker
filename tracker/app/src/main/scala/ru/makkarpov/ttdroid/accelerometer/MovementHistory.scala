package ru.makkarpov.ttdroid.accelerometer

import ru.makkarpov.ttdroid.accelerometer.Movement.Movement

/**
  * Created by user on 7/26/16.
  */
class MovementHistory extends FeatureListener {
  private val counts = new Array[Int](Movement.values.size)
  private var _count = 0
  private var _lastMode = Movement.Unclassified

  override def movementClassified(tpe: Movement): Unit = {
    counts(tpe.id) += 1
    _count += 1
  }

  def getMode: Movement = {
    if (_count == 0)
      return _lastMode

    val idx = counts.indexOf(counts.max)

    _count = 0
    for (i <- counts.indices)
      counts(i) = 0

    _lastMode = Movement(idx)
    _lastMode
  }

  def plot = None
}
