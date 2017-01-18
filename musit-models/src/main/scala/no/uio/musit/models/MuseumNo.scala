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

package no.uio.musit.models

import play.api.libs.json._

trait MuseumNumber {
  val value: String

  /**
   * A sequence of non-digits, followed by a sequence of digits, followed by an
   * optional tail starting with a non-digit (and then whatever).
   *
   * NOTE: This regex will _not_ match the full number sequence for MuseumNumbers
   * that have the form "E10-15" (note the hyphen). Such MuseumNumbers indicate
   * a sequence of objects that are registered in the same entry. One important
   * point regarding these entries is that the objects are do not necessarily
   * share any properties. They might not even be related in any other way than
   * being registered at the same point in history.
   */
  val regExp = """\A\D*(\d+)(?:\D.*)?\z""".r

  /**
   * The number part of a museumNo
   */
  def asNumber: Option[Long] = {
    val optM = regExp.findFirstMatchIn(value)

    // This regular expression is designed to only return one group. Per def of
    // this re, this should always be possible (within reasonable length of
    // museumNo!) and never throw any exceptions.
    optM.map { m =>
      assert(m.groupCount == 1) // TODO: Don't throw Exception here!
      m.group(1).toLong
    }
  }
}

case class MuseumNo(value: String) extends MuseumNumber

object MuseumNo {
  implicit val reads: Reads[MuseumNo] = __.read[String].map(MuseumNo.apply)
  implicit val writes: Writes[MuseumNo] = Writes(mn => JsString(mn.value))
}

case class SubNo(value: String) extends MuseumNumber

object SubNo {
  implicit val reads: Reads[SubNo] = __.read[String].map(SubNo.apply)
  implicit val writes: Writes[SubNo] = Writes(mn => JsString(mn.value))
}