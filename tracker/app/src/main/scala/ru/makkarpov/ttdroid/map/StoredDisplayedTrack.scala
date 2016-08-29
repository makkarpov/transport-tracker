package ru.makkarpov.ttdroid.map

import java.io.File

import ru.makkarpov.ttdroid.R
import ru.makkarpov.ttdroid.data.TrackFiles
import ru.makkarpov.ttdroid.utils.Extensions._
import ru.makkarpov.ttdroid.utils.Utils._

/**
  * Created by user on 7/7/16.
  */
class StoredDisplayedTrack(activity: MapActivity.Frontend, file: File) extends DisplayedTrack(activity) {
  val trackFile = new TrackFiles(file)
  val header = trackFile.header
  val rawTrack = trackFile.points use { _.all }

  override def canBeAnalyzed = true
  override def currentFile = Some(trackFile)

  override def zoomPressed(): Unit = zoomToTrack(rawTrack)
  override def zoomEnabled: Boolean = rawTrack.nonEmpty

  override def updateRequested(): Unit = {
    activity.showSummary(header)
    activity.statusText.setText(s"${dateStr(header.startTime)} | " +
      s"${quantityFmt(activity, R.plurals.n_points, rawTrack.size)}.")
    renderTrack(rawTrack)
  }
}
