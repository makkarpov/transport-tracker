package ru.makkarpov.ttdroid

import android.app.{PendingIntent, Service}
import android.content.{BroadcastReceiver, Context, Intent, IntentFilter}
import android.hardware.{Sensor, SensorEvent, SensorEventListener, SensorManager}
import android.location.{Location, LocationListener, LocationManager, LocationProvider}
import android.os.{Bundle, IBinder}
import android.support.v4.app.NotificationCompat
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import ru.makkarpov.ttdroid.data.MyPlace
import Tracker.Frontend
import ru.makkarpov.ttdroid.map.MapActivity
import ru.makkarpov.ttdroid.utils.Extensions._
import TrackingService._

/**
  * Created by user on 7/6/16.
  */
object TrackingService {
  val TRACK_UPDATE = "ru.makkarpov.transporttracker.TRACK_UPDATE"
  val GPS_FIX_UPDATE = "android.location.GPS_FIX_CHANGE"
  val GPS_INTERVAL = 5000
}

class TrackingService extends Service with LocationListener with SensorEventListener {
  private var tracker: Tracker = _

  private val frontend = new Frontend {
    override def stop(): Unit = stopSelf()

    override def autoStop(place: MyPlace): Unit = {
      tracker.autoStop(place)
      stopSelf()
    }

    override def tracker: Tracker = TrackingService.this.tracker
  }

  val broadcastReceiver = new BroadcastReceiver {
    override def onReceive(context: Context, intent: Intent): Unit = intent.getAction match {
      case GPS_FIX_UPDATE =>
        val en = intent.getBooleanExtra("enabled", true)
        Log.i("TrackingService", s"Got GPS fix update: enabled=$en")
        tracker.onStatusUpdate(en)
      case _ =>
    }
  }

  lazy val locationManager =
    getSystemService(Context.LOCATION_SERVICE).asInstanceOf[LocationManager]

  lazy val sensorManager =
    getSystemService(Context.SENSOR_SERVICE).asInstanceOf[SensorManager]

  override def onBind(intent: Intent): IBinder = null


  override def onStartCommand(intent: Intent, flags: Int, startId: Int): Int = {
    super.onStartCommand(intent, flags, startId)

    val notification = new NotificationCompat.Builder(this)
      .setContentTitle(getResources.getString(R.string.running_notification_title))
      .setContentText(getResources.getString(R.string.running_notification_text))
      .setSmallIcon(R.drawable.ic_my_location_white)
      .setColor(0xFF00AA00)
      .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, classOf[MapActivity]), 0))
      .setOngoing(true)
      .build()

    startForeground(R.string.running_notification_title, notification)
    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_INTERVAL, 0.01F, this)

    for (s <- Option(sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)))
      sensorManager.registerListener(this, s, SensorManager.SENSOR_DELAY_FASTEST)

    tracker = new Tracker(TTDroid.tracksDirectory, Option(intent.extra[MyPlace]("autoStart")))

    TTDroid.currentTracking = Some(frontend)
    registerReceiver(broadcastReceiver, new IntentFilter(GPS_FIX_UPDATE))
    broadcastUpdate()

    Service.START_STICKY
  }

  override def onSensorChanged(sensorEvent: SensorEvent): Unit =
    tracker.onAccelerometerSample(sensorEvent)

  override def onAccuracyChanged(sensor: Sensor, i: Int): Unit = {}

  override def onProviderEnabled(s: String): Unit = {}

  override def onStatusChanged(s: String, i: Int, bundle: Bundle): Unit = {
    tracker.onStatusUpdate(i == LocationProvider.AVAILABLE)
    broadcastUpdate()
  }

  override def onLocationChanged(location: Location): Unit = {
    tracker.onLocationUpdate(location)
    broadcastUpdate()
  }

  override def onProviderDisabled(s: String): Unit = {
    tracker.onStatusUpdate(false)
    broadcastUpdate()
  }

  override def onDestroy(): Unit = {
    locationManager.removeUpdates(this)
    sensorManager.unregisterListener(this)
    unregisterReceiver(broadcastReceiver)
    TTDroid.currentTracking = None
    stopForeground(true)
    tracker.close()
    broadcastUpdate()
    super.onDestroy()
  }

  private def broadcastUpdate(): Unit =
    LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(TRACK_UPDATE))
}
