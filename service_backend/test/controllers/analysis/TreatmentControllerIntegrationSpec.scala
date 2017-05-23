package controllers.analysis

import no.uio.musit.models.MuseumId
import no.uio.musit.security.BearerToken
import no.uio.musit.test.matchers.DateTimeMatchers
import no.uio.musit.test.{FakeUsers, MusitSpecWithServerPerSuite}
import play.api.libs.json.JsArray
import play.api.test.Helpers._

class TreatmentControllerIntegrationSpec
    extends MusitSpecWithServerPerSuite
    with DateTimeMatchers {

  val mid   = MuseumId(99)
  val token = BearerToken(FakeUsers.testAdminToken)

  val treatmentUrl = s"/treatments"

  "Invoking the treatment controller API" should {

    "list all treatments" in {
      val res = wsUrl(treatmentUrl).withHeaders(token.asHeader).get().futureValue
      res.status mustBe OK
      val treatments = res.json.as[JsArray].value
      treatments.size mustBe 24
    }

  }

}
