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

import no.uio.musit.security.{BearerToken, FakeAuthenticator}
import no.uio.musit.test.MusitSpecWithServerPerSuite
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.http.Status
import play.api.libs.ws.WSRequest

class UserIntegrationSpec extends MusitSpecWithServerPerSuite {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  def withFakeUser(wsr: WSRequest, username: String) = {
    val token = BearerToken(FakeAuthenticator.fakeAccessTokenPrefix + username)
    wsr.withHeaders(token.asHeader)
  }

  "Actor and dataporten integration" must {

    "get 401 when not providing token" in {
      wsUrl("/v1/dataporten/currentUser").get().futureValue.status mustBe 401
    }

    "get actor with matching dataportenId" in {
      val res = withFakeUser(wsUrl("/v1/dataporten/currentUser"), "guest").get().futureValue
      res.status mustBe Status.OK
      (res.json \ "fn").as[String] mustBe "Gjestebruker"
    }
  }

}
