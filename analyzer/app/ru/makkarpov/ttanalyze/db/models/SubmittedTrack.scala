package ru.makkarpov.ttanalyze.db.models

import java.time.Instant

import com.github.tminglei.slickpg.InetString
import ru.makkarpov.ttanalyze.Context
import ru.makkarpov.ttanalyze.Context._
import ru.makkarpov.ttanalyze.analyze.TrackPoint


import scala.concurrent.Future

/**
  * Created by user on 8/9/16.
  */
case class SubmittedTrack(id: Int, remoteAddr: InetString, version: Int, sha256: String, track: Array[Byte],
                          submittedAt: Instant)

object SubmittedTrack {
  import ru.makkarpov.ttanalyze.db.PgDriver.api
  import api._

  case class ViewInfo(id: Int, sha256: String, submittedAt: Instant, pointsN: Int)

  def findTracks(implicit ctx: Context): Future[Seq[ViewInfo]] =
    ctx.db(SubmittedTrack.query
      .map(x => (x.id, x.sha256, x.submittedAt, x.track.length))
      .sortBy(_._1.desc)
      .result.map(_.map {
        case (id, sha, time, len) => ViewInfo(id, sha, time, len / TrackPoint.PointSize)
      }))

  def fetchTrack(id: Int)(implicit ctx: Context): Future[Option[SubmittedTrack]] =
    ctx.db(SubmittedTrack.query.filter(_.id === id).result.map(_.headOption))

  class Table(t: Tag) extends api.Table[SubmittedTrack](t, "submitted_tracks") {
    def id          = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def remoteAddr  = column[InetString]("remote_addr")
    def version     = column[Int]("version")
    def sha256      = column[String]("sha256")
    def track       = column[Array[Byte]]("track_data")
    def submittedAt = column[Instant]("submitted_at")

    def * = (id, remoteAddr, version, sha256, track, submittedAt) <>
            ((SubmittedTrack.apply _).tupled, SubmittedTrack.unapply)
  }

  val query = TableQuery[Table]
}