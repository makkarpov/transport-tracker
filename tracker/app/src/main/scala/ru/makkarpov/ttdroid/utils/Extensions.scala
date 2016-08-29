package ru.makkarpov.ttdroid.utils

import java.io.{InputStream, OutputStream, File, IOException}

import android.content.Intent
import android.database.Cursor
import android.os.Parcel

import scala.collection.generic.CanBuildFrom
import scala.language.higherKinds

/**
  * Created by user on 7/29/16.
  */
object Extensions {
  implicit class AnyRefExtensions[T <: AnyRef](val t: T) extends AnyVal {
    def ifNull[T1 >: T](orElse: T1): T1 = if (t == null) orElse else t
  }

  implicit class CursorExtensions(val c: Cursor) extends AnyVal {
    def fetchAll[T](f: Cursor => T): Seq[T] = {
      val r = Vector.newBuilder[T]

      if (c.moveToFirst())
        do { r += f(c) } while (c.moveToNext())

      r.result()
    }

    def getString(s: String) = c.getString(c.getColumnIndexOrThrow(s))
    def getInt(s: String) = c.getInt(c.getColumnIndexOrThrow(s))
  }

  implicit class ParcelExtensions(val p: Parcel) extends AnyVal {
    def writeColl[T](coll: Traversable[T])(f: T => Unit): Unit = {
      p.writeInt(coll.size)
      coll.foreach(f)
    }

    def readColl[T, C[X] <: Traversable[X]](f: => T)(implicit cbf: CanBuildFrom[_, T, C[T]]): C[T] = {
      val bld = cbf()

      for (i <- 0 until p.readInt())
        bld += f

      bld.result()
    }
  }

  implicit class IntentExtensions(val i: Intent) extends AnyVal {
    def extra[T <: Serializable](k: String): T =
      i.getSerializableExtra(k).asInstanceOf[T]
  }

  implicit class FileExtensions(val f: File) extends AnyVal {
    def / (s: String) = new File(f, s)

    def makeDirectory(): Unit =
      if (!f.exists() && !f.mkdirs())
        throw new IOException(s"Failed to create directory for $f")

    def makeParent(): Unit = {
      val parent = f.getParentFile
      if (!parent.exists() && !parent.mkdirs())
        throw new IOException(s"Failed to create parent directory for $f")
    }

    def deleteDirectory(): Unit = {
      if (f.isDirectory) {
        val fls = f.listFiles()
        if (fls ne null)
          for (x <- fls) x.deleteDirectory()
      }

      if (!f.delete())
        throw new IOException(s"Failed to delete $f")
    }
  }

  implicit class IntExtension(val i: Int) extends AnyVal {
    def clamp(min: Int, max: Int): Int =
      if (i < min) min else if (i > max) max else i

    def %%(mod: Int) = if (i >= 0) i % mod else i % mod + mod
  }

  implicit class DoubleExtensions(val f: Double) extends AnyVal {
    def clamp(min: Double, max: Double): Double =
      if (f < min) min else if (f > max) max else f
  }

  implicit class CloseableExtensions[T <: AutoCloseable](val t: T) extends AnyVal {
    def use[R](f: T => R): R = {
      var wasClosed = false
      try f(t)
      catch {
        case e: Exception =>
          try {
            wasClosed = true
            t.close()
          } catch {
            case ex: Exception =>
              e.addSuppressed(ex)
          }

          throw e
      } finally {
        if (!wasClosed) t.close()
      }
    }
  }

  implicit class OutputStreamExtensions(val os: OutputStream) extends AnyVal {
    def << (is: InputStream): Unit = {
      val buf = new Array[Byte](1024)
      var r = 0

      do {
        r = is.read(buf)
        if (r > 0) os.write(buf, 0, r)
      } while (r != -1)
    }
  }
}
