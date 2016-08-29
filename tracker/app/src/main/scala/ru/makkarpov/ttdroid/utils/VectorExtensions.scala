package ru.makkarpov.ttdroid.utils

/**
  * Created by user on 8/1/16.
  */
object VectorExtensions {
  type V2 = (Double /* Vertical */, Double /* Horizontal */)
  type V3 = (Double, Double, Double)
  implicit class VectorExtensions(val x: V3) extends AnyVal {
    def -(y: V3): V3 = (x._1 - y._1, x._2 - y._2, x._3 - y._3)
    def *(v: Double): V3 = (x._1 * v, x._2 * v, x._3 * v)
    def dot(y: V3): Double = x._1 * y._1 + x._2 * y._2 + x._3 * y._3
    def length: Double = math.sqrt(x._1 * x._1 + x._2 * x._2 + x._3 * x._3)
    def proj(v: V3): V3 = v * ((x dot v) / (v dot v))
  }

  implicit class VectorSeqExtensions[C[X] <: Traversable[X]](val coll: C[V3]) extends AnyVal {
    def mean: V3 = {
      // Extracted to operate without tuples
      var meanX = 0.0
      var meanY = 0.0
      var meanZ = 0.0
      var cnt = 0

      for (x <- coll if x != null) {
        meanX += x._1
        meanY += x._2
        meanZ += x._3
        cnt += 1
      }

      meanX /= cnt
      meanY /= cnt
      meanZ /= cnt

      (meanX, meanY, meanZ)
    }

    def meanVar: (V3, Double) = {
      // Extracted to operate without tuples
      val avg = mean

      var varX = 0.0
      var varY = 0.0
      var varZ = 0.0
      var cnt = 0

      for (x <- coll if x != null) {
        varX += math.pow(x._1 - avg._1, 2)
        varY += math.pow(x._2 - avg._2, 2)
        varZ += math.pow(x._3 - avg._3, 2)
        cnt += 1
      }

      varX /= cnt
      varY /= cnt
      varZ /= cnt

      val variance = math.sqrt(varX * varX + varY * varY + varZ * varZ)
      (avg, variance)
    }
  }
}
