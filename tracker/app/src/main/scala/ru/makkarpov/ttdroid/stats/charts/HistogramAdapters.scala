package ru.makkarpov.ttdroid.stats.charts

import android.util.Log
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import ru.makkarpov.ttdroid.data.AnalyzedTrack.{GroundFragment, WalkFragment}
import ru.makkarpov.ttdroid.data.TrackFiles
import ru.makkarpov.ttdroid.stats.charts.Histogram.{HistogramBin, AggregatedAdapter}
import ru.makkarpov.ttdroid.utils.Extensions._

/**
  * Created by user on 8/15/16.
  */
object HistogramAdapters {
  abstract class TrackTimed(data: Set[TrackFiles]) extends AggregatedAdapter[TrackFiles](data) {
    override def included(v: TrackFiles): Boolean = v.points use { _.length > 1 }
    override def time(v: TrackFiles): Long =
      v.points use { p =>
        p.seek(0)
        p.read().time
      }
    protected def hue(vs: Set[TrackFiles]): Float = 240 + 60 * (1 - vs.size.toFloat / data.size)
    protected def trackTime(t: TrackFiles): Long =
      t.points use { p =>
        p.seek(0)
        val st = p.read().time
        p.seek(p.length - 1)
        val sp = p.read().time

        sp - st
      }
  }

  abstract class TimeValued(data: Set[TrackFiles]) extends TrackTimed(data) {
    override def describeValue(v: Float): String = {
      val minutes = v.toLong / 1000 / 60
      if (minutes == 0) ""
      else f"${minutes / 60}%d:${minutes % 60}%02d"
    }

    protected def roundTime(vs: Float, step: Int): Float = {
      val ts = vs.toLong
      (ts / step + (if (ts % step != 0) 1 else 0)) * step
    }

    override def roundValueScale(vs: Float): Float = roundTime(vs, 60 * 1000 * 20)
  }

  class ArriveTime(data: Set[TrackFiles]) extends TimeValued(data) {
    override def aggregate(vs: Set[TrackFiles]): HistogramBin =
      HistogramBin(vs.map(trackTime).sum / vs.size, hue(vs))
  }

  class IQR(data: Set[TrackFiles]) extends TimeValued(data) {
    val ds = new DescriptiveStatistics()

    override def aggregate(vs: Set[TrackFiles]): HistogramBin = {
      ds.clear()

      for (x <- vs)
        ds.addValue(trackTime(x))

      HistogramBin((ds.getPercentile(75) - ds.getPercentile(25)).toFloat, hue(vs))
    }
  }

  class WaitTime(data: Set[TrackFiles], idx: Int) extends TimeValued(data) {
    override def roundValueScale(vs: Float): Float = roundTime(vs, 60 * 1000)

    override def aggregate(vs: Set[TrackFiles]): HistogramBin = {
      // Select i'th track fragment except walks:

      val waitTime = data
        .flatMap(_.header.analyze)
        .map(_.fragments.filterNot(_.isInstanceOf[WalkFragment])(idx) match {
          case g: GroundFragment => g.waitTime
          case x => throw new IllegalArgumentException(s"GroundFragment expected, got $x")
        }).filter(_ != 0).sum.toFloat / data.size

      HistogramBin(waitTime, hue(vs))
    }
  }
}
