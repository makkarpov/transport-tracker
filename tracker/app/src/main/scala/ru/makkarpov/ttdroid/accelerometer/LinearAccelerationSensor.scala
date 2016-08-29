package ru.makkarpov.ttdroid.accelerometer

import ru.makkarpov.ttdroid.utils.VectorExtensions._

/**
  * @param sampleRate Desired output sample rate
  */
class LinearAccelerationSensor(val sampleRate: Int, target: FeatureListener) {
  // Resampling variables:
  private var _samplePeriod = 0
  private var _recalculationSamples = 200
  private var _lastSample = System.currentTimeMillis()
  private var _lastRawSample = System.currentTimeMillis()
  private val _inputFrequency = new SlidingWindow(300)

  // Low pass filter:
  private val _alpha = 0.25f
  private val _output = new Array[Float](3)

  // Sliding (really - jumping) window:
  val windowOverlap = 8
  private val _window = new JumpingWindow[V3](256, windowOverlap)
  val windowRate = _window.nPoints.toDouble / sampleRate * windowOverlap

  // Gravity estimation:
  private val _5sec = new SlidingWindow.Vector(5 * sampleRate)
  private var _gravity = (0.0, 0.0, 0.0)
  private var _varThr = 0.0
  private var _varInc = 0.05

  // Cached data array
  private val _processedData = new Array[V2](_window.nPoints)
  private val _feature = new FeatureExtractor(this, target)

  def sampleReceived(data: Array[Float]): Unit = {
    val now = System.currentTimeMillis()

    _inputFrequency << (now - _lastRawSample)
    _lastRawSample = now
    _recalculationSamples -= 1

    if (_recalculationSamples <= 0) {
//      Log.i("LinearAcceleration", f"Raw sample rate is ${1000 / _inputFrequency.mean}%.2f Hz")
      _samplePeriod = (1000 / sampleRate) - _inputFrequency.mean.toInt
      _inputFrequency.flushMean()
      _recalculationSamples = 400
    }

    if ((_samplePeriod != 0) && (now - _lastSample >= _samplePeriod)) {
      for (i <- data.indices)
        _output(i) = _output(i) + _alpha * (data(i) - _output(i))

      val vec = (_output(0).toDouble, _output(1).toDouble, _output(2).toDouble)

      _lastSample = now
      _5sec << vec
      _window
        .append(vec)
        .foreach(processData)
    }
  }

  private def processData(window: Array[V3]): Unit = {
    // Compensate gravity:
    val (mean, variance) = window.toSeq.meanVar

    if ((_gravity == null) || (mean - _gravity).length >= 2) _varThr = 2
    if (variance < 1.5) {
      if (variance < _varThr) {
        _gravity = mean
        _varThr = (_varThr + variance) / 2
        _varInc = _varThr * 0.01
      } else {
        _varThr += _varInc
      }
    } else {
      _gravity = _5sec.mean
    }

    if (_gravity != null && !window.contains(null)) {
      // Convert it to vertical and horizontal parts of acceleration
      for (i <- window.indices) {
        val cmp = window(i) - _gravity
        val vert = (cmp dot _gravity) / (_gravity dot _gravity)
        _processedData(i) = (vert * _gravity.length, (cmp - _gravity * vert).length)
      }

      _feature.processData(_processedData)
    }
  }

  def gravity = _gravity

  def windowSize = _processedData.length
}
