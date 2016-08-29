package ru.makkarpov.ttdroid.map

import java.io.ByteArrayOutputStream

import android.content.Context
import android.graphics.{Point => _, _}
import com.google.android.gms.maps.model.{LatLng, Tile, TileProvider}
import com.google.maps.android.geometry.Point
import com.google.maps.android.projection.SphericalMercatorProjection
import ru.makkarpov.ttdroid.accelerometer.Movement
import ru.makkarpov.ttdroid.accelerometer.Movement.Movement
import ru.makkarpov.ttdroid.data.TrackPoint.PointType
import ru.makkarpov.ttdroid.utils.Extensions._
import ru.makkarpov.ttdroid.data.TrackPoint
import TrackOverlayProvider._

/**
  * Created by user on 7/8/16.
  */
object TrackOverlayProvider {
  // A magic number from
  //
  //   https://developers.google.com/maps/documentation/android/views#zoom
  //
  val BaseTileSize = 256

  val LineThickness = 3F

  class MutPoint {
    var x: Float = 0
    var y: Float = 0

    def :=(p: Point)(implicit ctx: RenderCtx): Unit = {
      this.x = p.x.toFloat * ctx.scale - ctx.x * ctx.tileDimension
      this.y = p.y.toFloat * ctx.scale - ctx.y * ctx.tileDimension
    }
  }

  case class RenderCtx(canvas: Canvas, shaderMat: Matrix, gradientPaint: Paint, colorPaint: Paint,
                       gpsLostPaint: Paint, scale: Float, x: Int, y: Int, tileDimension: Int,
                       density: Double)

  type ColorMode = ColorMode.Value
  object ColorMode extends Enumeration {
    val Speed = Value
    val Movement = Value
  }

  case class ShowOptions(mode: ColorMode, pointNumbers: Boolean)
}

class TrackOverlayProvider(ctx: Context, track: Seq[TrackPoint], options: ShowOptions)
extends TileProvider {
  val density = ctx.getResources.getDisplayMetrics.density
  val tileDimension = (BaseTileSize * density).toInt
  val projection = new SphericalMercatorProjection(BaseTileSize)

  val projectedPoints = track.map(x => projection.toPoint(new LatLng(x.loc.lat, x.loc.lng)))

  def pointOffset(p: TrackPoint): Float = options.mode match {
    // Maps [0..120] km/h to [0..1]
    case ColorMode.Speed => (p.loc.speed * 3.6 / 120.0).clamp(0, 1).toFloat
    case ColorMode.Movement => p.movement match {
      case Movement.Pedestrian => 0
      case Movement.Unclassified => 1
      case _ => 0.5F
    }
  }

  // Maps [0 .. 1] to [red .. green .. blue]
  def offsetColor(offset: Float, s: Float = 1.0F, v: Float = 0.8F): Int =
    Color.HSVToColor(Array(offset * 240.0F, s, v))

  override def getTile(x: Int, y: Int, zoom: Int): Tile = {
    val bitmap = Bitmap.createBitmap(tileDimension, tileDimension, Bitmap.Config.ARGB_8888)
    val canvas = new Canvas(bitmap)

    val gradientPaint = new Paint()
    val shaderMat = new Matrix()

    def setPaint(p: Paint): Unit = {
      p.setStyle(Paint.Style.STROKE)
      p.setStrokeCap(Paint.Cap.BUTT)
      p.setStrokeJoin(Paint.Join.ROUND)
      p.setFlags(Paint.ANTI_ALIAS_FLAG)
    }

    setPaint(gradientPaint)
    gradientPaint.setShader(new LinearGradient(0, 0, 1, 0,
      Array.tabulate(5)(_.toFloat / 4).map(offsetColor(_)), null, Shader.TileMode.CLAMP))
    gradientPaint.getShader.setLocalMatrix(shaderMat)

    val colorPaint = new Paint()
    setPaint(colorPaint)

    val gpsLostPaint = new Paint()
    setPaint(gpsLostPaint)
    gpsLostPaint.setColor(0xFF666666)
    gpsLostPaint.setPathEffect(new DashPathEffect(Array(4F, 6F), 0))

    val scale = (math.pow(2, zoom) * density).toFloat

    implicit val ctx = new RenderCtx(canvas, shaderMat, gradientPaint, colorPaint, gpsLostPaint,
                                     scale, x, y, tileDimension, density)

    colorPaint.setStrokeWidth(LineThickness * density)
    gradientPaint.setStrokeWidth(colorPaint.getStrokeWidth)
    gpsLostPaint.setStrokeWidth(colorPaint.getStrokeWidth / 2)

    renderTrail()

    val os = new ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
    new Tile(tileDimension, tileDimension, os.toByteArray)
  }

  private def renderTrail()(implicit ctx: RenderCtx): Unit = {
    // TODO: Culling

    val a = new MutPoint
    val b = new MutPoint

    for {
      i <- 0 until (track.size - 1)
      ta = track(i)
      tb = track(i + 1)
    } {
      a := projectedPoints(i)
      b := projectedPoints(i + 1)

      if ((ta.tpe == PointType.GPSLost) && (tb.tpe == PointType.GPSAcquired)) {
        ctx.canvas.drawLine(a.x, a.y, b.x, b.y, ctx.gpsLostPaint)
      } else {
        val aOfs = pointOffset(ta)
        val bOfs = pointOffset(tb)

        drawLine(a, aOfs)(b, bOfs)
      }
    }

    for (i <- track.indices) {
      a := projectedPoints(i)
      val ofs = pointOffset(track(i))

      ctx.colorPaint.setColor(offsetColor(ofs))
      ctx.colorPaint.setStyle(Paint.Style.FILL)
      ctx.canvas.drawCircle(a.x, a.y, ctx.colorPaint.getStrokeWidth / 2, ctx.colorPaint)

      if (options.pointNumbers) {
        ctx.colorPaint.setColor(offsetColor(ofs, s = 0.8F, v = 0.6F))
        ctx.canvas.drawText("#" + i, a.x + 4, a.y, ctx.colorPaint)
      }

      ctx.colorPaint.setStyle(Paint.Style.STROKE)
    }
  }

  private def drawLine(f: MutPoint, fRatio: Float)
                      (t: MutPoint, tRatio: Float)(implicit ctx: RenderCtx): Unit = {

    if ((fRatio - tRatio).abs < 0.01) {
      ctx.colorPaint.setColor(offsetColor((fRatio + tRatio) / 2))
      ctx.canvas.drawLine(f.x, f.y, t.x, t.y, ctx.colorPaint)
      return
    }

    ctx.shaderMat.reset()

    ctx.shaderMat.preRotate(Math.toDegrees(Math.atan2(t.y - f.y, t.x - f.x)).toFloat, f.x, f.y)
    ctx.shaderMat.preTranslate(f.x, f.y)
    val scale = Math.hypot(t.x - f.x, t.y - f.y).toFloat
    ctx.shaderMat.preScale(scale, scale)
    ctx.shaderMat.preScale(1f / (tRatio - fRatio), 1f / (tRatio - fRatio))
    ctx.shaderMat.preTranslate(-fRatio, 0)

    ctx.gradientPaint.getShader.setLocalMatrix(ctx.shaderMat)
    ctx.canvas.drawLine(f.x, f.y, t.x, t.y, ctx.gradientPaint)
  }
}
