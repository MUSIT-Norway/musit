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
import no.uio.musit.microservices.common.extensions.FutureExtensions.{MusitFuture, MusitResult}
import play.api.http.Status

import scala.util.control.NonFatal

// TODO: Move to another file (create MusitResult.scala), this is really MusitResult methods, not general Either methods. 
object EitherExtensions {

  implicit class EitherExtensionsImp[T](val either: Either[MusitError, T]) extends AnyVal {

    def map[S](f: T => S) = either.right.map(f)

    def flatMap[S](f: T => MusitResult[S]) = either.right.flatMap(f)

    ///a quick and dirty way to get the value or throw an exception, only meant to be used for testing or quick and dirty stuff!
    def getOrFail = {
      either match {
        case Left(l) => throw new Exception(l.message)
        case Right(v) => v
      }
    }

    def toMusitFuture = MusitFuture.fromMusitResult(either)

  }

  object MusitResult {
    def apply[T](t: T): MusitResult[T] = Right(t)

    def create[T](r: => T, status: Int = Status.BAD_REQUEST): MusitResult[T] = {
      try Right(r) catch {
        case NonFatal(e) => Left(MusitError(status, e.getMessage))
      }
    }
  }

  // TODO: Really concatenate errors, now we take the first error!
  def concatenateMusitResults[T](musitResults: Seq[MusitResult[T]]): MusitResult[Seq[T]] = {
    if (musitResults.isEmpty)
      Right(Seq.empty)
    else {
      val head = musitResults.head
      val tail = musitResults.tail

      val prevResult = concatenateMusitResults(tail)
      val result = prevResult match {
        case Left(errorT) => Left(errorT) //Todo, concatenate
        case Right(seqT) =>
          head.map { headT => (headT +: seqT) } //Could do: Check whether this is the fastest way, if not, change the order and reverse the list at the very end

      }
      result
    }
  }
}
