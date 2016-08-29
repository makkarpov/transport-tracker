package ru.makkarpov.ttdroid.stats

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.{View, ViewGroup, LayoutInflater}
import android.widget.AdapterView.OnItemClickListener
import android.widget.{AdapterView, TextView, ArrayAdapter, ListView}
import ru.makkarpov.ttdroid.R
import ru.makkarpov.ttdroid.data.AnalyzedTrack.GroundKind
import ru.makkarpov.ttdroid.settings.SettingsDatabase
import ru.makkarpov.ttdroid.stats.InGroupFragment.ListEntry
import ru.makkarpov.ttdroid.stats.TrackGrouper.{PlaceGroupKey, RouteGroupKey, GroupKey}
import ru.makkarpov.ttdroid.utils.Utils

/**
  * Created by user on 8/9/16.
  */
object InGroupFragment {
  case class ListEntry(name: String, count: Int, key: GroupKey)
}

class InGroupFragment(places: Boolean) extends BaseFragment {
  val args = new Bundle()
  args.putBoolean("places", places)
  setArguments(args)

  lazy val groupedTracks =
    if (getArguments.getBoolean("places")) TrackGrouper.groupedPlaces
    else TrackGrouper.groupedRoutes

  lazy val database = new SettingsDatabase(getActivity)
  lazy val viewData = listViewData

  private def listViewData: Array[ListEntry] = {
    val groupNames = database.getRoutes.map(x => x.key -> x.name).toMap

    groupedTracks.map{
      case (k: RouteGroupKey, v) =>
        val name = groupNames.getOrElse(k, k.name(getActivity))
        ListEntry(name, v.size, k)

      case (k: PlaceGroupKey, v) =>
        ListEntry(s"${k.start} - ${k.finish}", v.size, k)
    }.toArray
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    val ret = inflater.inflate(R.layout.stats_in_fragment, container, false)
    val list = ret.findViewById(R.id.listView).asInstanceOf[ListView]

    list.setAdapter(new ArrayAdapter[ListEntry](getActivity, R.layout.two_line_list_item, listViewData) {
      override def getView(position: Int, convertView: View, parent: ViewGroup): View = {
        val v = convertView match {
          case null => getActivity.getLayoutInflater.inflate(R.layout.two_line_list_item, parent, false)
          case x => x
        }

        val itm = getItem(position)

        v.findViewById(android.R.id.text1).asInstanceOf[TextView].setText(itm.name)
        v.findViewById(android.R.id.text2).asInstanceOf[TextView]
          .setText(Utils.quantityFmt(getActivity, R.plurals.n_tracks, itm.count))

        v
      }
    })

    list.setOnItemClickListener(new OnItemClickListener {
      override def onItemClick(adapter: AdapterView[_], view: View, pos: Int, l: Long): Unit = {
        val itm = listViewData(pos)

        val intent = new Intent(getActivity, itm.key match {
          case p: PlaceGroupKey => classOf[PlaceStatsActivity]
          case r: RouteGroupKey => classOf[RouteStatsActivity]
        })

        intent.putExtra("key", itm.key)
        startActivity(intent)
      }
    })

    ret
  }
}
