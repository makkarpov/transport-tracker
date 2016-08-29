package ru.makkarpov.ttdroid.data

import java.io.{Closeable, File, RandomAccessFile}

import ru.makkarpov.ttdroid.accelerometer.Movement
import ru.makkarpov.ttdroid.data.TrackPoint.Point
import ru.makkarpov.ttdroid.data.TrackPointFile._

/**
  * Created by user on 7/12/16.
  */
object TrackPointFile {
  /* long: time, byte: type, movement, doubles: lat, lon, alt, speed, bearing */
  val PointLength = 8 + 2 + 8 * 5
}

class TrackPointFile(raf: RandomAccessFile) extends Closeable {
  def this(f: File, mode: String) = this(new RandomAccessFile(f, mode))

  // Set append mode
  raf.seek(raf.length())

  def close(): Unit = raf.close()
  def position(): Long = raf.getFilePointer / PointLength
  def seek(idx: Long): Unit = raf.seek(idx * PointLength)
  def length = (raf.length() / PointLength).toInt
  def setLength(len: Long): Unit = raf.setLength(len * PointLength)

  def read(): TrackPoint = {
    val time = raf.readLong()
    val tpe = TrackPoint.PointType(raf.readUnsignedByte())
    val movement = Movement(raf.readUnsignedByte())
    val lat = raf.readDouble()
    val lon = raf.readDouble()
    val alt = raf.readDouble()
    val speed = raf.readDouble()
    val bearing = raf.readDouble()

    TrackPoint(time, Point(lat, lon, alt, speed, bearing), movement, tpe)
  }

  def write(p: TrackPoint): Unit = {
    raf.writeLong(p.time)
    raf.writeByte(p.tpe.id)
    raf.writeByte(p.movement.id)
    raf.writeDouble(p.loc.lat)
    raf.writeDouble(p.loc.lng)
    raf.writeDouble(p.loc.alt)
    raf.writeDouble(p.loc.speed)
    raf.writeDouble(p.loc.bearing)
  }

  def all: Seq[TrackPoint] = {
    seek(0)
    (0 until length).map(_ => read())
  }

  def iterator: Iterator[TrackPoint] = {
    seek(0)
    Iterator.range(0, length).map(_ => read())
  }

  def autoCloseIterator: Iterator[TrackPoint] = new Iterator[TrackPoint] {
    @volatile var wasClosed = false
    seek(0)

    override def hasNext: Boolean = {
      if (wasClosed)
        return false
      if (position() >= TrackPointFile.this.length) {
        close()
        wasClosed = true
        false
      } else true
    }

    override def next() = {
      if (!hasNext)
        throw new IllegalArgumentException("read beyound end of iterator")
      read()
    }
  }
}
