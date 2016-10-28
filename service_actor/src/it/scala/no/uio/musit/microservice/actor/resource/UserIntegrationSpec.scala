package no.uio.musit.microservice.actor.resource

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
      val res = withFakeUser(wsUrl("/v1/dataporten/currentUser"), "jarle").get().futureValue
      res.status mustBe Status.OK
      (res.json \ "fn").as[String] mustBe "Jarle Stabell"
    }
  }

}
