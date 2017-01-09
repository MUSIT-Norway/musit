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

package no.uio.musit.security.dataporten

import java.util.UUID

import no.uio.musit.models.{ActorId, Email}
import no.uio.musit.security.{BearerToken, SessionUUID}
import no.uio.musit.security.oauth2.OAuth2Info
import no.uio.musit.test.MusitSpecWithAppPerSuite
import org.scalamock.scalatest.MockFactory
import play.api.Configuration
import play.api.http.{DefaultWriteables, Writeable}
import play.api.libs.json.Json
import play.api.libs.ws.{WSAPI, WSRequest, WSResponse}
import play.api.mvc.{Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class DataportenAuthenticatorSpec extends MusitSpecWithAppPerSuite
    with MockFactory
    with DefaultWriteables {

  val conf = fromInstanceCache[Configuration]
  val resolver = fromInstanceCache[DatabaseAuthResolver]

  val mockWS = mock[WSAPI]
  val mockWSRequest = mock[WSRequest]
  val mockWSResponse = mock[WSResponse]

  val authenticator = new DataportenAuthenticator(conf, resolver, mockWS)

  val userId = ActorId.generate()
  val userIdSec = Email("vader@deathstar.io")
  val name = "Darth Vader"
  val email = Email("darth.vader@deathstar.io")

  type FormDataType = Map[String, Seq[String]]

  "DataportenAuthenticator" should {

    var sessionId = ""

    "initialise a new UserSession when starting the authentication process" in {

      implicit val fakeRequest = FakeRequest("GET", "/authenticate")

      val futRes = authenticator.authenticate()
      val res = futRes.futureValue
      res.isLeft mustBe true
      res.left.get mustBe a[Result]

      val redirectLoc = redirectLocation(futRes.map(_.left.get))
      redirectLoc must not be None

      sessionId = redirectLoc.get.substring(redirectLoc.get.lastIndexOf('=') + 1)
      SessionUUID.validate(sessionId).isSuccess mustBe true
    }

    "fetch an access token and update the UserSession when receiving a code" in {
      val code = UUID.randomUUID().toString
      val token = BearerToken(UUID.randomUUID().toString)
      val expiresIn = (2 hours).toMillis
      implicit val fakeRequest = FakeRequest(
        "GET",
        s"/authenticate?code=$code&state=$sessionId"
      )

      // Setup some mocks to avoid actually calling Dataporten.
      (mockWS.url _).expects("https://auth.dataporten.no/oauth/token")
        .returning(mockWSRequest)

      (mockWSRequest.post(_: FormDataType)(_: Writeable[FormDataType]))
        .expects(*, *)
        .returning(Future.successful(mockWSResponse))

      (mockWSResponse.json _).expects().returning(
        Json.obj(
          "access_token" -> token.underlying,
          "expiresIn" -> expiresIn
        )
      )

      (mockWS.url _).expects("https://auth.dataporten.no/userinfo")
        .returning(mockWSRequest)

      (mockWSRequest.withHeaders _).expects(*).returning(mockWSRequest)
      (mockWSRequest.get _).expects().returning(Future.successful(mockWSResponse))
      (mockWSResponse.status _).expects().returning(OK)
      (mockWSResponse.json _).expects().returning(
        Json.obj(
          "audience" -> "",
          "user" -> Json.obj(
            "userid" -> userId.asString,
            "userid_sec" -> userIdSec.value,
            "name" -> name,
            "email" -> email.value
          )
        )
      )

      val futRes = authenticator.authenticate()

      val res = futRes.futureValue
      res.isRight mustBe true
    }

  }

}
