/**
 * Created by jarle on 27.09.16.
 */
/*
 *   MUSIT is a cooperation between the university museums of Norway.
 *   Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License,
 *   or any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License along
 *   with this program; if not, write to the Free Software Foundation, Inc.,
 *   51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */
package no.uio.musit.security

import akka.stream.Materializer
import no.uio.musit.security.SecurityGroups.{ Read, Write }
import org.scalatestplus.play.{ OneAppPerSuite, PlaySpec }
import play.api.libs.json.Json
import play.api.mvc.EssentialAction
import play.api.mvc.Results._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.mvc.Http.Status

object LocalUtils {
  implicit class FakeRequestUtils[A](val wsr: FakeRequest[A]) {
    def withFakeUser(fakeUserId: String): FakeRequest[A] = wsr.withHeaders("Authorization" -> ("Bearer " + FakeSecurity.fakeAccessTokenPrefix + fakeUserId))
    def withFakeUser: FakeRequest[A] = withFakeUser("musitTestUser")
  }
}

class MusitSecureActionSpec extends PlaySpec with OneAppPerSuite {

  /*TODO: How to get this to work?
  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )
  */

  implicit lazy val materializer: Materializer = app.materializer

  import LocalUtils._

  "A MusitSecureAction" should {

    "fail with UNAUTHORIZED if missing access token" in {
      val action: EssentialAction = MusitSecureAction() { request =>
        val value = (request.body.asJson.get \ "field").as[String]
        assert(false) //Should never get here
        Ok(value)
      }

      val request = FakeRequest(POST, "/").withJsonBody(Json.parse("""{ "field": "value" }"""))
      val result = call(action, request)
      status(result) mustEqual Status.UNAUTHORIZED
      contentAsString(result) must include(Security.noTokenInRequestMsg)
    }

    "Ok if the request has a valid access token (using fakes for test)" in {

      val fakeUserName = "Fake Musit Test User"

      val action: EssentialAction = MusitSecureAction() { request =>
        request.user.userName mustBe fakeUserName
        Ok(s"hello: ${request.user.userName}")
      }

      val request = FakeRequest(POST, "/").withJsonBody(Json.parse("""{ "field": "value" }""")).withFakeUser
      val result = call(action, request)
      status(result) mustEqual Status.OK
      contentAsString(result) must include(fakeUserName)
    }

    val fakeUserWithKhmReadAccess = "musitTestUserKhmRead"

    "Accept a read request when the user has Read (on the correct museum)" in {
      val action: EssentialAction = MusitSecureAction(Khm.id.underlying, Read) { request =>
        Ok(s"hello: ${request.user.userName}")
      }

      val request = FakeRequest(POST, "/").withJsonBody(Json.parse("""{ "field": "value" }""")).withFakeUser(fakeUserWithKhmReadAccess)
      val result = call(action, request)
      status(result) mustEqual Status.OK
    }

    "Don't accept a read request (with FORBIDDEN) when the user has Read on 'wrong' (another) museum, not the correct one" in {
      val action: EssentialAction = MusitSecureAction(Vm.id.underlying, Read) { request =>
        Ok(s"hello: ${request.user.userName}")
      }

      val request = FakeRequest(POST, "/").withJsonBody(Json.parse("""{ "field": "value" }""")).withFakeUser(fakeUserWithKhmReadAccess)
      val result = call(action, request)
      status(result) mustEqual Status.FORBIDDEN
    }

    "Don't accept a read request (with FORBIDDEN) when the user has Read, but not Write and the required permission includes Write, variant 1" in {
      val action: EssentialAction = MusitSecureAction(Khm.id.underlying, Read, Write) { request =>
        Ok(s"hello: ${request.user.userName}")
      }

      val request = FakeRequest(POST, "/").withJsonBody(Json.parse("""{ "field": "value" }""")).withFakeUser(fakeUserWithKhmReadAccess)
      val result = call(action, request)
      status(result) mustEqual Status.FORBIDDEN
    }

    "Don't accept a read request (with FORBIDDEN) when the user has Read, but not Write and the required permission includes Write, variant 2" in {
      val action: EssentialAction = MusitSecureAction(Khm.id.underlying, Write) { request =>
        Ok(s"hello: ${request.user.userName}")
      }

      val request = FakeRequest(POST, "/").withJsonBody(Json.parse("""{ "field": "value" }""")).withFakeUser(fakeUserWithKhmReadAccess)
      val result = call(action, request)
      status(result) mustEqual Status.FORBIDDEN
    }
  }
}