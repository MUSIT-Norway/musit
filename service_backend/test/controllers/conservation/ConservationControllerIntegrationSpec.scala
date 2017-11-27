package controllers.conservation

import no.uio.musit.models.MuseumId
import no.uio.musit.security.BearerToken
import no.uio.musit.test.{FakeUsers, MusitSpecWithServerPerSuite}
import no.uio.musit.test.matchers.DateTimeMatchers
import play.api.libs.json.JsArray
import play.api.test.Helpers._

class ConservationControllerIntegrationSpec
    extends MusitSpecWithServerPerSuite
    with DateTimeMatchers {
  val mid       = MuseumId(99)
  val token     = BearerToken(FakeUsers.testAdminToken)
  val tokenRead = BearerToken(FakeUsers.testReadToken)
  val tokenTest = BearerToken(FakeUsers.testUserToken)

  val baseUrl = (mid: Int) => s"/$mid/conservation"

  val typesUrl       = (mid: Int) => s"${baseUrl(mid)}/types"
  val getRoleListUrl = s"/conservation/roles"

  "Using the conservation controller" when {

    "fetching conservation types" should {

      "return all event types" in {
        val res =
          wsUrl(typesUrl(mid)).withHttpHeaders(tokenRead.asHeader).get().futureValue

        res.status mustBe OK
        res.json.as[JsArray].value.size mustBe 5
        (res.json \ 0 \ "noName").as[String] mustBe "konserveringsprosess"
        (res.json \ 0 \ "id").as[Int] mustBe 1
      }
    }
    "fetching role list" should {

      "return the list of roles for actors in conservation events " in {
        val res =
          wsUrl(getRoleListUrl).withHttpHeaders(tokenRead.asHeader).get().futureValue
        res.status mustBe OK
        res.json.as[JsArray].value.size mustBe 2
        (res.json \ 0 \ "noRole").as[String] mustBe "Utført av"
        (res.json \ 0 \ "enRole").as[String] mustBe "Done by"
        (res.json \ 0 \ "roleId").as[Int] mustBe 1
        (res.json \ 1 \ "noRole").as[String] mustBe "Deltatt i"
        (res.json \ 1 \ "enRole").as[String] mustBe "Participated in"
        (res.json \ 1 \ "roleId").as[Int] mustBe 2
      }
    }
  }
}
