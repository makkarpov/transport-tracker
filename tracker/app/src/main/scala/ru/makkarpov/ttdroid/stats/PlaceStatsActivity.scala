package ru.makkarpov.ttdroid.stats

import java.util.TimeZone

import android.app.TimePickerDialog.OnTimeSetListener
import android.app.{TimePickerDialog, Activity}
import android.content.DialogInterface
import android.content.DialogInterface.OnCancelListener
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View.OnClickListener
import android.view.{ViewGroup, View}
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget._
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import ru.makkarpov.ttdroid.R
import android.{R => aR}
import ru.makkarpov.ttdroid.data.AnalyzedTrack.GroundFragment
import ru.makkarpov.ttdroid.data.TrackFiles
import ru.makkarpov.ttdroid.stats.PlaceStatsActivity.RouteStats
import ru.makkarpov.ttdroid.stats.TrackGrouper.{RouteGroupKey, PlaceGroupKey}

import ru.makkarpov.ttdroid.utils.Extensions._
import ru.makkarpov.ttdroid.utils.Utils
import ru.makkarpov.ttdroid.utils.Utils.quantityFmt

/**
  * Created by user on 8/22/16.
  */
object PlaceStatsActivity {
  case class RouteStats(count: Int, avgTime: Long, avgWait: Long, extremes: Int, weight: Double)

  def calculateStats(f: Set[TrackFiles]) = {
    val avgWait = f.map(_.header.analyze.get.fragments.collect {
      case g: GroundFragment => g.waitTime
    }.sum).sum / f.size

    val times = f.map(_.trackTime)

    val ds = new DescriptiveStatistics()
    times.foreach(ds.addValue(_))

    val avgTime = ds.getMean.toLong
    val percentile = ds.getPercentile(75)
    val extremes = times.count(_ > percentile)

    val weight = 0.6 * avgTime + 0.2 * avgWait + 0.1 * (extremes.toDouble / f.size)

    RouteStats(f.size, avgTime, avgWait, extremes, weight)
  }
}

class PlaceStatsActivity extends AppCompatActivity { self =>
  lazy val key = getIntent.extra[PlaceGroupKey]("key")
  lazy val baseData = TrackGrouper.groupedPlaces(key)
  lazy val listView = findViewById(R.id.listView).asInstanceOf[ListView]

  lazy val rbAlways = findViewById(R.id.rbAlways).asInstanceOf[RadioButton]
  lazy val rbNow = findViewById(R.id.rbNow).asInstanceOf[RadioButton]
  lazy val rbTime = findViewById(R.id.rbTime).asInstanceOf[RadioButton]

  var dataView = Array.empty[(RouteGroupKey, RouteStats)]

  val timeWindow = 24 * 3600 * 1000
  val timeEps = 10 * 60 * 1000
  var selectedTime = System.currentTimeMillis() % timeWindow

  class ListAdapter
  extends ArrayAdapter[(RouteGroupKey, RouteStats)](this, R.layout.route_details_list_item, dataView) {
    val inflater = getLayoutInflater

    override def getView(position: Int, convertView: View, parent: ViewGroup): View = {
      val ret = convertView ifNull inflater.inflate(R.layout.route_details_list_item, parent, false)
      val (key, stats) = dataView(position)

      ret.findViewById(aR.id.text1).asInstanceOf[TextView].setText(key.name(self))
      ret.findViewById(aR.id.text2).asInstanceOf[TextView]
        .setText(quantityFmt(self, R.plurals.n_tracks, stats.count))

      ret.findViewById(R.id.avgWaitTime).asInstanceOf[TextView]
        .setText(Utils.intervalStr(stats.avgWait))

      ret.findViewById(R.id.avgPathTime).asInstanceOf[TextView]
        .setText(Utils.intervalStr(stats.avgTime))

      ret.findViewById(R.id.extremes).asInstanceOf[TextView]
        .setText(f"${stats.extremes} / ${stats.count} (${stats.extremes.toFloat * 100 / stats.count}%.2f %%)")

      ret
    }
  }

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.stats_place_details)
    getSupportActionBar.setDisplayHomeAsUpEnabled(true)

    refreshData()

    val refreshChangeListener = new OnCheckedChangeListener {
      override def onCheckedChanged(compoundButton: CompoundButton, b: Boolean): Unit =
        refreshData()
    }

    rbAlways.setOnCheckedChangeListener(refreshChangeListener)
    rbNow.setOnCheckedChangeListener(refreshChangeListener)

    rbTime.setOnClickListener(new OnClickListener {
      override def onClick(w: View): Unit = {
        val tz = TimeZone.getDefault
        val ctime = (selectedTime + tz.getOffset(selectedTime)) % timeWindow

        val dialog = new TimePickerDialog(self, new OnTimeSetListener {
          override def onTimeSet(timePicker: TimePicker, h: Int, m: Int): Unit = {
            selectedTime = h * 3600000 + m * 60000 - tz.getRawOffset
            if (selectedTime < 0)
              selectedTime += timeWindow

            rbTime.setText(f"$h%02d:$m%02d")
            refreshData()
          }
        }, (ctime / 3600000).toInt, (ctime / 60000 % 60).toInt, true)

        dialog.setOnCancelListener(new OnCancelListener {
          override def onCancel(dialogInterface: DialogInterface): Unit = {
            rbTime.setClickable(true)
          }
        })

        dialog.show()
      }
    })
  }

  private def refreshData(): Unit = {
    val time = System.currentTimeMillis() % timeWindow

    val rawData = baseData.filter(t => {
      if (rbAlways.isChecked) true
      else if (rbNow.isChecked) (t.header.startTime % timeWindow - time).abs <= timeEps
      else (t.header.startTime % timeWindow - selectedTime).abs <= timeEps
    })

    dataView = TrackGrouper
      .groupRoutes(rawData)
      .mapValues(PlaceStatsActivity.calculateStats)
      .toArray
      .sortBy { case (_, v) => -v.weight }

    listView.setAdapter(new ListAdapter)
  }
}
