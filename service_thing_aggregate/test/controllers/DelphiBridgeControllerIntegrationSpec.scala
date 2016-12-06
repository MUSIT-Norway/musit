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
import play.api.test.Helpers._

class DelphiBridgeControllerIntegrationSpec extends MusitSpecWithServerPerSuite {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  val fakeToken = BearerToken(FakeAuthenticator.fakeAccessTokenPrefix + "musitTestUser")

  val archeologyCollection = "a4d768c8-2bf8-4a8f-8d7e-bc824b52b575"
  val numismaticsCollection = "8ea5fa45-b331-47ee-a583-33cd0ca92c82"

  def get(objId: Int, oldSchema: String) =
    wsUrl(s"/delphi/objects/$objId")
      .withHeaders(fakeToken.asHeader)
      .withQueryString("schemaName" -> oldSchema)
      .get()

  "The DelphiBridgeController" when {

    "finding the current location for an old object ID and schema" should {

      "return the objects current nodeId and path location" in {
        val expectedLocation = "Utviklingsmuseet, Utviklingsmuseet Org, " +
          "Forskningens hus, Naturværelset"
        val res = get(111, "USD_ARK_GJENSTAND_O").futureValue
        res.status mustBe OK
        (res.json \ "nodeId").as[Int] mustBe 5
        (res.json \ "currentLocation").as[String] mustBe expectedLocation
      }

    }

  }

}
