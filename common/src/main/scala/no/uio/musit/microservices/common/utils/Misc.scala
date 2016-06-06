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


  def flattenFutureEitherFuture[L, R](futureEitherFuture: Future[Either[L, Future[R]]]): Future[Either[L, R]] = {
    futureEitherFuture.map(eitherFuture => {
      eitherFuture.fold(l => Future.successful(Left(l)), innerFuture => innerFuture.map(Right(_)))
    }).flatMap(identity)
  }

  def flattenEither[L, R](eitherEither: Either[L, Either[L, R]]): Either[L, R] = {
    eitherEither.fold(l => Left(l), identity)
  }

  def flattenFutureEitherFutureEither[L, R](futureEitherFutureEither: Future[Either[L, Future[Either[L, R]]]]): Future[Either[L, R]] = {
    flattenFutureEitherFuture(futureEitherFutureEither).map(flattenEither)
  }

}
