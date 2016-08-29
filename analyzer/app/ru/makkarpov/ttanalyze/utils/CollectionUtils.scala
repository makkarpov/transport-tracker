package ru.makkarpov.ttanalyze.utils

import scala.collection.generic.CanBuildFrom
import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds
import scala.util.Success

/**
  * Created by user on 8/3/16.
  */
object CollectionUtils {
  implicit class TraversableExtensions[T, C[X] <: Traversable[X]](val coll: C[T]) extends AnyVal {
    // Maps one by one element to avoid creating a lot of pending futures
    def flatMapFuture[B](f: T => Future[TraversableOnce[B]])
                        (implicit cbf: CanBuildFrom[_, B, C[B]], ec: ExecutionContext): Future[C[B]] = {
      var accum = Future.successful(cbf())

      for (x <- coll)
        accum = accum.flatMap(bld => {
          f(x).andThen {
            case Success(y) => bld ++= y
          }.map(_ => bld)
        })

      accum.map(_.result())
    }
  }

  implicit class IndexedSeqExtensions[T, C[X] <: Seq[X]](val coll: C[T]) extends AnyVal {
    def biMap[B](f: (T, T) => B)(implicit cbf: CanBuildFrom[IndexedSeq[Int], B, C[B]]): C[B] =
      (0 until (coll.size - 1)).map[B, C[B]](i => f(coll(i), coll(i + 1)))
  }

  implicit class DoubleExtensions(val d: Double) extends AnyVal {
    def zeroIfNan = if (d.isNaN) 0.0 else d
    def zeroIfNanOrInf = if (d.isNaN || d.isInfinity) 0.0 else d
  }
}
