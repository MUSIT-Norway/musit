package controllers.actor

import no.uio.musit.security.BearerToken
import no.uio.musit.test.{FakeUsers, MusitSpecWithServerPerSuite}
import play.api.http.Status

class UserControllerIntegrationSpec extends MusitSpecWithServerPerSuite {

  val token = BearerToken(FakeUsers.fakeGuestToken)

  "The UserController" must {

    "get 401 when not providing a bearer token" in {
      wsUrl("/dataporten/currentUser").get().futureValue.status mustBe 401
    }

    "get actor with matching dataportenId" in {
      val res =
        wsUrl("/dataporten/currentUser").withHeaders(token.asHeader).get().futureValue

      res.status mustBe Status.OK
      (res.json \ "fn").as[String] mustBe "Gjestebruker"
    }
  }

}
