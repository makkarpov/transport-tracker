package ru.makkarpov.ttdroid

import java.io.File

import android.os.Environment

/**
  * Created by user on 8/1/16.
  */
object TTDroid {
  var currentTracking = Option.empty[Tracker.Frontend]
  def tracksDirectory = new File(Environment.getExternalStorageDirectory, "TransportTracker")
}
