package ru.makkarpov.ttdroid.data

import ru.makkarpov.ttdroid.data.TrackHeader.AutoStart

object TrackHeader {
  case class AutoStart(startPlace: MyPlace, finishPlace: Option[MyPlace])
}

case class TrackHeader(startTime: Long, finishTime: Option[Long], autoStart: Option[AutoStart] = None,
                       analyze: Option[AnalyzedTrack] = None) {


}