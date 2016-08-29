package ru.makkarpov.ttdroid.accelerometer

import java.util

import android.content.Context
import android.graphics.{Canvas, DashPathEffect, Paint, Path}
import android.util.AttributeSet
import android.view.View

import ru.makkarpov.ttdroid.utils.VectorExtensions._

/**
  * Created by user on 7/21/16.
  */
class AccelerometerPlot(ctx: Context, attrs: AttributeSet) extends View(ctx, attrs) {
  var points = Array.empty[V3]
  var offset = 0

  val textPaint = new Paint()
  val refPaint = new Paint()
  val dataPaints = new Array[Paint](3)
  val path = new Path()

  var text = Seq.empty[String]

  initPaints()

  private def initPaints(): Unit = {
    textPaint.setColor(0xFF000000)
    refPaint.setColor(0xFFAAAAAA)
    refPaint.setStyle(Paint.Style.STROKE)
    refPaint.setStrokeWidth(2)
    refPaint.setPathEffect(new DashPathEffect(Array(5F, 5F), 0F))

    val colors = Array(0xFF0000, 0x008000, 0x0000FF)
    for (i <- dataPaints.indices) {
      dataPaints(i) = new Paint()
      dataPaints(i).setColor(colors(i) | 0xDD000000)
      dataPaints(i).setStyle(Paint.Style.STROKE)
      dataPaints(i).setStrokeWidth(2)
      dataPaints(i).setStrokeCap(Paint.Cap.ROUND)
      dataPaints(i).setStrokeJoin(Paint.Join.ROUND)
    }
  }

  def append(f: V3): Unit = {
    if (points.isEmpty)
      return

    offset = (offset + 1) % points.length

    points(offset) = f

    invalidate()
  }

  def set(data: Array[V3]): Unit = {
    offset = 0
    for (i <- points.indices)
      if (i < data.length) points(i) = data(i)
      else points(i) = null

    invalidate()
  }

  def setText(t: String*): Unit = text = t
  def addText(t: String*): Unit = text ++= t

  override def onDraw(canvas: Canvas): Unit = {
    if (points.length != canvas.getWidth) {
      points = util.Arrays.copyOf(points, canvas.getWidth)
      if (offset >= points.length)
        offset = 0
    }

    canvas.drawRGB(255, 255, 255)

    val scale = (canvas.getHeight / 2.0) / 15.0 * -1 /* Y axis is inverted */
    def yCoord(f: Float): Int = (f * scale + canvas.getHeight / 2).toInt

    def ref(f: Float) = {
      val y = yCoord(f)
      canvas.drawLine(0, y, canvas.getWidth, y, refPaint)
      refPaint.setStyle(Paint.Style.FILL)
      canvas.drawText(f"$f%.2f m/s\u00b2", 3, y - 3, refPaint)
      refPaint.setStyle(Paint.Style.STROKE)
    }

    ref(-9.81F)
    ref(0F)
    ref(9.81F)

    def draw(axis: Int): Unit = {
      var gap = true
      path.reset()
      for (i <- points.indices) {
        val idx = (offset + i) % points.length
        if (points(idx) == null) gap = true
        else {
          val coord = yCoord(points(idx).productElement(axis).asInstanceOf[Double].toFloat)
          if (gap) path.moveTo(i, coord)
          else path.lineTo(i, coord)
          gap = false
        }
      }

      canvas.drawPath(path, dataPaints(axis))
    }

    for (i <- 0 until 3) draw(i)

    for (i <- text.indices; t = text(i))
      canvas.drawText(t, 3, canvas.getHeight - 17 * (text.size - 1 - i) - 3, textPaint)
  }
}