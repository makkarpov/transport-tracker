package ru.makkarpov.ttdroid.map

import android.content.Intent
import ru.makkarpov.ttdroid.data.TrackFiles
import ru.makkarpov.ttdroid.utils.Extensions._
import ru.makkarpov.ttdroid.utils.Utils
import ru.makkarpov.ttdroid.{R, TTDroid, TrackingService}

/**
  * Created by user on 7/7/16.
  */
class LiveDisplayedTrack(activity: MapActivity.Frontend) extends DisplayedTrack(activity) {
  override def currentFile = TTDroid.currentTracking.map(_.tracker.currentFile)

  override def fabVisible: Boolean = true

  override def fabPressed(): Unit = {
    TTDroid.currentTracking match {
      case Some(ct) =>
        // start activity for view
        ct.stop()

      case None =>
        activity.startService(new Intent(activity, classOf[TrackingService]))
    }
  }

  override def zoomEnabled: Boolean = TTDroid.currentTracking match {
    case Some(ct) => ct.tracker.currentFile.pointCount != 0
    case None => false
  }

  override def zoomPressed(): Unit = for (p <- points) zoomToTrack(p)

  override def updateRequested(): Unit = {
    TTDroid.currentTracking match {
      case Some(ct) =>
        val track = ct.tracker.currentFile.points use { _.all }

        activity.fab.setImageResource(R.drawable.ic_stop_white)
        activity.showSummary(ct.tracker.currentFile.header)
        val points = Utils.quantityFmt(activity, R.plurals.n_points, track.size)
        val status = activity.getString(if (ct.tracker.gpsStatus) R.string.gps_status_ok else R.string.gps_status_failure)
        activity.statusText.setText(activity.getString(R.string.track_recording) format (points, status))
        renderTrack(track)
      case None =>
        activity.fab.setImageResource(R.drawable.ic_record_white)
        activity.clearSummary()
        activity.statusText.setText(activity.getString(R.string.track_ready))
        Option(activity.map).foreach(_.clear())
    }
  }

  private def points =
    TTDroid.currentTracking.map(_.tracker.currentFile.points use { _.all })
}
