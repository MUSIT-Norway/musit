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

import scala.annotation.implicitNotFound
import scala.concurrent.{ExecutionContext, Future}

object Implicits {

  /**
   * Implicit converter to wrap a {{{no.uio.musit.functional.Monad}}} around a
   * {{{scala.concurrent.Future}}}. This allows for composition of Monads using
   * Monad transformers.
   *
   * @param ec The ExecutionContext for mapping on the Future type
   * @return a Monad of type Future
   */
  @implicitNotFound(
    "A Future[Monad] needs an implicit ExecutionContext in " +
      "scope. If you are using the Play! Framework, consider using the " +
      "frameworks default context:\n" +
      "import play.api.libs.concurrent.Execution.Implicits.defaultContext\n\n" +
      "Otherwise, use the ExecutionContext that best suits your needs."
  )
  implicit def futureMonad(implicit ec: ExecutionContext) = new Monad[Future] {
    override def map[A, B](value: Future[A])(f: (A) => B) = value.map(f)

    override def flatMap[A, B](value: Future[A])(f: (A) => Future[B]) =
      value.flatMap(f)

    override def pure[A](x: A): Future[A] = Future(x)
  }
}
