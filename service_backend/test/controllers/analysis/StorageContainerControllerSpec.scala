package controllers.analysis

import no.uio.musit.models.MuseumId
import no.uio.musit.security.BearerToken
import no.uio.musit.test.matchers.DateTimeMatchers
import no.uio.musit.test.{FakeUsers, MusitSpecWithServerPerSuite}
import play.api.libs.json.JsArray
import play.api.test.Helpers._

class StorageContainerControllerSpec
    extends MusitSpecWithServerPerSuite
    with DateTimeMatchers {

  val mid       = MuseumId(99)
  val token     = BearerToken(FakeUsers.testAdminToken)
  val tokenTest = BearerToken(FakeUsers.testUserToken)

  val scUrl = s"/storagecontainer"

  "Invoking the storagecontainer controller API" should {

    "list all storage containers" in {
      val res = wsUrl(scUrl).withHttpHeaders(token.asHeader).get().futureValue
      res.status mustBe OK
      val storageContainer = res.json.as[JsArray].value
      storageContainer.size mustBe 29
    }

    /* "return 403 Forbidden when trying to list all storage containers without access to the module" in {
      val res = wsUrl(scUrl).withHttpHeaders(tokenTest.asHeader).get().futureValue
      res.status mustBe FORBIDDEN
    }*/
  }

}
