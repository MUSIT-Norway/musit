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
import play.api.libs.json.JsArray
import play.api.test.Helpers._

class StatsControllerSpec extends MusitSpecWithServerPerSuite {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  val fakeToken1 = BearerToken(FakeAuthenticator.fakeAccessTokenPrefix + "musitTestUser")
  val fakeToken2 = BearerToken(FakeAuthenticator.fakeAccessTokenPrefix + "superuser")

  "Calling the stats endpoint" should {
    "return stats for a node including objects per collection" in {

      val res = wsUrl("/museum/99/storagenodes/4/stats")
        .withHeaders(fakeToken1.asHeader)
        .get()
        .futureValue

      res.status mustBe OK

      (res.json \ "numNodes").as[Int] mustBe 3
      (res.json \ "numObjects").as[Int] mustBe 4
      (res.json \ "totalObjects").as[Int] mustBe 54
    }
  }

}
