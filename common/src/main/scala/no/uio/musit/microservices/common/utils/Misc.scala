package no.uio.musit.microservices.common.utils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Created by ellenjo and jstabel on 6/2/16.
 */
object Misc {

  implicit class Pipe[T](x: T) {
    def |>[U](f: T => U): U = f(x)
  }

  ///We need sadomachocistic stuff like this because it's been decided we should use Either for errors in combination with futures. ;)
  def futureEitherFutureToFutureEither[L, R](futureEitherFuture: Future[Either[L, Future[R]]]): Future[Either[L, R]] = {
    futureEitherFuture.map(eitherFuture => {
      eitherFuture.fold(l => Future.successful(Left(l)), innerFuture => innerFuture.map(Right(_)))
    }).flatMap(identity)
  }

  def flattenEither[L, R](eitherEither: Either[L, Either[L, R]]): Either[L, R] = {
    eitherEither.fold(l => Left(l), identity)
  }

}
