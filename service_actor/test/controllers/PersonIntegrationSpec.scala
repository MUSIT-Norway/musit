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

import java.util.UUID

import no.uio.musit.models.ActorId
import no.uio.musit.security.BearerToken
import no.uio.musit.security.fake.FakeAuthenticator.fakeAccessTokenPrefix
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

  val andersAuthId = ActorId(UUID.fromString("12345678-adb2-4b49-bce3-320ddfe6c90f"))
  val andersAppId = ActorId(UUID.fromString("41ede78c-a6f6-4744-adad-02c25fb1c97c"))
  val kalleAppId = ActorId(UUID.fromString("5224f873-5fe1-44ec-9aaf-b9313db410c6"))

  "PersonIntegration " must {

    "fail getting person by id when there is no valid token" in {
      wsUrl(s"/v1/person/${andersAppId.asString}").get()
        .futureValue.status mustBe Status.UNAUTHORIZED
    }

    "get by id" in {
      val res = wsUrl(s"/v1/person/${andersAppId.asString}")
        .withHeaders(fakeToken.asHeader)
        .get().futureValue
      res.status mustBe Status.OK
      (res.json \ "id").as[Int] mustBe 1
    }

    "not find a user if the ID doesn't exist" in {
      val res = wsUrl(s"/v1/person/${UUID.randomUUID().toString}")
        .withHeaders(fakeToken.asHeader)
        .get().futureValue
      res.status mustBe Status.NOT_FOUND
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

    "get person details from Actor and UserInfo" in {
      val jsStr = s"""["${andersAuthId.asString}", "${kalleAppId.asString}"]"""
      val reqBody = Json.parse(jsStr)
      val res = wsUrl("/v1/person/details")
        .withHeaders(fakeToken.asHeader).post(reqBody).futureValue
      res.status mustBe Status.OK
      val js = res.json.as[JsArray].value
      js.length mustBe 2
      (js.last \ "fn").as[String] mustBe "Fred Flintstone"
      (js.head \ "fn").as[String] mustBe "Kanin, Kalle1"
    }

    "get person details with extra ids" in {
      val (id1, id2) = (ActorId.generate(), ActorId.generate())
      val jsStr = s"""["${id1.asString}", "${kalleAppId.asString}", "${id2.asString}"]"""
      val reqBody: JsValue = Json.parse(jsStr)
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
