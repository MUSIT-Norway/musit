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
import no.uio.musit.security.fake.FakeAuthenticator
import no.uio.musit.test.MusitSpecWithServerPerSuite
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.libs.json.{JsArray, JsNumber, Json}
import play.api.test.Helpers._

class DelphiBridgeControllerIntegrationSpec extends MusitSpecWithServerPerSuite {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  val fakeToken = BearerToken(FakeAuthenticator.fakeAccessTokenPrefix + "musitTestUser")

  val archeologyCollection = "a4d768c8-2bf8-4a8f-8d7e-bc824b52b575"
  val numismaticsCollection = "8ea5fa45-b331-47ee-a583-33cd0ca92c82"

  "The DelphiBridgeController" when {

    "finding the current location for an old object ID and schema" should {

      "return the objects current nodeId and path location" in {
        val expectedLocation = "Utviklingsmuseet, Utviklingsmuseet Org, " +
          "Forskningens hus, Naturværelset"
        val res = wsUrl(s"/delphi/objects/111")
          .withHeaders(fakeToken.asHeader)
          .withQueryString("schemaName" -> "USD_ARK_GJENSTAND_O")
          .get.futureValue

        res.status mustBe OK
        (res.json \ "nodeId").as[Int] mustBe 6
        (res.json \ "currentLocation").as[String] mustBe expectedLocation
      }

    }

    "listing all external nodes" should {
      "return a list of all nodes below the RootLoan node sorted by name" in {
        val expectedNames = List(
          "British museum",
          "Death Star gallery",
          "FooBar of History",
          "The Louvre",
          "Utenfor 2",
          "Utenfor museet"
        )

        val res = wsUrl(s"/delphi/museum/99/nodes/external")
          .withHeaders(fakeToken.asHeader)
          .get.futureValue

        res.status mustBe OK
        val rl = res.json.as[JsArray].value.toList
        rl.size mustBe 6
        rl.map(js => (js \ "name").as[String]) mustBe expectedNames

      }
    }

    "translating old objectIds in a schema" should {
      "return a list of the new ObjectIds" in {
        val expected = (11 to 20).toList

        val in = Json.obj(
          "schemaName" -> "USD_ARK_GJENSTAND_O",
          "oldObjectIds" -> JsArray((110 to 120).map(i => JsNumber(i)))
        )

        val res = wsUrl(s"/delphi/objects/tranlsate_old_ids")
          .withHeaders(fakeToken.asHeader)
          .put(in)
          .futureValue

        res.status mustBe OK
        val rl = res.json.as[Seq[Long]].toList
        rl.size mustBe 10
        rl mustBe expected
      }
    }

  }

}
