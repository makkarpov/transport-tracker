package ru.makkarpov.ttdroid.accelerometer

import ru.makkarpov.ttdroid.utils.VectorExtensions._

/**
  * Created by user on 7/21/16.
  */
object SlidingWindow {
  /* Sliding window used for gravity estimation in a cases where variance is too high,
   * does not calculate mean at all time, just when it needed. */
  class Vector(nPoints: Int) {
    private val points = Array.fill(nPoints)(null.asInstanceOf[V3])
    private var offset = 0

    def << (x: V3): Unit = {
      points(offset) = x
      offset = (offset + 1) % points.length
    }

    def mean: V3 = points.toSeq.mean
  }
}

class SlidingWindow(nPoints: Int) {
  private val points = Array.fill(nPoints)(Double.NaN)
  private var offset = 0
  private var _sum = 0.0

  def << (n: Double): Double = {
    if (!points(offset).isNaN)
      _sum -= points(offset)

    points(offset) = n
    _sum += n

    offset = (offset + 1) % points.length
    mean
  }

  def flushMean(): Unit = {
    _sum = 0
    for (d <- points if !d.isNaN)
      _sum += d
  }

  def mean = _sum / nPoints
}
