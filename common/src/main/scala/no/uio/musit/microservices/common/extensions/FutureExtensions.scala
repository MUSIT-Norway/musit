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

import play.api.Application

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.reflect.ClassTag
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.functional.Functor


/**
  * Created by jstabel on 4/22/16.
  */

object FutureExtensions {

  implicit class FutureExtensionsImp[T](val fut: Future[T]) extends AnyVal {
    def awaitInSeconds(seconds: Int) = Await.result(fut, seconds.seconds)

  }

  implicit class FutureOptionExtensions[T](val fut: Future[Option[T]]) extends AnyVal {
    def foldOption[S](ifSome: T => S, ifNone: => S): Future[S] = fut.map(optValue => optValue.map(ifSome).getOrElse(ifNone))
  }
/*
  implicit class FunctorOptionExtensions[F[_] : Functor, T](ft: F[T]) {
    def unpackOption[S](someMapper: T => S, noneHandler: => S)(implicit f: Functor[Option[_]]): F[S] = f.fmap(optValue => optValue.map(someMapper).getOrElse(noneHandler))

    //    def unpackOptionToJsonOrNotFound[S](notFoundText: String) = Ok()
  }
*/
  /*
    implicit class FutureOptionExtensions[T, X<:Option[T]](val fut: Future[X]) extends AnyVal {
      def unpackOption[S](someMapper: T=>S, noneMapper: =>S): Future[S] = fut.map(optValue=>optValue.map(someMapper).getOrElse(noneMapper))
    }*/
}