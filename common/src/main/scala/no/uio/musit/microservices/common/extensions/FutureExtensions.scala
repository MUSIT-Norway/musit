/*
 *   MUSIT is a cooperation between the university museums of Norway.
 *   Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License,
 *   or any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License along
 *   with this program; if not, write to the Free Software Foundation, Inc.,
 *   51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package no.uio.musit.microservices.common.extensions

import no.uio.musit.microservices.common.domain.MusitError
import no.uio.musit.microservices.common.utils.Misc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

/**
 * Created by jstabel on 4/22/16.
 */

object FutureExtensions {

  type MusitResult[T] = Either[MusitError, T]
  type MusitFuture[T] = Future[Either[MusitError, T]]

  implicit class FutureOptionExtensions[T](val fut: Future[Option[T]]) extends AnyVal {
    def foldInnerOption[S](ifNone: => S, ifSome: T => S): Future[S] = fut.map(optValue => optValue.map(ifSome).getOrElse(ifNone))

    /**
     * Transforms a Future[Option[T]] to a MusitFuture[T] (Future[Either[MusitError, T]]) in the obvious way.
     */
    def toMusitFuture(errorIfNone: => MusitError): MusitFuture[T] = fut.foldInnerOption(Left(errorIfNone), Right(_))
  }

  implicit class FutureFutureExtensions[T](val fut: Future[Future[T]]) extends AnyVal {
    def flatten /*(implicit ec: ExecutionContext)*/ : Future[T] = fut.flatMap(identity)
  }

  /** Transforms a regular Future into a MusitFuture in the obvious way. */
  implicit class FutureExtensions[T](val fut: Future[T]) extends AnyVal {
    def toMusitFuture: MusitFuture[T] = fut.map(Right(_))
  }

  implicit class FutureEitherExtensions[T](val futEither: Future[Either[MusitError, T]]) extends AnyVal {
    /** The classical map on "MusitFuture". f maps the T in a MusitFuture[T] into an S and we return MusitFuture[S]. */
    def musitFutureMap[S](f: T => S): Future[Either[MusitError, S]] = {
      futEither.map { either => either.right.map(f) }
    }

    /**
     * The classical flatMap on "MusitFuture". f maps the T in a MusitFuture[T] into a MusitFuture[S]. This means we
     * sort of end up with a MusitFuture[MusitFuture[S]], which we flatten into a MusitFuture[S]
     */
    def musitFutureFlatMap[S](f: T => Future[Either[MusitError, S]]): Future[Either[MusitError, S]] = {
      futureEitherFlatten(futEither.musitFutureMap(f))
    }

    /**
     * Inside the future, flatMaps the Either part. f maps the T in a MusitFuture[T] into an Either[MusitError, S].
     * This means we sort of end up with MusitFuture[Either[MusitError, S]], which we flatten into MusitFuture[S].
     * (ie a regular Either flatMap on the "inner" Either.)
     */
    def musitFutureFlatMapInnerEither[S](f: T => Either[MusitError, S]): Future[Either[MusitError, S]] = {
      futEither.map { either => either.right.flatMap(f) }
    }
  }

  object MusitFuture {
    def successful[T](result: T): MusitFuture[T] = Future.successful(Right(result))
    def fromError[T](error: MusitError): MusitFuture[T] = Future.successful(Left(error))
  }

}