/**
 * Created by ellenjo on 4/15/16.
 */

import no.uio.musit.microservices.common.PlayTestDefaults
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json._

import scala.language.postfixOps

class MusitThingIntegrationSpec extends PlaySpec with OneServerPerSuite with ScalaFutures {

  override lazy val port: Int = 19003

  implicit override lazy val app = new GuiceApplicationBuilder()
    .configure(PlayTestDefaults.inMemoryDatabaseConfig())
    .build()

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  "MusitThing integration" must {
    "get by id" in {
      val response = wsUrl("/v1/1").get().futureValue
      val json = Json.parse(response.body)
      json \ "id" mustBe JsDefined(JsNumber(1))
      val displayId = json \ "displayid"
      displayId mustBe JsDefined(JsString("C1"))
      val displayName = json \ "displayname"
      displayName mustBe JsDefined(JsString("Øks5"))
    }

    "fail adding with lalala json" in {
      val response = wsUrl("/v1").post(JsString("Lalalal")).futureValue
      val json = Json.parse(response.body)
      json \ "message" mustBe JsDefined(JsString("Input is not valid: \"Lalalal\""))
    }

    "fail adding with loko json" in {
      val response = wsUrl("/v1").post(Json.toJson(Map("test" -> "loko"))).futureValue
      val json = Json.parse(response.body)
      json \ "message" mustBe JsDefined(JsString("Input is not valid: {\"test\":\"loko\"}"))
    }

    "succeed adding with correct json" in {
      val response = wsUrl("/v1").post(
        Json.toJson(Map(
          "displayid" -> "loko",
          "displayname" -> "displayname2"
        ))
      ).futureValue
      val json = Json.parse(response.body)
      val id = json \ "id"
      id mustBe JsDefined(JsNumber(3))
      val displayId = json \ "displayid"
      displayId mustBe JsDefined(JsString("loko"))
      val displayName = json \ "displayname"
      displayName mustBe JsDefined(JsString("displayname2"))
    }
  }
}
