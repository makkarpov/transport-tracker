package ru.makkarpov.ttdroid.accelerometer

import scala.reflect.ClassTag

/**
  * Created by user on 7/22/16.
  */
class JumpingWindow[T: ClassTag](val nPoints: Int, val nWindows: Int) {
  if (nPoints % nWindows != 0)
    throw new IllegalArgumentException("nPoints % nWindows != 0")

  private val _points = Array.tabulate(nWindows)(_ => new Array[T](nPoints))
  private val _offsets = Array.tabulate(nWindows)(i => i * nPoints / nWindows)

  /**
    * Appends specified point to all windows, returns Array if some window is full
    */
  def append(v: T): Option[Array[T]] = {
    var ret = Option.empty[Array[T]]

    for (i <- 0 until nWindows) {
      _points(i)(_offsets(i)) = v
      _offsets(i) = (_offsets(i) + 1) % nPoints
      if (_offsets(i) == 0)
        ret = Some(_points(i))
    }

    ret
  }
}
