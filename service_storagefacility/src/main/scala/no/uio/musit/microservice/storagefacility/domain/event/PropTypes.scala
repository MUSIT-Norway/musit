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

package no.uio.musit.microservice.storagefacility.domain.event

import scala.util.Try

object PropTypes {

  /**
   * Contains an ADT definition that specifies property validation results.
   * It is very specific to validating the entries in a Map[String, Any]
   * against SubEvent in the EventTypeRegistry.
   */
  object PropValidation {

    sealed trait ValidatedProps

    case class ValidProps(success: EventProps) extends ValidatedProps

    case class InvalidProps(
      missing: Set[String] = Set.empty,
      invalid: Set[String] = Set.empty,
      badTypes: Set[String] = Set.empty
    ) extends ValidatedProps

  }

  /**
   * A PropType provides a way to validate arguments of type `Any` against a
   * specific type.
   *
   * @tparam T the type that will be used when validating arguments.
   */
  sealed trait PropType[T] {

    /**
     * This function has 2 responsibilities to consider when implementing.
     *
     * 1. It needs to validate that the given `arg` is of the expected type `T`.
     * 2. If the `arg` is valid, the arg should be returned as an instance of
     *    the correct type.
     */
    def transform(arg: Any): Option[T]
  }

  case object FloatPropType extends PropType[Float] {
    override def transform(arg: Any): Option[Float] = {
      Try(arg.asInstanceOf[BigDecimal]).toOption.flatMap { x =>
        if (x.isDecimalFloat) Some(x.toFloat)
        else None
      }
    }
  }

  case object DoublePropType extends PropType[Double] {
    override def transform(arg: Any): Option[Double] = {
      Try(arg.asInstanceOf[BigDecimal]).toOption.flatMap { x =>
        if (x.isDecimalDouble) Some(x.toDouble)
        else None
      }
    }

  }

  case object LongPropType extends PropType[Long] {
    override def transform(arg: Any): Option[Long] = {
      Try(arg.asInstanceOf[BigDecimal]).toOption.flatMap { x =>
        if (x.isValidLong) Some(x.toLong)
        else None
      }
    }

  }

  case object IntPropType extends PropType[Int] {
    override def transform(arg: Any): Option[Int] = {
      Try(arg.asInstanceOf[BigDecimal]).toOption.flatMap { x =>
        if (x.isValidInt) Some(x.toInt)
        else None
      }
    }

  }

  case object BooleanPropType extends PropType[Boolean] {
    override def transform(arg: Any): Option[Boolean] = {
      Try(arg.asInstanceOf[Boolean]).toOption
    }

  }

  case object StringPropType extends PropType[String] {
    override def transform(arg: Any): Option[String] = {
      Try(arg.asInstanceOf[String]).toOption
    }

  }

}
