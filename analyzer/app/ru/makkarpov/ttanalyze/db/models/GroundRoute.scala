package ru.makkarpov.ttanalyze.db.models

import com.vividsolutions.jts.geom.LineString
import ru.makkarpov.ttanalyze.db.GroundTransport
import ru.makkarpov.ttanalyze.db.GroundTransport.GroundTransport
import slick.jdbc.GetResult

/**
  * Created by user on 7/18/16.
  */
case class GroundRoute(id: Int, index: String, kind: GroundTransport, forwardPath: LineString, backwardPath: LineString)

object GroundRoute {
  import ru.makkarpov.ttanalyze.db.PgDriver.api
  import api._

  class Table(t: Tag) extends api.Table[GroundRoute](t, "ground_routes") {
    def id           = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def index        = column[String]("index")
    def kind         = column[GroundTransport]("type")
    def forwardPath  = column[LineString]("forward_path")
    def backwardPath = column[LineString]("backward_path")

    def * = (id, index, kind, forwardPath, backwardPath) <>
            ((GroundRoute.apply _).tupled, GroundRoute.unapply)
  }

  val query = TableQuery[Table]
}
