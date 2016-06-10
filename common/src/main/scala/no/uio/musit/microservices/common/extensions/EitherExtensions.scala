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
import play.api.Application

import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration._
import scala.reflect.ClassTag
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.functional.Functor
import play.api.mvc.Result

/**
 * Created by jstabel on 6/10/16.
 */

object EitherExtensions {

  implicit class EitherExtensionsImp[T](val either: Either[MusitError, T]) extends AnyVal {
    /* Not used. (yet?)
    def mapToMusitFuture[S](ifRight: T => Future[Either[MusitError, S]]): Future[Either[MusitError, S]] = {
      either match {
        case Left(l) => Future.successful(Left(l))
        case Right(r) => ifRight(r)
      }
    }
    */

    /**
     * If the either is Right, it is mapped into a future[Result] the obvious way.
     * If it is left, left, it is mapped into a successful future containing the error!
     * So before of further mapping of the result! (This explains the 'Final' in the name.)
     * (Ideally I'd want to make this into a value (of a type) which cannot further be mapped etc on,
     * to prevent potential bugs/misuse.)
     */
    def mapToFinalPlayResult[S](ifRight: T => Future[Result]): Future[Result] = {
      either match {
        case Left(l) => Future.successful(l.toPlayResult)
        case Right(r) => ifRight(r)
      }
    }
  }
}
