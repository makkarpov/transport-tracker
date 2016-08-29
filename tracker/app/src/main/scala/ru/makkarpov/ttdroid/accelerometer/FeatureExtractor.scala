package ru.makkarpov.ttdroid.accelerometer

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import ru.makkarpov.ttdroid.accelerometer.Movement.Movement
import ru.makkarpov.ttdroid.utils.VectorExtensions._

/**
  * Created by user on 7/22/16.
  */
class FeatureExtractor(sensor: LinearAccelerationSensor, target: FeatureListener) {
  private val horizontalStats = new DescriptiveStatistics(sensor.windowSize)
  private val verticalStats = new DescriptiveStatistics(sensor.windowSize)

  private val fftVertical = new Array[Double](sensor.windowSize)
  private val fft = new FFT(sensor.windowSize)

  private val resultHistory = new Array[Movement]((sensor.windowRate * 5).toInt)
  private var resultOffset = 0

  def processData(data: Array[V2]): Unit = {
    for ((v, h) <- data) {
      verticalStats.addValue(v)
      horizontalStats.addValue(h)
    }

    for {
      i <- data.indices
      (v, _) = data(i)
    } fftVertical(i) = v

    fft.fft_mag(fftVertical)

    for (p <- target.plot) {
      p.setText()
      p.set(data.zip(fftVertical).map { case ((v, h), f) => (v, h, f) })
      p.addText(f"FFT peak: $fftPeak%.2f Hz")
    }

    val res = classify()

    target.movementClassified(res)
    resultHistory(resultOffset) = res
    resultOffset = (resultOffset + 1) % resultHistory.length

    for (p <- target.plot) {
      for ((x, y, z) <- Option(sensor.gravity)) {
        val mag = math.sqrt(x * x + y * y + z * z)
        p.addText(f"Gravity: ($x%.2f, $y%.2f, $z%.2f) (mag: $mag%.2f)")
      }

      p.addText(s"--> Result: $res")

      val counts = new Array[Int](Movement.values.size)

      for (x <- resultHistory if x != null)
        counts(x.id) += 1

      val sum = counts.sum

      val ret =
        Movement.values.filter(x => counts(x.id) != 0).toSeq.sortBy(x => -counts(x.id))
          .map(x => f"${x.toString.head}: ${counts(x.id).toDouble / sum * 100}%.2f%%")
          .mkString(", ")

      p.addText(s"--> Hist: $ret")
    }
  }

  private def fftPeak: Double = {
    var max = Double.MinValue
    var maxIdx = 0

    for (i <- 1 to (fftVertical.length / 2) if fftVertical(i) >= max) {
      maxIdx = i
      max = fftVertical(i)
    }

    maxIdx * sensor.sampleRate.toDouble / sensor.windowSize
  }

  private def fft(freq: Double): Double = {
    val bin = (freq / (sensor.sampleRate.toDouble / sensor.windowSize)).toInt

    var sum = 0.5 * fftVertical(bin)
    var weight = 0.5

    if (bin > 1) {
      sum += 0.25 * fftVertical(bin - 1)
      weight += 0.25
    }

    sum += 0.25 * fftVertical(bin + 1)
    weight += 0.25

    sum / weight
  }



  private def classify(): Movement = {
    if (pedestrian())
      return Movement.Pedestrian

    if (stationary())
      return Movement.Stationary

    Movement.Unclassified
  }

  private def pedestrian(): Boolean = {
    val hVariance = horizontalStats.getVariance
    val hRange = horizontalStats.getMax - horizontalStats.getMin
    val vVariance = verticalStats.getVariance
    val vIQR = verticalStats.getPercentile(75) - verticalStats.getPercentile(25)
    val vFFT = fft(2 /* Hz */)

    if (vFFT < 0.2)
      return false

    var votes = 0

    if (hVariance > 0.3) votes += 1
    if (hVariance > 0.7) votes += 1
    if (hRange > 1.4) votes += 1
    if (hRange > 2.0) votes += 1
    if (vVariance > 1.4) votes += 1
    if (vVariance > 2) votes += 1
    if (vIQR > 2.5) votes += 1
    if (vIQR > 3.5) votes += 1
    if (vFFT > 0.3) votes += 1
    if (vFFT > 0.6) votes += 2

    for (p <- target.plot)
      p.addText(
        f"hVariance: $hVariance%.3f",
        f"hRange: $hRange%.3f",
        f"vVariance: $vVariance%.3f",
        f"vIQR: $vIQR%.3f",
        f"vFFT: $vFFT%.3f"
      )

    votes >= 7
  }

  private def stationary(): Boolean = {
    false
  }
}
