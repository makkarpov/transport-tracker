package ru.makkarpov.ttdroid.map

import android.app.Activity
import android.util.Log
import android.view.{LayoutInflater, ViewGroup, View}
import android.widget.{TextView, BaseExpandableListAdapter}
import ru.makkarpov.ttdroid.R
import ru.makkarpov.ttdroid.data.AnalyzedTrack._
import ru.makkarpov.ttdroid.utils.Utils

/**
  * Created by user on 8/5/16.
  */
class AnalyzedListAdapter(fragments: Seq[TrackFragment], ctx: Activity)
extends BaseExpandableListAdapter {
  override def getGroupView(pos: Int, expanded: Boolean, view: View, viewGroup: ViewGroup): View = {
    val ret =
      if (view == null) ctx.getLayoutInflater.inflate(R.layout.expandable_list_item, viewGroup, false)
      else view

    val frag = fragments(pos)

    ret.findViewById(android.R.id.text1).asInstanceOf[TextView].setText(frag match {
      case w: WalkFragment => ctx.getString(R.string.type_walk)
      case r: RawFragment => ctx.getString(R.string.type_raw)
      case b: GroundFragment =>
        val kind = b.route.kind match {
          case GroundKind.Bus => ctx.getString(R.string.type_ground_bus)
          case GroundKind.Trolleybus => ctx.getString(R.string.type_ground_trolleybus)
          case GroundKind.Tram => ctx.getString(R.string.type_ground_tram)
          case GroundKind.Monorail => ctx.getString(R.string.type_ground_monorail)
        }

        ctx.getString(R.string.type_ground) format (kind, b.route.index)

      case u: UndergroundFragment => ctx.getString(R.string.type_underground)
    })

    ret.findViewById(android.R.id.text2).asInstanceOf[TextView].setText(frag match {
      case r: RawFragment => Utils.quantityFmt(ctx, R.plurals.n_meters, r.metadata.distance.toInt)
      case w: WalkFragment => Utils.quantityFmt(ctx, R.plurals.n_meters, w.metadata.distance.toInt)
      case b: GroundFragment => s"${b.stations.enter.name} — ${b.stations.exit.name}"
      case u: UndergroundFragment => s"${u.stations.enter.station} — ${u.stations.exit.station}"
    })

    ret
  }

  override def getGroupCount: Int = fragments.size
  override def getGroupId(i: Int): Long = i * 2
  override def getGroup(i: Int): AnyRef = fragments(i)
  override def getChildrenCount(i: Int): Int = fragments(i) match {
    case g: GroundFragment => 1
    case _ => 0
  }

  override def isChildSelectable(i: Int, i1: Int): Boolean = false

  override def getChildView(pos: Int, childPos: Int, expanded: Boolean, view: View,
                            viewGroup: ViewGroup): View =
  {
    val ret = ctx.getLayoutInflater.inflate(R.layout.analyze_fragment_details, viewGroup, false)
    val frag = fragments(pos)

    frag match {
      case g: GroundFragment =>
        ret.findViewById(R.id.waitTime).asInstanceOf[TextView].setText(Utils.intervalStr(g.waitTime))

      case _ =>
        ret.findViewById(R.id.waitTime).setVisibility(View.GONE)
        ret.findViewById(R.id.waitTimeLabel).setVisibility(View.GONE)
    }

    ret
  }

  override def getChildId(i: Int, i1: Int): Long = i * 2 + 1
  override def getChild(i: Int, i1: Int): AnyRef = fragments(i)

  override def hasStableIds: Boolean = true
}
