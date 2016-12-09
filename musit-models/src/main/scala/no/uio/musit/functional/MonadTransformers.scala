/*
 * MUSIT is a museum database to archive natural and cultural history data.
 * Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package no.uio.musit.functional

import no.uio.musit.MusitResults.{MusitInternalError, MusitResult}

object MonadTransformers {

  /**
   * Monad transformer for MusitResult. Makes it easier to perform do map/flatMap
   * on MusitResults that are nested inside other Monadic types.
   *
   * @param value The value to perform monadic operations on
   * @param m     The Monad type this transformer works with
   * @tparam T The type of the wrapping Monad
   * @tparam A The type we want to access inside the transformed functions
   */
  case class MusitResultT[T[_], A](value: T[MusitResult[A]])(implicit m: Monad[T]) {

    def map[B](f: A => B): MusitResultT[T, B] =
      MusitResultT[T, B](m.map(value)(_.map(f)))

    def flatMap[B](f: A => MusitResultT[T, B]): MusitResultT[T, B] = {
      val res: T[MusitResult[B]] = m.flatMap(value) { a =>
        a.map(b => f(b).value).getOrElse {
          m.pure(MusitInternalError("Unable to map into MusitResult in the " +
            "MusitResultT transformer"))
        }
      }
      MusitResultT[T, B](res)
    }

  }

  /**
   * Monad transformer for Option. Makes coding with nested monadic types
   * (map/flatMap) easier.
   *
   * @param value The value to perform monadic operations on
   * @param m     The Monad type this transformer works with
   * @tparam T The type of the wrapping Monad
   * @tparam A The type we want to access inside the transformed functions
   */
  case class OptionT[T[_], A](value: T[Option[A]])(implicit m: Monad[T]) {

    def map[B](f: A => B): OptionT[T, B] =
      OptionT[T, B](m.map(value)(_.map(f)))

    def flatMap[B](f: A => OptionT[T, B]): OptionT[T, B] = {
      val res: T[Option[B]] = m.flatMap(value) { a =>
        a.map(b => f(b).value).getOrElse(m.pure(None))
      }
      OptionT[T, B](res)
    }

  }

}
