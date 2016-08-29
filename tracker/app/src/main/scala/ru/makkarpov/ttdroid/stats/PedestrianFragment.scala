package ru.makkarpov.ttdroid.stats

import java.text.SimpleDateFormat
import java.util.Date

import android.os.Bundle
import android.util.Log
import android.view.{View, ViewGroup, LayoutInflater}
import android.widget.TextView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.data.{LineData, LineDataSet, Entry}
import com.github.mikephil.charting.formatter.AxisValueFormatter
import ru.makkarpov.ttdroid.data.AnalyzedTrack.WalkFragment
import ru.makkarpov.ttdroid.data.TrackPoint
import ru.makkarpov.ttdroid.utils.Utils
import ru.makkarpov.ttdroid.{FileListActivity, R}
import ru.makkarpov.ttdroid.utils.Extensions._
import scala.collection.JavaConversions._

/**
  * Created by user on 8/24/16.
  */
class PedestrianFragment extends BaseFragment {
  val timeWindow = 30L * 24 * 3600 * 1000

  val files = FileListActivity.allFiles
    .filter(t => t.header.analyze.isDefined && t.header.startTime > System.currentTimeMillis() - timeWindow)
    .map(t => t -> t.header.analyze.get.fragments.collect { case w: WalkFragment => w.metadata })
    .filter{ case (_, f) => f.nonEmpty }

  val avgSpeed = files.map { case (_, v) => v.map(_.avgSpeed).sum / v.size }.sum / files.length
  val totalDist = files.map { case (_, v) => v.map(_.distance).sum }.sum

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, st: Bundle): View = {
    val ret = inflater.inflate(R.layout.stats_pedestrian, container, false)

    ret.findViewById(R.id.walkDistance).asInstanceOf[TextView].setText(
      getActivity.getString(R.string.pedestrian_dist) format (totalDist / 1000.0))
    ret.findViewById(R.id.walkSpeed).asInstanceOf[TextView].setText(
      getActivity.getString(R.string.pedestrian_speed) format (avgSpeed * 3.6))

    val chart = ret.findViewById(R.id.chart).asInstanceOf[LineChart]
    drawSpeedChart(chart)
    chart.invalidate()

    ret
  }

  private def drawSpeedChart(chart: LineChart): Unit = {
    val dailyWindow = 24L * 3600 * 1000

    val baseTimestamp = (System.currentTimeMillis() / dailyWindow + 1) * dailyWindow
    val data = Seq.newBuilder[Entry]
    val range = 30

    for (i <- 0 until range) {
      val tsStart = baseTimestamp - (range - i) * dailyWindow
      val tsEnd = tsStart + dailyWindow

      val speed = files.filter {
        case (h, _) =>
          val st = h.header.startTime
          (st >= tsStart) && (st < tsEnd)
      } match {
        case Array() => Float.NaN
        case x => 3.6F * x.map { case (_, f) => f.map(_.avgSpeed).sum / f.size }.sum / x.length
      }

      data += new Entry(i, speed.toFloat)
    }

    val dataSet = new LineDataSet(data.result(), "Speed")
    dataSet.setDrawCircles(false)
    dataSet.setLineWidth(2F)
    dataSet.setColor(0xFFFF0000)
    dataSet.setDrawValues(false)
    dataSet.setMode(LineDataSet.Mode.STEPPED)

    chart.setData(new LineData(dataSet))
    chart.setDescription(getActivity.getString(R.string.avg_pedestrian_speed))
    chart.getXAxis.setGranularity(1F)
    chart.getXAxis.setValueFormatter(new AxisValueFormatter {
      val sdf = new SimpleDateFormat("dd.MM")

      override def getDecimalDigits: Int = 0
      override def getFormattedValue(value: Float, axis: AxisBase): String = {
        val ts = baseTimestamp - (dataSet.getEntryCount - value.toInt - 1) * dailyWindow
        sdf.format(new Date(ts))
      }
    })
  }
}
