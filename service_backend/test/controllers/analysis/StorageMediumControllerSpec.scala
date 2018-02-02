package controllers.analysis

import no.uio.musit.models.MuseumId
import no.uio.musit.security.BearerToken
import no.uio.musit.test.matchers.DateTimeMatchers
import no.uio.musit.test.{FakeUsers, MusitSpecWithServerPerSuite}
import play.api.libs.json.JsArray
import play.api.test.Helpers._

class StorageMediumControllerSpec
    extends MusitSpecWithServerPerSuite
    with DateTimeMatchers {

  val mid       = MuseumId(99)
  val token     = BearerToken(FakeUsers.testAdminToken)
  val tokenTest = BearerToken(FakeUsers.testUserToken)

  val smUrl = s"/storagemediums"

  "Invoking the storageMedium controller API" should {

    "list all storage mediums" in {
      val res = wsUrl(smUrl).withHttpHeaders(token.asHeader).get().futureValue
      res.status mustBe OK
      val treatments = res.json.as[JsArray].value
      treatments.size mustBe 28
    }

    /*"return 403 Forbidden when trying to list all storage mediums without access to the module" in {
      val res = wsUrl(smUrl).withHttpHeaders(tokenTest.asHeader).get().futureValue
      res.status mustBe FORBIDDEN
    }*/

  }

}
