package ru.makkarpov.ttdroid.stats

import android.app.Activity
import android.util.Log
import android.view.{View, ViewGroup}
import android.widget.{BaseExpandableListAdapter, ArrayAdapter, TextView}
import ru.makkarpov.ttdroid.R
import ru.makkarpov.ttdroid.data.AnalyzedTrack.GroundKind
import ru.makkarpov.ttdroid.data.TrackFiles
import ru.makkarpov.ttdroid.stats.TrackGrouper._
import ru.makkarpov.ttdroid.stats.charts.Histogram
import ru.makkarpov.ttdroid.stats.charts.HistogramAdapters.WaitTime
import ru.makkarpov.ttdroid.utils.Extensions._
import ru.makkarpov.ttdroid.utils.Utils

import scala.collection.JavaConversions._

/**
  * Created by user on 8/12/16.
  */
object GroupDetailsAdapter {
  class Place(ctx: Activity, key: PlaceGroupKey) extends BaseExpandableListAdapter {
    val inflater = ctx.getLayoutInflater

    override def getGroupView(i: Int, b: Boolean, view: View, viewGroup: ViewGroup): View = {
      val ret = view ifNull inflater.inflate(android.R.layout.simple_list_item_1, viewGroup, false)
      val t = s"${key.start} — ${key.finish}"
      ret.findViewById(android.R.id.text1).asInstanceOf[TextView].setText(t)
      ret
    }

    // dummy methods:
    override def getGroupCount: Int = 1
    override def getChildId(i: Int, i1: Int): Long = i
    override def getChild(i: Int, i1: Int): AnyRef = null
    override def isChildSelectable(i: Int, i1: Int): Boolean = false
    override def getGroupId(i: Int): Long = i
    override def getGroup(i: Int): AnyRef = key
    override def getChildrenCount(i: Int): Int = 0
    override def hasStableIds: Boolean = true
    override def getChildView(i: Int, i1: Int, b: Boolean, view: View, viewGroup: ViewGroup): View = null
  }

  class Route(ctx: Activity, key: RouteGroupKey, data: Set[TrackFiles]) extends BaseExpandableListAdapter {
    val inflater = ctx.getLayoutInflater

    override def getGroupView(i: Int, b: Boolean, view: View, viewGroup: ViewGroup): View = {
      val ret = view ifNull inflater.inflate(R.layout.expandable_list_item, viewGroup, false)

      val frag = key.entries(i)

      ret.findViewById(android.R.id.text1).asInstanceOf[TextView].setText(frag match {
        case b: Ground =>
          val kind = b.route.kind match {
            case GroundKind.Bus => ctx.getString(R.string.type_ground_bus)
            case GroundKind.Trolleybus => ctx.getString(R.string.type_ground_trolleybus)
            case GroundKind.Tram => ctx.getString(R.string.type_ground_tram)
            case GroundKind.Monorail => ctx.getString(R.string.type_ground_monorail)
          }

          ctx.getString(R.string.type_ground) format (kind, b.route.index)

        case u: Underground => ctx.getString(R.string.type_underground)
      })

      ret.findViewById(android.R.id.text2).asInstanceOf[TextView].setText(frag match {
        case b: Ground => s"${b.trip.enter.name} — ${b.trip.exit.name}"
        case u: Underground => s"${u.trip.enter.station} — ${u.trip.exit.station}"
      })

      ret
    }

    override def getChildView(i: Int, i1: Int, b: Boolean, view: View, viewGroup: ViewGroup): View = {
      val ret = inflater.inflate(R.layout.stats_ground_details, viewGroup, false)

      ret.findViewById(R.id.waitTimeHistogram).asInstanceOf[Histogram]
        .setData(new WaitTime(data, i))

      ret
    }

    override def getGroupCount: Int = key.entries.size
    override def getChildId(i: Int, i1: Int): Long = i
    override def getChild(i: Int, i1: Int): AnyRef = Unit
    override def isChildSelectable(i: Int, i1: Int): Boolean = false
    override def getGroupId(i: Int): Long = i
    override def getGroup(i: Int): AnyRef = key.entries(i)

    override def getChildrenCount(i: Int): Int = key.entries(i) match {
      case _: Ground => 1
      case _ => 0
    }

    override def hasStableIds: Boolean = true
  }

}
