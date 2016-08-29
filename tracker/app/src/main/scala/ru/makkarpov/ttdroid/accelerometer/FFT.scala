package ru.makkarpov.ttdroid.accelerometer

/**
  * Created by user on 7/25/16.
  */
object FFT {

}

class FFT(var n: Int) {
  var nLog2: Int = (Math.log(n) / Math.log(2)).toInt
  var cos = new Array[Double](n / 2)
  var sin = new Array[Double](n / 2)

  private val im = new Array[Double](n)

  if (n != (1 << nLog2))
    throw new RuntimeException("FFT length must be power of 2")

  for (i <- 0 until n / 2) {
    cos(i) = Math.cos(-2 * Math.PI * i / n)
    sin(i) = Math.sin(-2 * Math.PI * i / n)
  }

  def fft(re: Array[Double], im: Array[Double]) {
    var i: Int = 1
    var j: Int = 0
    var k: Int = 0
    var n1: Int = 0
    var n2: Int = n / 2
    var a: Int = 0
    var c: Double = 0.0
    var s: Double = 0.0
    var t1: Double = 0.0
    var t2: Double = 0.0

    while (i < n - 1) {
      n1 = n2
      while (j >= n1) {
        j = j - n1
        n1 = n1 / 2
      }
      j = j + n1
      if (i < j) {
        t1 = re(i)
        re(i) = re(j)
        re(j) = t1
        t1 = im(i)
        im(i) = im(j)
        im(j) = t1
      }
      i += 1
    }

    n1 = 0
    n2 = 1

    i = 0
    while (i < nLog2) {
      n1 = n2
      n2 = n2 + n2
      a = 0
      j = 0
      while (j < n1) {
        c = cos(a)
        s = sin(a)
        a += 1 << (nLog2 - i - 1)
        k = j
        while (k < n) {
          t1 = c * re(k + n1) - s * im(k + n1)
          t2 = s * re(k + n1) + c * im(k + n1)
          re(k + n1) = re(k) - t1
          im(k + n1) = im(k) - t2
          re(k) = re(k) + t1
          im(k) = im(k) + t2
          k = k + n2
        }
        j += 1
      }
      i += 1
    }
  }

  def fft_mag(data: Array[Double]): Unit = {
    for (i <- im.indices) im(i) = 0
    fft(data, im)
    for (i <- im.indices) data(i) = math.hypot(data(i), im(i)) / n
  }
}
