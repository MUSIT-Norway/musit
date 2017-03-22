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

import no.uio.musit.formatters.StrictFormatters.maxCharsFormat
import org.scalatest.{Inside, MustMatchers, WordSpec}
import play.api.libs.json.{JsString, JsSuccess, Json}

class StrictFormattersSpec extends WordSpec with MustMatchers with Inside {

  val maxChars = 20

  val shortString = "This is a String"
  val longString  = "This is a much longer String that should fail."

  "Converting a String to JSON" should {
    "successfully generate a JsString when the String is shorter than max" in {
      Json.toJson(shortString)(maxCharsFormat(maxChars)) mustBe JsString(shortString)
    }

    "successfully generate a JsString when the String is longer than max" in {
      Json.toJson(longString)(maxCharsFormat(maxChars)) mustBe JsString(longString)
    }
  }

  "Converting a JSON String to String" should {
    "result in a valid String" in {
      val js = JsString(shortString)

      val res = js.validate[String](maxCharsFormat(20))

      inside(res) {
        case JsSuccess(str, _) => str mustBe shortString
      }
    }

    "fail when the value is too long" in {
      val js = JsString(longString)

      val res = js.validate[String](maxCharsFormat(20))
      res.isError mustBe true
    }
  }

}
