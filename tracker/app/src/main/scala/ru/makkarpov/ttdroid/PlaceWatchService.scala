package ru.makkarpov.ttdroid

import android.app.Service
import android.content.{IntentFilter, Context, BroadcastReceiver, Intent}
import android.net.wifi.WifiManager
import android.os.IBinder
import android.preference.PreferenceManager
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import ru.makkarpov.ttdroid.data.MyPlace
import ru.makkarpov.ttdroid.settings.{SettingsDatabase, SettingsActivity}
import scala.collection.JavaConversions._

/**
  * Created by user on 8/1/16.
  */
class PlaceWatchService extends Service {
  override def onBind(intent: Intent): IBinder = null

  lazy val database = new SettingsDatabase(this)
  lazy val wifiManager = getSystemService(Context.WIFI_SERVICE).asInstanceOf[WifiManager]

  var places = Set.empty[MyPlace]
  var lastPlaces = Set.empty[MyPlace]
  var trackingStarted = false

  val broadcastReceiver = new BroadcastReceiver {
    override def onReceive(context: Context, intent: Intent): Unit = intent.getAction match {
      case SettingsActivity.SettingsChangedAction =>
        val pref = PreferenceManager.getDefaultSharedPreferences(PlaceWatchService.this)
        if (!pref.getBoolean("auto_start", true)) {
          Log.i("PlaceWatchService", "Auto-start was disabled, stopping self.")
          stopSelf()
        } else {
          Log.i("PlaceWatchService", "Settings were changed, refreshing place list.")
          places = database.getLocations.toSet
          Log.i("PlaceWatchService", s"Loaded ${places.size} places")
          refreshNetworks()
        }

      case _ => refreshNetworks()
    }
  }

  override def onCreate(): Unit = {
    super.onCreate()

    Log.i("PlaceWatchService", "Started place watch service")

    val wifiFilter = new IntentFilter()
    wifiFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
    wifiFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)
    wifiFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)

    registerReceiver(broadcastReceiver, wifiFilter)
    LocalBroadcastManager.getInstance(this)
      .registerReceiver(broadcastReceiver, new IntentFilter(SettingsActivity.SettingsChangedAction))

    places = database.getLocations.toSet
    refreshNetworks()
  }

  private def refreshNetworks(): Unit = {
    if (!wifiManager.isWifiEnabled)
      return

    val ssids = (wifiManager.getScanResults.map(_.SSID) ++
      Option(wifiManager.getConnectionInfo).map(_.getSSID)).toSet

    val current = places.filter(x => ssids.intersect(x.networks.map(_.ssid)).nonEmpty)

    Log.i("PlaceWatchService", s"Current places: ${current.map(_.name).mkString(", ")}")
    Log.i("PlaceWatchService", s"Tracking started: $trackingStarted, last places: " +
              lastPlaces.map(_.name).mkString(", "))

    if (current.isEmpty ^ trackingStarted) {
      if (current.isEmpty) {
        if (lastPlaces.nonEmpty)
          startTracking(lastPlaces.head)
        trackingStarted = true
      } else {
        stopTracking(current.head)
        trackingStarted = false
      }
    }

    lastPlaces = current
  }

  private def startTracking(srcPlace: MyPlace): Unit = {
    Log.i("PlaceWatchService", s"Starting tracking as user left place $srcPlace")
    val intent = new Intent(this, classOf[TrackingService])
    intent.putExtra("autoStart", srcPlace)
    startService(intent)
  }

  private def stopTracking(dstPlace: MyPlace): Unit = {
    TTDroid.currentTracking match {
      case Some(t) if t.tracker.wasAutoStarted =>
        Log.i("PlaceWatchService", s"Stopping tracking as user has arrived to place $dstPlace")
        t.autoStop(dstPlace)
      case _ =>
    }
  }

  override def onDestroy(): Unit = {
    unregisterReceiver(broadcastReceiver)
    LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    super.onDestroy()
  }
}
