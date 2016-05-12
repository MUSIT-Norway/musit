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
package controllers

import no.uio.musit.microservices.common.domain.{ MusitError, MusitFilter }
import play.api.test.FakeRequest
import play.api.libs.json.Json
import no.uio.musit.microservices.time.resource.TimeResource
import no.uio.musit.microservices.time.domain.MusitTime
import org.scalatestplus.play._
import play.api.test.Helpers._

class TimeControllerSpec extends PlaySpec {
  "TimeController" should {
    "give date and time when provided a datetime filter" in {
      val futureResult = new TimeResource().now(Some(MusitFilter(List("date", "time"))), None).apply(FakeRequest())
      status(futureResult) mustBe (OK)
      val json = contentAsString(futureResult)
      val now = Json.parse(json).validate[MusitTime].get
      now.time must not be None
      now.date must not be None
    }

    "give date but not time when provided a date filter" in {
      val futureResult = new TimeResource().now(Some(MusitFilter(List("date"))), None)(FakeRequest())
      status(futureResult) mustBe (OK)
      val json = contentAsString(futureResult)
      val now = Json.parse(json).validate[MusitTime].get
      now.time mustEqual None
      now.date must not be None
    }

    "give time but not date when provided a time filter" in {
      val futureResult = new TimeResource().now(Some(MusitFilter(List("time"))), None)(FakeRequest())
      status(futureResult) mustBe (OK)
      val json = contentAsString(futureResult)
      val now = Json.parse(json).validate[MusitTime].get
      now.date mustEqual None
      now.time must not be None
    }

    "give date and time when provided no filter" in {
      val futureResult = new TimeResource().now(None, None)(FakeRequest())
      status(futureResult) mustBe (OK)
      val json = contentAsString(futureResult)
      val now = Json.parse(json).validate[MusitTime].get
      now.date must not be None
      now.time must not be None
    }

    "give error message when provided invalid filter" in {
      val futureResult = new TimeResource().now(Some(MusitFilter(List("uglepose"))), None)(FakeRequest())
      status(futureResult) mustBe (BAD_REQUEST)
      val json = contentAsString(futureResult)
      val now = Json.parse(json).validate[MusitError].get
      now.message mustEqual "Only supports empty filter or filter on time, date or time and date"
    }
  }
}