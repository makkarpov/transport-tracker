package ru.makkarpov.ttdroid.accelerometer

import ru.makkarpov.ttdroid.accelerometer.Movement.Movement

/**
  * Created by user on 7/26/16.
  */
trait FeatureListener {
  def movementClassified(tpe: Movement): Unit
  def plot: Option[AccelerometerPlot]
}
