package controllers.analysis

import no.uio.musit.models.MuseumId
import no.uio.musit.security.BearerToken
import no.uio.musit.test.matchers.DateTimeMatchers
import no.uio.musit.test.{FakeUsers, MusitSpecWithServerPerSuite}
import play.api.libs.json.JsArray
import play.api.test.Helpers._

class SampleTypeControllerIntegrationSpec
    extends MusitSpecWithServerPerSuite
    with DateTimeMatchers {

  val mid   = MuseumId(99)
  val token = BearerToken(FakeUsers.testAdminToken)

  val sampleTypeUrl = s"/sampletypes"

  "Invoking the sampleType controller API" should {

    "list all sampleTypes" in {
      val res = wsUrl(sampleTypeUrl).withHeaders(token.asHeader).get().futureValue
      res.status mustBe OK
      val sampleTypes = res.json.as[JsArray].value
      sampleTypes.size mustBe 51
    }

  }
}
