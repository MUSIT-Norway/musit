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

package no.uio.musit.formatters

import no.uio.musit.formatters.DateTimeFormatters.dateTimeFormatter
import org.joda.time.DateTime
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json._

class DateTimeFormattersSpec extends WordSpec with MustMatchers with OptionValues {

  val dateString = "2017-01-13T10:46:32+00:00"
  val dateJson   = JsString("2017-01-13T10:46:32+00:00")
  val dateTime   = new DateTime(2017, 1, 13, 10, 46, 32, 0)

  val badDateJson = JsString("13-01-2017T10:46:32+00:00")

  "Formatting dates" should {

    "successfully generate a JSON date" in {
      Json.toJson(dateTime)(dateTimeFormatter).as[String] mustBe dateString
    }

    "parse ISO-8601 dates correctly" in {
      val jsd = Json.fromJson(dateJson)(dateTimeFormatter).asOpt
      jsd must not be None
      jsd.value mustBe dateTime
    }

    "fail when trying to parse an incorrect date JS String" in {
      Json.fromJson(badDateJson)(dateTimeFormatter).isError mustBe true
    }

  }
}
