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

import no.uio.musit.security.BearerToken
import no.uio.musit.security.FakeAuthenticator.fakeAccessTokenPrefix
import no.uio.musit.test.MusitSpecWithServerPerSuite
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.http.Status
import play.api.libs.json.{JsArray, JsValue, Json}

class PersonIntegrationSpec extends MusitSpecWithServerPerSuite {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  val fakeUserId = "musitTestUser"
  val fakeToken = BearerToken(fakeAccessTokenPrefix + fakeUserId)

  "LegacyPersonIntegration " must {
    "get by id" in {
      val res = wsUrl("/v1/person/1")
        .withHeaders(fakeToken.asHeader)
        .get().futureValue
      res.status mustBe Status.OK
      (res.json \ "id").as[Int] mustBe 1
    }

    "negative get by id" in {
      val res = wsUrl("/v1/person/9999")
        .withHeaders(fakeToken.asHeader)
        .get().futureValue
      res.status mustBe Status.NOT_FOUND
      (res.json \ "message").as[String] mustBe "Did not find object with id: 9999"
    }

    "search on person" in {
      val res = wsUrl("/v1/person?museumId=0&search=[And]")
        .withHeaders(fakeToken.asHeader)
        .get().futureValue
      res.status mustBe Status.OK
      val js = res.json.as[JsArray].value
      js.length mustBe 1
      (js.head \ "fn").as[String] mustBe "And, Arne1"
    }

    "search on person case insensitive" in {
      val res = wsUrl("/v1/person?museumId=0&search=[and]")
        .withHeaders(fakeToken.asHeader)
        .get().futureValue
      res.status mustBe Status.OK
      val js = res.json.as[JsArray].value
      js.length mustBe 1
      (js.head \ "fn").as[String] mustBe "And, Arne1"
    }

    "return bad request when no search criteria is specified" in {
      val res = wsUrl("/v1/person?museumId=0")
        .withHeaders(fakeToken.asHeader)
        .get().futureValue
      res.status mustBe Status.BAD_REQUEST
    }

    "get person details" in {
      val reqBody: JsValue = Json.parse("[1,2]")
      val res = wsUrl("/v1/person/details")
        .withHeaders(fakeToken.asHeader)
        .post(reqBody).futureValue
      res.status mustBe Status.OK
      val js = res.json.as[JsArray].value
      js.length mustBe 2
      (js.head \ "fn").as[String] mustBe "And, Arne1"
      (js.last \ "fn").as[String] mustBe "Kanin, Kalle1"
    }

    "get person details with extra ids" in {
      val reqBody: JsValue = Json.parse("[1234567,2,9999]")
      val res = wsUrl("/v1/person/details")
        .withHeaders(fakeToken.asHeader)
        .post(reqBody).futureValue
      res.status mustBe Status.OK
      val js = res.json.as[JsArray].value
      js.length mustBe 1
      (js.head \ "fn").as[String] mustBe "Kanin, Kalle1"
    }

    "not get person details with illegal json" in {
      val reqBody = Json.parse("[12,99999999999999999999999999999999999]")
      val res = wsUrl("/v1/person/details")
        .withHeaders(fakeToken.asHeader)
        .post(reqBody).futureValue
      res.status mustBe Status.BAD_REQUEST
    }
  }
}
