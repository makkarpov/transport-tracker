package ru.makkarpov.ttdroid.stats.charts

import java.util.TimeZone

import android.content.Context
import android.graphics._
import android.util.AttributeSet
import android.view.View
import ru.makkarpov.ttdroid.stats.charts.Histogram.{CachedData, HistogramAdapter, Settings}
import ru.makkarpov.ttdroid.utils.SimpleCached

/**
  * Created by user on 8/15/16.
  */
object Histogram {
  case class Settings(timeWindow: Long = 24 * 3600 * 1000, binWidth: Int = 2)
  case class HistogramBin(value: Float, color: Float)

  trait HistogramAdapter {
    def fillBinData(setts: Settings, nBins: Int, binData: Array[HistogramBin]): Unit
    def roundValueScale(vs: Float): Float = vs
    def describeValue(v: Float): String
  }

  abstract class AggregatedAdapter[T](data: Set[T]) extends HistogramAdapter {
    def included(v: T): Boolean = true

    /**
      * @return Time of specified value
      */
    def time(v: T): Long

    /**
      * @param vs Values belonging to this bin
      * @return Calculate bin data
      */
    def aggregate(vs: Set[T]): HistogramBin

    override def fillBinData(setts: Settings, nBins: Int, binData: Array[HistogramBin]): Unit = {
      val timezone = TimeZone.getDefault

      def bin(v: T): Int = {
        val t = time(v)
        val lt = t + timezone.getOffset(t)
        ((lt % setts.timeWindow).toFloat / setts.timeWindow * nBins).toInt
      }

      for ((b, d) <- data.filter(included).groupBy(bin).mapValues(aggregate))
        binData(b) = d
    }
  }

  case class Data(data: Array[HistogramBin], valueScale: Float)

  class CachedData(setts: Settings, val adapter: HistogramAdapter)
  extends SimpleCached[Int, Data] {
    override protected def calculate(width: Int): Data = {
      val nBins = width / setts.binWidth
      val data = new Array[HistogramBin](nBins)
      adapter.fillBinData(setts, nBins, data)

      for (i <- data.indices if data(i) == null)
        data(i) = HistogramBin(0, 0)

      val valueScale = adapter.roundValueScale(data.map(_.value).max) max Float.MinPositiveValue
      Data(data, valueScale)
    }
  }
}

class Histogram(ctx: Context, attrs: AttributeSet) extends View(ctx, attrs) {
  var dataHolder: CachedData = _
  val bgColor = getBackgroundColor

  // Paint settings
  val axisTopPadding = 10
  val axisVertPadding = 40
  val axisHorizPadding = 40
  val axisTickWidth = 6
  val axisCross = 8
  val axisTextAngle = 30
  val axisTextPadding = 3
  val vertTicksCount = 5

  val hourTicks = 0 to 21 by 3

  val settings = Settings(binWidth = 3)

  // Cached paint objects
  val axisPaint = new Paint()
  axisPaint.setStyle(Paint.Style.STROKE)
  axisPaint.setColor(0xFF000000)
  axisPaint.setStrokeWidth(2F)

  val rulerPaint = new Paint()
  rulerPaint.setStyle(Paint.Style.STROKE)
  rulerPaint.setColor(0xFFCCCCCC)
  rulerPaint.setStrokeWidth(2F)
  rulerPaint.setPathEffect(new DashPathEffect(Array(5F, 5F), 0F))

  val axisText = new Paint()
  axisText.setStyle(Paint.Style.FILL)
  axisText.setTextSize(14)
  axisText.setAntiAlias(true)
  axisText.setColor(0xFF000000)

  val chartPaint = new Paint()
  chartPaint.setStyle(Paint.Style.FILL)
  axisText.setColor(0xFFFF0000)

  private def getBackgroundColor: Int = {
    val arr = getContext.getTheme.obtainStyledAttributes(Array(
      android.R.attr.colorBackground
    ))

    val ret = arr.getColor(0, 0xFFFFFFFF)
    arr.recycle()

    ret
  }

  def setData(t: HistogramAdapter): Unit =
    dataHolder = new CachedData(settings, t)

