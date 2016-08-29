package ru.makkarpov.ttanalyze.db.models

import com.vividsolutions.jts.geom.{Point, Polygon}
import slick.jdbc.GetResult

/**
  * Created by user on 7/18/16.
  */
case class GroundStop(id: Int, name: String, coordinates: Polygon)

object GroundStop {
  import ru.makkarpov.ttanalyze.db.PgDriver.api
  import api._

  class Table(t: Tag) extends api.Table[GroundStop](t, "ground_stops") {
    def id          = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name        = column[String]("name")
    def coordinates = column[Polygon]("coordinates")

    def * = (id, name, coordinates) <>
            ((GroundStop.apply _).tupled, GroundStop.unapply)
  }

  val query = TableQuery[Table]
}
