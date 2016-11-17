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

package no.uio.musit.service

import akka.stream.Materializer
import no.uio.musit.models.Museums._
import no.uio.musit.security._
import FakeAuthenticator.fakeAccessTokenPrefix
import Permissions._
import no.uio.musit.models.MuseumId
import no.uio.musit.test.MusitSpecWithAppPerSuite
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.mvc.Results._
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future

class MusitSecureActionSpec extends MusitSpecWithAppPerSuite {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  implicit lazy val materializer: Materializer = app.materializer

  class Dummy extends MusitActions {
    override val authService: Authenticator = new FakeAuthenticator
  }

  "A MusitSecureAction" when {

    "used without permissions on a controller" should {

      "return HTTP Unauthorized if bearer token is missing" in {
        val action = new Dummy().MusitSecureAction()(request => Ok)

        val req = FakeRequest(GET, "/")
        val res = call(action, req)

        status(res) mustEqual UNAUTHORIZED
      }

      "return OK if the request has a valid bearer token" in {
        val userId = "caef058a-16d7-400f-9f73-3736477c6b44"
        val token = BearerToken(fakeAccessTokenPrefix + "musitTestUserTestRead")

        val action = new Dummy().MusitSecureAction().async { request =>
          request.token mustBe token
          request.user.userInfo.id.asString mustBe userId
          Future.successful(Ok(request.user.userInfo.id.asString))
        }

        val req = FakeRequest(GET, "/").withHeaders(token.asHeader)
        val res = call(action, req)

        status(res) mustEqual OK
        contentAsString(res) must include(userId)
      }
    }

    "used with Read restrictions on a controller" should {

      "accept requests if the user has read access to the museum" in {
        val userId = "caef058a-16d7-400f-9f73-3736477c6b44"
        val token = BearerToken(fakeAccessTokenPrefix + "musitTestUserTestRead")

        val action = new Dummy().MusitSecureAction(Test.id, Read) { request =>
          request.token mustBe token
          request.user.userInfo.id.asString mustBe userId
          Ok(request.user.userInfo.id.asString)
        }

        val req = FakeRequest(GET, "/").withHeaders(token.asHeader)
        val res = call(action, req)

        status(res) mustEqual OK
        contentAsString(res) must include(userId)
      }

      "accept requests if the user has write access to the museum" in {
        val userId = "a5d2a21b-ea1a-4123-bc37-6d31b81d1b2a"
        val token = BearerToken(fakeAccessTokenPrefix + "musitTestUserTestWrite")

        val action = new Dummy().MusitSecureAction(Test.id, Read) { request =>
          request.token mustBe token
          request.user.userInfo.id.asString mustBe userId
          Ok(request.user.userInfo.id.asString)
        }

        val req = FakeRequest(GET, "/").withHeaders(token.asHeader)
        val res = call(action, req)

        status(res) mustEqual OK
        contentAsString(res) must include(userId)
      }

      "accept requests if the user has God permission" in {
        val userId = "896125d3-0563-46b6-a7c5-51f3f899ff0a"
        val token = BearerToken(fakeAccessTokenPrefix + "superuser")

        val action = new Dummy().MusitSecureAction(Test.id, Read) { request =>
          request.token mustBe token
          request.user.userInfo.id.asString mustBe userId
          Ok(request.user.userInfo.id.asString)
        }

        val req = FakeRequest(GET, "/").withHeaders(token.asHeader)
        val res = call(action, req)

        status(res) mustEqual OK
        contentAsString(res) must include(userId)
      }

      "reject requests if the user hasn't got read access to the museum" in {
        val userId = "ddb4dc62-8c14-4bac-aafd-0401f619b0ac"
        val token = BearerToken(fakeAccessTokenPrefix + "musitTestUserNhmRead")

        val action = new Dummy().MusitSecureAction(Test.id, Read)(_ => Ok)

        val req = FakeRequest(GET, "/").withHeaders(token.asHeader)
        val res = call(action, req)

        status(res) mustEqual FORBIDDEN
      }
    }

    "used with Write restrictions on a controller" should {
      "accept requests if the user has got write access to the museum" in {
        val userId = "d2bad684-d41e-4b9f-b813-70a75ea1728a"
        val token = BearerToken(fakeAccessTokenPrefix + "musitTestUserNhmWrite")

        val action = new Dummy().MusitSecureAction(Nhm.id, Write) { request =>
          request.token mustBe token
          request.user.userInfo.id.asString mustBe userId
          Ok(request.user.userInfo.id.asString)
        }

        val req = FakeRequest(GET, "/").withHeaders(token.asHeader)
        val res = call(action, req)

        status(res) mustEqual OK
        contentAsString(res) must include(userId)
      }

      "accept requests if the user has god permissions" in {
        val userId = "896125d3-0563-46b6-a7c5-51f3f899ff0a"
        val token = BearerToken(fakeAccessTokenPrefix + "superuser")

        val action = new Dummy().MusitSecureAction(Nhm.id, Write) { request =>
          request.token mustBe token
          request.user.userInfo.id.asString mustBe userId
          Ok(request.user.userInfo.id.asString)
        }

        val req = FakeRequest(GET, "/").withHeaders(token.asHeader)
        val res = call(action, req)

        status(res) mustEqual OK
        contentAsString(res) must include(userId)
      }

      "reject requests if the user hasn't got write access to the museum" in {
        val userId = "ddb4dc62-8c14-4bac-aafd-0401f619b0ac"
        val token = BearerToken(fakeAccessTokenPrefix + "musitTestUserNhmRead")

        val action = new Dummy().MusitSecureAction(Nhm.id, Write)(_ => Ok)

        val req = FakeRequest(GET, "/").withHeaders(token.asHeader)
        val res = call(action, req)

        status(res) mustEqual FORBIDDEN
      }

    }

    "used with Admin restrictions on a controller" should {
      "accept requests if the user has got admin access to the museum" in {
        val userId = "84e47cd0-a9de-4513-9318-1dec4bac5433"
        val token = BearerToken(fakeAccessTokenPrefix + "musitTestUserNhmAdmin")

        val action = new Dummy().MusitSecureAction(Nhm.id, Admin) { request =>
          request.token mustBe token
          request.user.userInfo.id.asString mustBe userId
          Ok(request.user.userInfo.id.asString)
        }

        val req = FakeRequest(GET, "/").withHeaders(token.asHeader)
        val res = call(action, req)

        status(res) mustEqual OK
        contentAsString(res) must include(userId)
      }
    }

    "used with God rights on a controller" should {
      "accept requests if the user has got God access to the application" in {
        val userId = "896125d3-0563-46b6-a7c5-51f3f899ff0a"
        val token = BearerToken(fakeAccessTokenPrefix + "superuser")

        val action = new Dummy().MusitSecureAction(Nhm.id, GodMode) { request =>
          request.token mustBe token
          request.user.userInfo.id.asString mustBe userId
          Ok(request.user.userInfo.id.asString)
        }

        val req = FakeRequest(GET, "/").withHeaders(token.asHeader)
        val res = call(action, req)

        status(res) mustEqual OK
        contentAsString(res) must include(userId)
      }

      "not accept requests from un-Godly users" in {
        val userId = "896125d3-0563-46b6-a7c5-51f3f899ff0a"
        val token = BearerToken(fakeAccessTokenPrefix + "musitTestUserNhmRead")

        val action = new Dummy().MusitSecureAction(Nhm.id, GodMode)(_ => Ok)

        val req = FakeRequest(GET, "/").withHeaders(token.asHeader)
        val res = call(action, req)

        status(res) mustEqual FORBIDDEN
      }
    }
  }
}
