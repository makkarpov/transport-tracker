package ru.makkarpov.ttdroid.map

import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.{LatLng, LatLngBounds, TileOverlayOptions}
import ru.makkarpov.ttdroid.data.{TrackFiles, TrackPoint}
import ru.makkarpov.ttdroid.map.TrackOverlayProvider.ShowOptions

import scala.Double.{MaxValue, MinValue}

/**
  * Created by user on 7/7/16.
  */
abstract class DisplayedTrack(activity: MapActivity.Frontend) {
  def fabVisible: Boolean = false
  def fabPressed(): Unit = {}
  def zoomPressed(): Unit = {}
  def zoomEnabled: Boolean = false
  def updateRequested(): Unit

  def canBeAnalyzed: Boolean = false
  def currentFile: Option[TrackFiles]

  protected var wasZoomed = false

  def renderTrack(track: Seq[TrackPoint]): Unit = {
    if (activity.map eq null)
      return

    val opts = ShowOptions(
      mode = activity.colorMode,
      pointNumbers = PreferenceManager.getDefaultSharedPreferences(activity)
                                      .getBoolean("show_point_numbers", false)
    )

    activity.map.clear()
    activity.map.addTileOverlay(new TileOverlayOptions()
        .tileProvider(new TrackOverlayProvider(activity, track, opts))
        .transparency(0.1F))

    if (!wasZoomed)
      zoomToTrack(track)
  }

  def zoomToTrack(track: Seq[TrackPoint]): Unit = {
    if (track.isEmpty)
      return

    try {
      if (track.size == 1) {
        val head = track.head.loc
        activity.map.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(head.lat, head.lng)))
      } else {
        val (minLat, minLng, maxLat, maxLng) =
          track.map(_.loc).foldLeft(MaxValue, MaxValue, MinValue, MinValue) {
            case ((a, b, c, d), loc) =>
              (a min loc.lat, b min loc.lng, c max loc.lat, d max loc.lng)
          }

        val bounds = new LatLngBounds(new LatLng(minLat, minLng), new LatLng(maxLat, maxLng))
        activity.map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 10))
      }

      wasZoomed = true
    } catch {
      case _: IllegalStateException =>
        /*
         * https://developers.google.com/android/reference/com/google/android/gms/maps/CameraUpdateFactory.html#newLatLngBounds(com.google.android.gms.maps.model.LatLngBounds, int)
         *
         * May happen if map layout was not determined yet.
         */
        Log.e("DisplayedTrack", "Zoom failed with IllegalStateException")
    }
  }
}