  override def onDraw(canvas: Canvas): Unit = {
    canvas.drawColor(bgColor)
    val bounds = new Rect()

    if (dataHolder == null) {
      val t = "No data was set"
      axisText.getTextBounds(t, 0, t.length, bounds)
      axisText.setColor(0xFF000000)
      canvas.drawText(t, (canvas.getWidth - bounds.width()) / 2, canvas.getHeight / 2, axisText)

      return
    }

    val data = dataHolder(canvas.getWidth - axisHorizPadding)

    def yCoord(i: Int): Int =
      ((canvas.getHeight - axisVertPadding - axisTopPadding).toFloat / vertTicksCount * i +
        axisTopPadding).toInt

    def xCoord(d: Double): Int =
      (axisHorizPadding + d * (canvas.getWidth - axisHorizPadding)).toInt

    def describeTick(i: Int): String =
      dataHolder.adapter.describeValue(data.valueScale / vertTicksCount * (vertTicksCount - i))

    def shouldDrawLabel(i: Int): Boolean = {
      val t = describeTick(i)
      t.nonEmpty && ((i == 0) || (t != describeTick(i - 1)))
    }

    for (i <- 0 until vertTicksCount) {
      val y = yCoord(i)

      val pt = new Path()
      pt.moveTo(axisHorizPadding, y)
      pt.lineTo(canvas.getWidth, y)
      canvas.drawPath(pt, rulerPaint)

      // https://code.google.com/p/android/issues/detail?id=29944
      // canvas.drawLine(axisHorizPadding, y, canvas.getWidth, y, rulerPaint)
    }

    // draw bins

    for ((b, i) <- data.data.zipWithIndex) {
      val x = axisHorizPadding + i * settings.binWidth

      val (color, offset) = b.value match {
        case Float.NaN => (0xFFAAAAAA, 0)
        case value =>
          val offset = axisTopPadding + (1 - value.toFloat / data.valueScale) *
            (canvas.getHeight - axisVertPadding - axisTopPadding)

          (Color.HSVToColor(Array(b.color, 0.8F, 1F)), offset.toInt)
      }

      chartPaint.setColor(color)
      canvas.drawRect(x, offset, x + settings.binWidth, canvas.getHeight - axisVertPadding, chartPaint)
    }

    // draw axes
    canvas.drawLine(axisHorizPadding, axisTopPadding, axisHorizPadding,
      canvas.getHeight - axisVertPadding + axisCross, axisPaint)

    canvas.drawLine(axisHorizPadding - axisCross, canvas.getHeight - axisVertPadding,
      canvas.getWidth, canvas.getHeight - axisVertPadding, axisPaint)

    // draw ticks on horizontal axis
    for ((h, i) <- hourTicks.zipWithIndex) {
      val x = xCoord(i.toDouble / hourTicks.length)
      val y = canvas.getHeight - axisVertPadding
      val t = f"$h%02d:00"

      axisText.getTextBounds(t, 0, t.length, bounds)
      axisText.setColor(0xFF000000)

      canvas.save()
      canvas.drawLine(x, y - axisTickWidth / 2, x, y + axisTickWidth / 2, axisPaint)
      canvas.rotate(axisTextAngle, x, y)

      val tX = (x - (bounds.width() * math.cos(axisTextAngle) * 0.5).toFloat) max (axisHorizPadding + axisTextPadding)
      val tY = y + axisTickWidth + bounds.height() + axisTextPadding

      canvas.drawText(t, tX, tY, axisText)
      canvas.restore()
    }

    // draw secondary ticks on horizontal axis
    for (i <- 0 until 24) {
      val x = xCoord(i / 24.0)
      val y = canvas.getHeight - axisVertPadding

      canvas.drawLine(x, y, x, y + axisTickWidth / 2, axisPaint)
    }

    // draw ticks on vertical axis

    for (i <- 0 until vertTicksCount if shouldDrawLabel(i)) {
      val y = yCoord(i)

      canvas.drawLine(axisHorizPadding - axisTickWidth / 2, y,
        axisHorizPadding + axisTickWidth / 2, y, axisPaint)

      val t = describeTick(i)

      axisText.getTextBounds(t, 0, t.length, bounds)

      val tX = axisHorizPadding - axisTickWidth / 2 - bounds.width() - axisTextPadding
      val tY = y + bounds.height() / 2

      canvas.drawText(t, tX, tY, axisText)
    }
  }
}
