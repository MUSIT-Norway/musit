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

import no.uio.musit.models.{MuseumNo, ObjectId, SubNo}
import no.uio.musit.security.BearerToken
import no.uio.musit.security.fake.FakeAuthenticator
import no.uio.musit.test.MusitSpecWithServerPerSuite
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.libs.json._
import play.api.test.Helpers._

import scala.language.postfixOps

class ObjectAggregationIntegrationSpec extends MusitSpecWithServerPerSuite {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  val fakeToken = BearerToken(FakeAuthenticator.fakeAccessTokenPrefix + "musitTestUser")

  val archeologyCollection = "a4d768c8-2bf8-4a8f-8d7e-bc824b52b575"
  val numismaticsCollection = "8ea5fa45-b331-47ee-a583-33cd0ca92c82"

  "ObjectAggregation integration" must {
    "find objects for nodeId that exists" in {
      val nodeId = 3
      val mid = 99
      val response = wsUrl(s"/museum/$mid/node/$nodeId/objects")
        .withHeaders(fakeToken.asHeader)
        .withQueryString("collectionIds" -> archeologyCollection)
        .get().futureValue
      response.status mustBe OK

      val objects = response.json.as[JsArray].value
      objects must not be empty
      val obj = objects.head
      (obj \ "id").as[ObjectId] mustBe ObjectId(1)
      (obj \ "term").as[String] mustBe "Øks"
      (obj \ "museumNo").as[MuseumNo] mustBe MuseumNo("C666")
      (obj \ "subNo").as[SubNo] mustBe SubNo("34")
    }

    "respond with 404 for nodeId that does not exist" in {
      val nodeId = 99999
      val mid = 99
      val response = wsUrl(s"/museum/$mid/node/$nodeId/objects")
        .withHeaders(fakeToken.asHeader)
        .withQueryString("collectionIds" -> archeologyCollection)
        .get().futureValue
      response.status mustBe NOT_FOUND
      (response.json \ "message").as[String] must endWith(s"$nodeId")
    }

    "respond with 400 if the request URI is missing nodeId " in {
      val nodeId = None
      val mid = 99
      val response = wsUrl(s"/museum/$mid/node/$nodeId/objects")
        .withHeaders(fakeToken.asHeader)
        .withQueryString("collectionIds" -> archeologyCollection)
        .get().futureValue
      response.status mustBe BAD_REQUEST
    }

    "respond with 400 if the museumId is invalid" in {
      val nodeId = 99999
      val mid = 555
      val response = wsUrl(s"/museum/$mid/node/$nodeId/objects")
        .withHeaders(fakeToken.asHeader)
        .withQueryString("collectionIds" -> archeologyCollection)
        .get().futureValue
      response.status mustBe BAD_REQUEST
      (response.json \ "message").as[String] must include(s"$mid")
    }

    "respond with 400 if the museumId is missing from the request URI" in {
      val nodeId = 3
      val mid = None
      val response = wsUrl(s"/museum/$mid/node/$nodeId/objects")
        .withHeaders(fakeToken.asHeader)
        .withQueryString("collectionIds" -> archeologyCollection)
        .get().futureValue
      response.status mustBe BAD_REQUEST
    }

    "respond with 400 if the museumId isn't a valid number" in {
      val nodeId = 3
      val mid = "blæBlæBlæ"
      val response = wsUrl(s"/museum/$mid/node/$nodeId/objects")
        .withHeaders(fakeToken.asHeader)
        .withQueryString("collectionIds" -> archeologyCollection)
        .get().futureValue
      response.status mustBe BAD_REQUEST
    }

    "respond with 403 if the user doesn't have read access to the museum" in {
      val nodeId = 99999
      val mid = 6
      val response = wsUrl(s"/museum/$mid/node/$nodeId/objects")
        .withHeaders(fakeToken.asHeader)
        .withQueryString("collectionIds" -> archeologyCollection)
        .get().futureValue
      response.status mustBe FORBIDDEN
    }

    "respond with 403 if the user doesn't have access to the collection" in {
      val nodeId = 345
      val mid = 99
      val collection = UUID.randomUUID().toString
      val response = wsUrl(s"/museum/$mid/node/$nodeId/objects")
        .withHeaders(fakeToken.asHeader)
        .withQueryString("collectionIds" -> collection)
        .get().futureValue
      response.status mustBe FORBIDDEN
    }
  }
}
