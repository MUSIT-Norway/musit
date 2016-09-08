package no.uio.musit.microservices.common.utils

import no.uio.musit.microservices.common.domain.MusitError
import no.uio.musit.microservices.common.extensions.FutureExtensions.{ MusitFuture, MusitResult }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Created by ellenjo and jstabel on 6/2/16.
 */
object Misc {

  implicit class Pipe[T](x: T) {
    def |>[U](f: T => U): U = f(x)
  }

  def flattenEither[L, R](eitherEither: Either[L, Either[L, R]]): Either[L, R] = {
    eitherEither.fold(l => Left(l), identity)
  }

  def futureEitherFlatten[L, R](futureEitherFutureEither: Future[Either[L, Future[Either[L, R]]]]): Future[Either[L, R]] = {
    def flattenFutureEitherFuture[A, B](futureEitherFuture: Future[Either[A, Future[B]]]): Future[Either[A, B]] = {
      futureEitherFuture.map(eitherFuture => {
        eitherFuture.fold(l => Future.successful(Left(l)), innerFuture => innerFuture.map(Right(_)))
      }).flatMap(identity)
    }
    flattenFutureEitherFuture(futureEitherFutureEither).map(flattenEither)
  }

  /*"Removes" the failures and "inverts" the futures */
  def filterSuccesses[T](values: Seq[MusitFuture[T]]): Future[Seq[T]] = {
    val tempValues: Seq[Future[Option[T]]] =
      values.map { futEither =>
        futEither.map {
          case Left(_) => None
          case Right(t) => Some(t)
        }
      }
    val tempValues2 = Future.sequence(tempValues)
    tempValues2.map(_.flatten)
  }

  /**
   * Some notes about error handling, especially related to Futures.
   *
   * In this note, I will use the terms MusitResult, MusitFuture, as if defined equivalently to the below.
   * (Note that we don't use these the concepts explicitly in the code (at least not yet), it is only for understanding the discussion.)
   *
   * type MusitResult[T] = Either[MusitError, T]
   * type MusitFuture[T] = Future[MusitResult[T]]  (= Future[Either[MusitError, T]])
   * type MusitBooleanResult[T] = MusitResult[Boolean/Unit]      (=Either[MusitError, Boolean/Unit])
   *
   *
   * (Better to use "MusitFuture" than "MusitFutureResult" or "FutureMusitResult"?)
   * So all of these types are related to the MusitError type, they represents code/processes which may fail with a MusitError.
   *
   * In general, we often need to "bubble up" error information from the inner layers to the outer layers. This can sometimes be a challenge.
   *
   * Option[T] can tell you if something succeeded or not, but when it fails, it can't explicitly tell you why, you only get None (aka "No soup for you!" ;) ), with no explanation.
   * So you need to do extra work in order to report what went wrong.
   *
   * MusitResult improves upon Option in this situation, because it can explicitly tell you *why* something went wrong, and this info can bubble upwards in the call chain,
   * using mapping and flatmapping, up through the call chain.
   *
   * MusitFuture is just a Future MusitResult.
   *
   * In a library like Slick, which generally returns Future[T] and not something similar to Future[Either...], errors are propagated using exceptions.
   * If one doesn't want to use/propagate exceptions for propagating failure information when using Futures, one needs to use something like MusitFuture.
   * It can be a challenge to compose functions returning MusitFutures (especially when seen "naked" as Future[Either[MusitError, T]]), unless one utilizes the fact that this is a monad.
   * Fortunately, when used as a monad, with the traditional map and flatMap functions, it becomes easy to compose functions returning Future[Either[MusitError, T]] (aka MusitFuture[T]).
   *
   * (If we actually create a type in Scala like MusitFuture, we can even use for-comprehensions on them)
   *
   * MusitBool/MusitBooleanResult represents whether some code succeeded or not. If "false", it also holds the information about why it is false/failed. (in its Either-Left branch)
   * If Either.Right, it means True (irrespective of what is in the right branch, ideally I'd like to use Either[MusitError, Unit] (ie Unit instead of Boolean), but I
   * tried that and the compiler ended up allowing a bit too much.
   *
   * (Not sure about whether to use the name MusitBooleanResult, MusitTrue or MusitSuccess or something else.)
   * *
   */

  type MusitBool = MusitResult[Boolean] //Use MusitResult[Unit] instead?

  // Not sure why I needed this object and couldn't directly use EitherExtensions. Should ideally use EitherExtensions instead.
  object MusitBoolExtensions {

    implicit class MusitBoolExtensionsImp(val either: MusitBool) extends AnyVal {
      def toMusitFuture = MusitFuture.fromMusitResult(either)
    }
  }

  /**
   * Maps a boolean condition to a "MusitBool" (see above discussion).
   */
  def boolToMusitBool(condition: Boolean, errorIfFalse: => MusitError): Either[MusitError, Boolean] = {
    if (condition) {
      Right(true) //Ok, we're in the true/right branch
    } else {
      Left(errorIfFalse) //Here we signal the error
    }
  }

  def musitTrue = Right(true)
}
