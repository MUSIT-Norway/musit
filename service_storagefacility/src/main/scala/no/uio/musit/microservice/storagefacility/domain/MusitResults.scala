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

package no.uio.musit.microservice.storagefacility.domain

object MusitResults {

  sealed abstract class MusitResult[+A] {

    def isSuccess: Boolean

    def isFailure: Boolean = !isSuccess

    def get: A

    def map[B](f: A => B): MusitResult[B]

    def flatMap[B](f: A => MusitResult[B]): MusitResult[B]

    def flatten[B](implicit ev: A <:< MusitResult[B]): MusitResult[B]

    final def getOrElse[B >: A](default: => B): B = {
      if (isFailure) default else this.get
    }

  }

  /**
   * Use this to as return type when operations is successful
   */
  case class MusitSuccess[+A](arg: A) extends MusitResult[A] {
    override val isSuccess: Boolean = true

    override def get: A = arg

    override def map[B](f: A => B): MusitResult[B] = MusitSuccess[B](f(arg))

    override def flatMap[B](f: A => MusitResult[B]): MusitResult[B] = f(arg)

    override def flatten[B](
      implicit
      ev: A <:< MusitResult[B]
    ): MusitResult[B] = arg
  }

  sealed trait MusitError[+A] extends MusitResult[A] {
    val message: String

    override def get: A =
      throw new NoSuchElementException("MusitResult.get on MusitError") // scalastyle:ignore

    override def map[B](f: A => B): MusitResult[B] =
      this.asInstanceOf[MusitError[B]]

    override def flatMap[B](f: A => MusitResult[B]): MusitResult[B] =
      this.asInstanceOf[MusitError[B]]

    override def flatten[B](implicit ev: A <:< MusitResult[B]): MusitResult[B] =
      this.asInstanceOf[MusitError[B]]
  }

  // ========================================================================
  // Specific error types inheriting from MusitError
  // ========================================================================

  /**
   * Use this when validation of fields and conditions are not met.
   */
  case class MusitValidationError[+A](
      message: String,
      expected: A,
      actual: A
  ) extends MusitError[A] {
    override val isSuccess: Boolean = false
  }

  /**
   * Use this when dealing with unexpected internal errors.
   */
  case class MusitInternalError[+A](message: String) extends MusitError[A] {
    override val isSuccess: Boolean = false
  }

  /**
   * Use this error type when handling Exceptions from the DB driver.
   */
  case class MusitDbError[+A](
      message: String,
      ex: Option[Throwable] = None
  ) extends MusitError[A] {
    override val isSuccess: Boolean = false
  }

}
