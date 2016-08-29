package ru.makkarpov.ttanalyze

import javax.inject.Inject

import akka.stream.Materializer
import ru.makkarpov.ttanalyze.db.Database

import scala.concurrent.ExecutionContext

/**
  * Created by user on 7/14/16.
  */
object Context {
  implicit def context2executionContext(implicit ctx: Context): ExecutionContext =
    ctx.executor

  implicit def context2materializer(implicit ctx: Context): Materializer =
    ctx.materializer
}

case class Context @Inject() (executor: ExecutionContext, db: Database, materializer: Materializer) {

}
