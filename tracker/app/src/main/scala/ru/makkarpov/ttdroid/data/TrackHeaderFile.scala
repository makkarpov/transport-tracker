package ru.makkarpov.ttdroid.data

import java.io.{ByteArrayOutputStream, File, FileInputStream, FileOutputStream}
import java.nio.charset.StandardCharsets

import ru.makkarpov.ttdroid.utils.Extensions
import upickle.json

import Extensions._

/**
  * Created by user on 7/12/16.
  */
object TrackHeaderFile {
  private implicit val pickler = upickle.default.macroRW[TrackHeader]

  // TODO: serialize directly to stream, uPickle does not support this yet

  def read(f: File): TrackHeader = {
    val baos = new ByteArrayOutputStream()
    new FileInputStream(f) use { fis =>
      val buf = new Array[Byte](256)
      var r = 0

      do {
        r = fis.read(buf)
        if (r > 0) baos.write(buf, 0, r)
      } while (r != -1)
    }

    val str = new String(baos.toByteArray, StandardCharsets.UTF_8)

    pickler.read(json.read(str))
  }

  def write(f: File, hdr: TrackHeader): Unit = {
    val str = pickler.write(hdr).toString()

    new FileOutputStream(f) use { fos =>
      fos.write(str.getBytes(StandardCharsets.UTF_8))
    }
  }
}