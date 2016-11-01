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

package models

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class Interval[T](base: T, tolerance: Option[Int])

object Interval {
  /*
     FIXME: https://github.com/playframework/playframework/issues/5261

     The above issue makes it impossible to use the JSON macros for generating
     JSON reads/writes/formats for `Interval`. Because it contains a recursive
     type `Option[T]`.

     Therefore the JSON picklers are hand-written.

  */
  //  implicit val intFmt: Format[Interval[Int]] = Json.format[Interval[Int]]
  //  implicit val longFmt: Format[Interval[Long]] = Json.format[Interval[Long]]
  //  implicit val dblFmt: Format[Interval[Double]] = Json.format[Interval[Double]]

  implicit val intFormat: Format[Interval[Int]] = (
    (__ \ "base").format[Int] and
    (__ \ "tolerance").formatNullable[Int]
  )((b, t) => Interval(b, t), i => (i.base, i.tolerance))

  implicit val longFormat: Format[Interval[Long]] = (
    (__ \ "base").format[Long] and
    (__ \ "tolerance").formatNullable[Int]
  )((b, t) => Interval(b, t), i => (i.base, i.tolerance))

  implicit val doubleFormat: Format[Interval[Double]] = (
    (__ \ "base").format[Double] and
    (__ \ "tolerance").formatNullable[Int]
  )((b, t) => Interval(b, t), i => (i.base, i.tolerance))
}
