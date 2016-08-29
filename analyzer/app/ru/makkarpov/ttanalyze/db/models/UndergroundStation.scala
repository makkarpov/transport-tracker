package ru.makkarpov.ttanalyze.db.models

import ru.makkarpov.ttanalyze.Context
import Context._
import ru.makkarpov.ttanalyze.analyze.XAnalyzer.StationCollection

import scala.concurrent.Future

/**
  * Created by user on 7/14/16.
  */
case class UndergroundStation(id: Int, lineId: Int, name: String)

object UndergroundStation {
  import ru.makkarpov.ttanalyze.db.PgDriver.api
  import api._

  class Table(t: Tag) extends api.Table[UndergroundStation](t, "underground_stations") {
    def id        = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def lineId    = column[Int]("line_id")
    def name      = column[String]("name")

    def * = (id, lineId, name) <>
            ((UndergroundStation.apply _).tupled, UndergroundStation.unapply)
  }

  def findStations(start: StationCollection, finish: StationCollection)(implicit ctx: Context) = {
    import ctx.db.api._

    def getStations(ids: Set[Int]) = ctx.db(
      UndergroundStation.query
        .join(UndergroundExit.query).on((st, ex) => st.id === ex.stationId)
        .join(UndergroundLine.query).on { case ((st, _), ln) => st.lineId === ln.id }
        .filter{ case ((_, ex), _) => ex.id inSet ids }
        .map { case ((st, _), ln) => (st, ln) }.distinct.result)

    getStations(start.ids) flatMap { st =>
      // some day here will be line detection algorithm
      getStations(finish.ids) map { fin => (st.head, fin.head) }
    }
  }

  val query = TableQuery[Table]
}
