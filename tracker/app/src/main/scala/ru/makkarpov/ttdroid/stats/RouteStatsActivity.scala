package ru.makkarpov.ttdroid.stats

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.{ExpandableListView, ListView}
import ru.makkarpov.ttdroid.R
import ru.makkarpov.ttdroid.stats.TrackGrouper.{PlaceGroupKey, RouteGroupKey, GroupKey}
import ru.makkarpov.ttdroid.stats.charts.HistogramAdapters.{IQR, ArriveTime}
import ru.makkarpov.ttdroid.stats.charts.Histogram
import ru.makkarpov.ttdroid.utils.Extensions._

/**
  * Created by user on 8/9/16.
  */
class RouteStatsActivity extends AppCompatActivity {
  lazy val groupKey = getIntent.extra[GroupKey]("key")
  lazy val data = TrackGrouper.getGroup(groupKey)
  lazy val fragmentDescription = findViewById(R.id.fragmentDetails).asInstanceOf[ExpandableListView]
  lazy val timeHistogram = findViewById(R.id.timeHistogram).asInstanceOf[Histogram]
  lazy val iqrHistogram = findViewById(R.id.iqrHistogram).asInstanceOf[Histogram]

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.stats_group_details)
    getSupportActionBar.setDisplayHomeAsUpEnabled(true)

    fragmentDescription.addFooterView(new View(this))
    fragmentDescription.setOnItemClickListener(null)

    groupKey match {
      case p: PlaceGroupKey =>
        fragmentDescription.setAdapter(new GroupDetailsAdapter.Place(this, p))
        fragmentDescription.setGroupIndicator(null)

      case r: RouteGroupKey =>
        fragmentDescription.setAdapter(new GroupDetailsAdapter.Route(this, r, data))
    }

    if (groupKey.isInstanceOf[PlaceGroupKey])
      fragmentDescription.setGroupIndicator(null)

    timeHistogram.setData(new ArriveTime(data))
    iqrHistogram.setData(new IQR(data))
  }
}
