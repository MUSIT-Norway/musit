package controllers.analysis

import no.uio.musit.security.BearerToken
import no.uio.musit.test.matchers.DateTimeMatchers
import no.uio.musit.test.{FakeUsers, MusitSpecWithServerPerSuite}
import play.api.libs.json.JsArray
import play.api.test.Helpers._

class PurposeControllerIntegrationSpec
    extends MusitSpecWithServerPerSuite
    with DateTimeMatchers {

  val token = BearerToken(FakeUsers.testAdminToken)

  val purposeUrl = s"/purposes"

  "Invoking the treatment controller API" should {

    "list all purposes" in {
      val res = wsUrl(purposeUrl).withHttpHeaders(token.asHeader).get().futureValue
      res.status mustBe OK
      val purposes = res.json.as[JsArray].value
      purposes.size mustBe 4
      val purpose = purposes.head
      (purpose \ "id").as[Int] mustBe 1
      (purpose \ "noPurpose").as[String] mustBe "Materialbestemmelse"
      (purpose \ "enPurpose").as[String] mustBe "Material determination"
    }

  }
}
