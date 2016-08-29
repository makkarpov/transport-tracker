package ru.makkarpov.ttanalyze.db

import javax.inject.Inject

import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile

import scala.concurrent.Future

/**
  * Created by user on 7/14/16.
  */
class Database @Inject()(prov: DatabaseConfigProvider) {
  val api = PgDriver.api
  val connection = prov.get[JdbcProfile].db

  import api._

  def apply[R](act: DBIOAction[R, NoStream, Nothing]): Future[R] = connection.run(act)
}
