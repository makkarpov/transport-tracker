package ru.makkarpov.ttanalyze.db.models

import com.vividsolutions.jts.geom.{Point, Polygon}

/**
  * Created by user on 7/14/16.
  */
case class UndergroundExit(id: Int, stationId: Int, coordinates: Polygon, description: Option[String])

object UndergroundExit {
  import ru.makkarpov.ttanalyze.db.PgDriver.api
  import api._

  class Table(t: Tag) extends api.Table[UndergroundExit](t, "underground_exits") {
    def id          = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def stationId   = column[Int]("station_id")
    def coordinates = column[Polygon]("coordinates")
    def description = column[String]("description").?

    def * = (id, stationId, coordinates, description) <>
            ((UndergroundExit.apply _).tupled, UndergroundExit.unapply)
  }

  val query = TableQuery[Table]
}
