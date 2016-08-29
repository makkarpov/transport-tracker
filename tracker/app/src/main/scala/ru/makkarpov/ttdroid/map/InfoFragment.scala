package ru.makkarpov.ttdroid.map

import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.{View, ViewGroup, LayoutInflater}
import android.widget.TextView
import ru.makkarpov.ttdroid.R
import ru.makkarpov.ttdroid.data.TrackHeader.AutoStart
import ru.makkarpov.ttdroid.data.{MyPlace, TrackHeader}
import ru.makkarpov.ttdroid.settings.SettingsDatabase
import ru.makkarpov.ttdroid.utils.Utils._
import scala.collection.JavaConversions._

/**
  * Created by user on 8/3/16.
  */
class InfoFragment extends MapBaseFragment {
  var recordingStart: TextView = _
  var recordingFinish: TextView = _
  var asSource: TextView = _
  var asDestination: TextView = _

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup,
                            savedInstanceState: Bundle): View = {
    val ret = inflater.inflate(R.layout.main_tab_info, container, false)

    recordingStart = ret.findViewById(R.id.recordingStart).asInstanceOf[TextView]
    recordingFinish = ret.findViewById(R.id.recordingFinish).asInstanceOf[TextView]
    asSource = ret.findViewById(R.id.asSource).asInstanceOf[TextView]
    asDestination = ret.findViewById(R.id.asDestination).asInstanceOf[TextView]

    ret
  }

  def showSummary(hdr: TrackHeader): Unit = {
    if (recordingStart == null)
      return

    recordingStart.setText(dateStr(hdr.startTime))
    recordingFinish.setText(hdr.finishTime.map(dateStr).getOrElse(""))

    hdr.autoStart match {
      case Some(as) =>
        asSource.setText(as.startPlace.name)
        asDestination.setText(as.finishPlace.map(_.name).getOrElse(""))

      case None =>
        asSource.setText("")
        asDestination.setText("")
    }
  }

  def clearSummary(): Unit = {
    recordingStart.setText("")
    recordingFinish.setText("")
    asSource.setText("")
    asDestination.setText("")
  }

  def setPlacesPressed(): Unit = {
    val places = new SettingsDatabase(getActivity).getLocations
    val placesArray = places.map(_.name.asInstanceOf[CharSequence]).toArray

    def askPlace(title: String)(callback: MyPlace => Unit): Unit = {
      var checked = 0
      val dialog = new AlertDialog.Builder(getActivity)
        .setTitle(title)
        .setSingleChoiceItems(placesArray, checked, new OnClickListener {
          override def onClick(dialogInterface: DialogInterface, i: Int): Unit =
            checked = i
        })
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(android.R.string.ok, new OnClickListener {
          override def onClick(dialogInterface: DialogInterface, i: Int): Unit =
            callback(places(checked))
        })
        .create()

      dialog.show()
    }

    askPlace(mapActivity.getString(R.string.departure_point)) { start =>
      askPlace(mapActivity.getString(R.string.arrival_point)) { finish =>
        for (f <- mapActivity.displayedTrack.currentFile) {
          f.header = f.header.copy(autoStart = Some(AutoStart(start, Some(finish))))
          showSummary(f.header)
        }
      }
    }
  }
}