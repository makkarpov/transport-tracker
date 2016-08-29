package ru.makkarpov.ttdroid

import java.io._

import android.hardware.SensorEvent
import android.location.Location
import android.util.Log
import ru.makkarpov.ttdroid.accelerometer.{LinearAccelerationSensor, MovementHistory}
import ru.makkarpov.ttdroid.data.TrackHeader.AutoStart
import ru.makkarpov.ttdroid.data.TrackPoint.{Point, PointType}
import ru.makkarpov.ttdroid.data.{MyPlace, TrackFiles, TrackHeader, TrackPoint}
import ru.makkarpov.ttdroid.utils.Extensions._
import ru.makkarpov.ttdroid.utils.Utils._

/**
  * Created by user on 7/7/16.
  */
object Tracker {
  trait Frontend {
    def tracker: Tracker
    def stop(): Unit
    def autoStop(place: MyPlace): Unit
  }
}

class Tracker(targetDir: File, autoStart: Option[MyPlace]) extends Closeable {
  val currentFile = findUnfinished().getOrElse {
    val f = new TrackFiles(targetDir / s"t_${System.currentTimeMillis}")
    f.header = new TrackHeader(System.currentTimeMillis(), None, autoStart.map(AutoStart(_, None)))
    f
  }

  val points = currentFile.points

  val movement = new MovementHistory
  val linearAcceleration = new LinearAccelerationSensor(100, movement)

  @volatile var gpsStatus = true
  @volatile var lastPoint = Option.empty[Point]
  @volatile var pointRepeats = 0

  Log.i("Tracker", s"Track started")

  def onLocationUpdate(loc: Location): Unit = {
    val time = System.currentTimeMillis()
    val point = Point(loc)

    if (lastPoint.contains(point)) {
      // Вероятно, сигнал GPS потерян - GPS в нормальных условиях не должно давать подряд
      // идеально совпадающие результаты - хоть какой-то шум, да будет, а Android будет исправно
      // повторять последнюю известную позицию в условиях отсутствия сигнала (некоторое время,
      // потом и Android скажет, что нет сигнала). Задетектим это раньше, чем придет отдельное
      // уведомление + не будем сохранить повторяющиеся точки в лог.

      pointRepeats += 1
      Log.w("Tracker", s"Received repeated point ($pointRepeats times). Possibly GPS signal lost.")

      if (pointRepeats >= 3)
        onStatusUpdate(false) // А если это продолжается уже 15 секунд - то сигнал точно потерян

      return
    }

    lastPoint = Some(point)

    val kind = if (gpsStatus) PointType.Regular else PointType.GPSAcquired
    val entry = TrackPoint(time, Point(loc), movement.getMode, kind)

    points.write(entry)
    pointRepeats = 0
    gpsStatus = true

    Log.i("Tracker", s"Received location update (${rfcDateStr(time)}): $entry")
    Log.i("Tracker", s"Current track length is ${points.length} point(s)")
  }

  def onStatusUpdate(status: Boolean): Unit = {
    val time = System.currentTimeMillis()
    Log.i("Tracker", s"Received status update (${rfcDateStr(time)}): GPS is ${if (status) "ok" else "fail"}")
    if (!status && gpsStatus) {
      for (p <- lastPoint)
        points.write(TrackPoint(time, p, movement.getMode, PointType.GPSLost))
      gpsStatus = false
    }
  }

  def onAccelerometerSample(evt: SensorEvent): Unit =
    linearAcceleration.sampleReceived(evt.values)

  def wasAutoStarted: Boolean = autoStart.isDefined

  def autoStop(place: MyPlace): Unit = {
    val h = currentFile.header
    currentFile.header = h.copy(autoStart = h.autoStart.map(x => x.copy(x.startPlace, Some(place))))
  }

  def close(): Unit = {
    val time = System.currentTimeMillis()
    Log.i("Tracker", s"Track closed at ${rfcDateStr(time)}")
    points.close()
    currentFile.header = currentFile.header.copy(finishTime = Some(System.currentTimeMillis()))
  }

  private def findUnfinished(): Option[TrackFiles] =
    Option(targetDir.listFiles()).getOrElse(Array.empty)
      .map(new TrackFiles(_)).find(_.header.finishTime.isEmpty)
}
