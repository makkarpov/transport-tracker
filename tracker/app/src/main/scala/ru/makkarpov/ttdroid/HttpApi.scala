package ru.makkarpov.ttdroid

import java.io.{ByteArrayOutputStream, FileInputStream}
import java.net.{HttpURLConnection, URL}
import java.nio.charset.StandardCharsets

import android.app.Activity
import android.preference.PreferenceManager
import ru.makkarpov.ttdroid.data.{AnalyzedTrack, TrackFiles}
import ru.makkarpov.ttdroid.utils.Extensions._

/**
  * Created by user on 8/5/16.
  */
object HttpApi {
  def request(ctx: Activity, f: TrackFiles)(h: Either[Throwable, AnalyzedTrack] => Unit): Unit = {
    new Thread {
      setName("HTTP API thread")
      setDaemon(true)
      start()

      override def run(): Unit = {
        val resp = syncRequest(ctx, f)

        ctx.runOnUiThread(new Runnable {
          override def run(): Unit = h(resp)
        })
      }
    }
  }

  def syncRequest(ctx: Activity, f: TrackFiles): Either[Throwable, AnalyzedTrack] = {
    val url = PreferenceManager.getDefaultSharedPreferences(ctx)
      .getString("api_url", "")

    try {
      val urlc = new URL(url).openConnection().asInstanceOf[HttpURLConnection]

      urlc.setRequestMethod("POST")
      urlc.setDoOutput(true)
      urlc.setChunkedStreamingMode(512)

      new FileInputStream(f.pointsFile) use { fis =>
        urlc.getOutputStream use { _ << fis }
      }

      val baos = new ByteArrayOutputStream()
      urlc.getInputStream use { baos << _ }

      val json = new String(baos.toByteArray, StandardCharsets.UTF_8)

      Right(AnalyzedTrack.readFromAPI(json))
    } catch {
      case t: Throwable => Left(t)
    }
  }
}