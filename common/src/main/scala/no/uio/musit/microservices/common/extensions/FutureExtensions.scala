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

import no.uio.musit.microservices.common.utils.Misc._
import play.api.Application

import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration._
import scala.reflect.ClassTag
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.functional.Functor

/**
 * Created by jstabel on 4/22/16.
 */

object FutureExtensions {

  /*
  implicit class FutureExtensionsImp[T](val fut: Future[T]) extends AnyVal {
    def awaitInSeconds(seconds: Int) = Await.result(fut, seconds.seconds)
  }
*/

  implicit class FutureOptionExtensions[T](val fut: Future[Option[T]]) extends AnyVal {
    def foldInnerOption[S](ifNone: => S, ifSome: T => S): Future[S] = fut.map(optValue => optValue.map(ifSome).getOrElse(ifNone))
  }

  implicit class FutureFutureExtensions[T](val fut: Future[Future[T]]) extends AnyVal {
    def flatten /*(implicit ec: ExecutionContext)*/ : Future[T] = fut.flatMap(identity)
  }

  implicit class FutureEitherExtensions[L, R](val futEither: Future[Either[L, R]]) extends AnyVal {
    def futureEitherMap[S](f: R => S): Future[Either[L, S]] = {
      futEither.map { either => either.right.map(f) }
    }

    def futureEitherFlatMap[S](f: R => Future[Either[L, S]]): Future[Either[L, S]] = {
      futureEitherFlatten(futEither.futureEitherMap(f))
    }

  }
}