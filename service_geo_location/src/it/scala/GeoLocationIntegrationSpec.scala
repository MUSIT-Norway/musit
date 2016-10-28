/**
 * Created by ellenjo on 4/15/16.
 */

import no.uio.musit.security.{BearerToken, FakeAuthenticator}
import no.uio.musit.test.MusitSpecWithServerPerSuite
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.http.Status
import play.api.libs.json.JsArray

import scala.language.postfixOps

class GeoLocationIntegrationSpec extends MusitSpecWithServerPerSuite {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  val queryParam = (adr: String) => s"/v1/address?search=$adr"

  val fakeToken = BearerToken(FakeAuthenticator.fakeAccessTokenPrefix + "musitTestUser")

  "Using the GeoLocation API" when {
    "searching for addresses" should {
      "return a list of results matching the query paramter" in {
        val res = wsUrl(queryParam("Paal Bergsvei 56, Rykkinn"))
          .withHeaders(fakeToken.asHeader)
          .get().futureValue

        res.status mustBe Status.OK

        val jsArr = res.json.as[JsArray].value
        jsArr must not be empty

        (jsArr.head \ "street").as[String] mustBe "Paal Bergs vei"
        (jsArr.head \ "streetNo").as[String] mustBe "56"
        (jsArr.head \ "place").as[String] mustBe "RYKKINN"
        (jsArr.head \ "zip").as[String] mustBe "1348"
      }
    }
  }

}
