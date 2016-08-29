package ru.makkarpov.ttanalyze.db.models

import ru.makkarpov.ttanalyze.Context
import ru.makkarpov.ttanalyze.analyze.XAnalyzer.StationCollection

import scala.concurrent.Future

/**
  * Created by user on 7/18/16.
  */
case class GroundRouteStop(id: Int, routeId: Int, stopId: Int, forward: Boolean)

object GroundRouteStop {
  import ru.makkarpov.ttanalyze.db.PgDriver.api
  import api._

  class Table(t: Tag) extends api.Table[GroundRouteStop](t, "ground_routes_stops") {
    def id      = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def routeId = column[Int]("route_id")
    def stopId  = column[Int]("stop_id")
    def forward = column[Boolean]("forward")

    def * = (id, routeId, stopId, forward) <>
            ((GroundRouteStop.apply _).tupled, GroundRouteStop.unapply)
  }

  def findRoute(start: StationCollection, stop: StationCollection)(implicit ctx: Context) = {
    import ctx.db.api._

    val q = GroundRoute.query
        .join(GroundRouteStop.query).on((rt, st) => rt.id === st.routeId)
        .join(GroundRouteStop.query).on { case ((rt, _), st) => rt.id === st.routeId }
        .join(GroundStop.query).on { case (((_, rst), _), st) => rst.stopId === st.id }
        .join(GroundStop.query).on { case ((((_, _), rst), _), st) => rst.stopId === st.id }
        .map {
          case ((((route, rstop1), rstop2), stop1), stop2) => (route, rstop1, rstop2, stop1, stop2)
        }.filter {
          case (route, rstop1, rstop2, stop1, stop2) =>
            (stop1.id inSet start.ids) && (stop2.id inSet stop.ids) && (rstop1.forward === rstop2.forward)
        }.map {
          case (route, _, _, stop1, stop2) => (route, stop1, stop2)
        }

//    ctx.db(sql"""
//     SELECT "ground_routes".*, "stop_s".*, "stop_f".*
//     FROM "ground_routes", "ground_routes_stops" AS "rstop_s", "ground_routes_stops" AS "rstop_f",
//          "ground_stops" AS "stop_s", "ground_stops" AS "stop_f"
//     WHERE
//       ("ground_routes"."id" = "rstop_s"."route_id") AND ("rstop_s"."id" = "rstop_f"."id") AND
//       ("rstop_s"."forward" = "rstop_f"."forward") AND ("stop_s"."id" = "rstop_s"."stop_id") AND
//       ("stop_f"."id" = "rstop_f"."stop_id") AND
//       ("stop_s"."id" IN (#${start.mkString(", ")})) AND ("stop_f"."id" IN (#${stop.mkString(", ")}))
//    """.as[(GroundRoute, GroundStop, GroundStop)])

    ctx.db(q.result)
  }

  val query = TableQuery[Table]
}
