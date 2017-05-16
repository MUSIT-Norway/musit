package controllers.geolocation

import no.uio.musit.security.BearerToken
import no.uio.musit.test.{FakeUsers, MusitSpecWithServerPerSuite}
import org.scalatest.Inside
import play.api.http.Status
import play.api.libs.json.JsArray

import scala.language.postfixOps

class GeoLocationControllerIntegrationSpec
    extends MusitSpecWithServerPerSuite
    with Inside {

  val queryParam = (adr: String) => s"/address?search=$adr"

  val fakeToken = BearerToken(FakeUsers.testUserToken)

  "Using the GeoLocation API" when {
    "searching for addresses" should {
      "return a list of results matching the query paramter" in {
        val res = wsUrl(queryParam("Paal Bergsvei 56, Rykkinn"))
          .withHeaders(fakeToken.asHeader)
          .get()
          .futureValue
        res.status mustBe Status.OK

        val jsArr = res.json.as[JsArray].value
        jsArr must not be empty

        (jsArr.head \ "street").as[String] mustBe "Paal Bergs vei"
        (jsArr.head \ "streetNo").as[String] mustBe "56"
        (jsArr.head \ "place").as[String] mustBe "RYKKINN"
        (jsArr.head \ "zip").as[String] mustBe "1348"
      }

      "not return any data if the address doesn't exist" in {
        val res =
          wsUrl(queryParam("nykt")).withHeaders(fakeToken.asHeader).get().futureValue

        res.status mustBe Status.OK
        res.json.as[JsArray].value mustBe empty
      }

      "pad with leading 0 for results where zip is a 3 digit integer" in {
        val res = wsUrl(queryParam("oslo gate 20, oslo"))
          .withHeaders(fakeToken.asHeader)
          .get()
          .futureValue

        res.status mustBe Status.OK

        val jsArr = res.json.as[JsArray].value
        jsArr must not be empty

        (jsArr.head \ "street").as[String] mustBe "Oslo gate"
        (jsArr.head \ "streetNo").as[String] mustBe "20"
        (jsArr.head \ "place").as[String] mustBe "OSLO"
        (jsArr.head \ "zip").as[String] mustBe "0192"
      }
    }
  }
}
