package ru.makkarpov.ttdroid.settings

import android.content.{ContentValues, Context}
import android.database.sqlite.{SQLiteDatabase, SQLiteOpenHelper}
import ru.makkarpov.ttdroid.stats.TrackGrouper._
import ru.makkarpov.ttdroid.utils.Extensions
import Extensions._
import ru.makkarpov.ttdroid.data.MyPlace
import ru.makkarpov.ttdroid.data.MyPlace.WiFiEntry
import ru.makkarpov.ttdroid.settings.SettingsDatabase._
import upickle.default.{read, write}

/**
  * Created by user on 7/29/16.
  */
object SettingsDatabase {
  val DatabaseName = "settings.db"

  val Migrations = Seq(
    """CREATE TABLE my_locations (
      |  id INTEGER NOT NULL UNIQUE PRIMARY KEY,
      |  name TEXT NOT NULL,
      |  networks TEXT NOT NULL
      |);
    """.stripMargin,
    """CREATE TABLE my_routes (
      |  id INTEGER NOT NULL UNIQUE PRIMARY KEY,
      |  name TEXT NOT NULL,
      |  group_key TEXT NOT NULL
      |);
    """.stripMargin
  )
}

class SettingsDatabase(ctx: Context) extends SQLiteOpenHelper(ctx, DatabaseName, null, Migrations.size) {
  override def onCreate(db: SQLiteDatabase): Unit = {
    for (s <- Migrations)
      db.execSQL(s)
  }

  override def onUpgrade(db: SQLiteDatabase, from: Int, to: Int): Unit = {
    for (s <- Migrations.drop(from))
      db.execSQL(s)
  }

  // -- // -- // -- // -- // -- // -- // -- // -- // -- // -- // -- // -- // -- // -- // -- // -- //

  def getLocations: Seq[MyPlace] = {
    val db = getReadableDatabase

    val cur = db.rawQuery("SELECT * FROM my_locations", Array())

    cur.fetchAll { c =>
      MyPlace(c.getInt("id"), c.getString("name"), read[Set[WiFiEntry]](c.getString("networks")))
    }
  }

  def storeLocation(l: MyPlace): Unit = {
    val cv = new ContentValues()

    cv.put("name", l.name)
    cv.put("networks", write(l.networks))

    if (l.id > 0) getWritableDatabase.update("my_locations", cv, "id = ?", Array( l.id.toString ))
    else getWritableDatabase.insert("my_locations", null, cv)
  }

  def deleteLocation(l: MyPlace): Unit =
    getWritableDatabase.delete("my_locations", "id = ?", Array( l.id.toString ))

  // -- // -- // -- // -- // -- // -- // -- // -- // -- // -- // -- // -- // -- // -- // -- // -- //

  def getRoutes: Seq[MyRoute] =
    getReadableDatabase.rawQuery("SELECT * FROM my_routes", Array()).fetchAll { c =>
      MyRoute(c.getInt("id"), c.getString("name"), read[RouteGroupKey](c.getString("group_key")))
    }

  def storeRoute(r: MyRoute): Unit = {
    val cv = new ContentValues()

    cv.put("name", r.name)
    cv.put("group_key", write(r.key))

    if (r.id > 0) getWritableDatabase.update("my_routes", cv, "id = ?", Array( r.id.toString ))
    else getWritableDatabase.insert("my_routes", null, cv)
  }

  def deleteLocation(r: MyRoute): Unit =
    getWritableDatabase.delete("my_routes", "id = ?", Array( r.id.toString ))
}