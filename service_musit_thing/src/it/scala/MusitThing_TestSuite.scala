/**
  * Created by ellenjo on 4/15/16.
  */

import no.uio.musit.microservice.musitThing.dao.MusitThingDao
import no.uio.musit.microservice.musitThing.domain.MusitThing
import no.uio.musit.microservices.common.PlayTestDefaults
import no.uio.musit.microservices.common.linking.LinkService
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json._
import play.api.libs.ws.WS
import MusitThingDao._
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import scala.concurrent.duration._

class MusitThing_TestSuite extends PlaySpec with OneServerPerSuite with ScalaFutures {

  val timeout = PlayTestDefaults.timeout
  implicit override lazy val app = new GuiceApplicationBuilder().configure(PlayTestDefaults.inMemoryDatabaseConfig()).build()

  override lazy val port: Int = 19003

  "MusitThing integration " must {
    "get by id" in {
      val response = WS.url(s"http://localhost:$port/v1/1").get().futureValue(Timeout(30 seconds))
      val json = Json.parse(response.body)
      json \ "id" mustBe JsDefined(JsNumber(1))
      val displayId = json \ "displayid"
      displayId mustBe JsDefined(JsString("C1"))
      val displayName = json \ "displayname"
      displayName mustBe JsDefined(JsString("Øks5"))
    }

    "fail adding with lalala json" in {
      val response = WS.url(s"http://localhost:$port/v1").post(JsString("Lalalal")).futureValue(Timeout(30 seconds))
      val json = Json.parse(response.body)
      json \ "message" mustBe JsDefined(JsString("Input is not valid: \"Lalalal\""))
    }

    "fail adding with loko json" in {
      val response = WS.url(s"http://localhost:$port/v1").post(Json.toJson(Map("test" -> "loko"))).futureValue(Timeout(30 seconds))
      val json = Json.parse(response.body)
      json \ "message" mustBe JsDefined(JsString("Input is not valid: {\"test\":\"loko\"}"))
    }

    "succeed adding with correct json" in {
      val response = WS.url(s"http://localhost:$port/v1").post(
        Json.toJson(Map(
          "displayid" -> "loko",
          "displayname" -> "displayname2"
        ))
      ).futureValue(Timeout(30 seconds))
      val json = Json.parse(response.body)
      val id = json \ "id"
      id mustBe JsDefined(JsNumber(3))
      val displayId = json \ "displayid"
      displayId mustBe JsDefined(JsString("loko"))
      val displayName = json \ "displayname"
      displayName mustBe JsDefined(JsString("displayname2"))
    }

    "testInsertMusitThing" in {
      insert(MusitThing(Some(1), "C2", "spyd", None))
      insert(MusitThing(Some(2), "C3", "øks", None))
      val svar = MusitThingDao.all.futureValue
      svar.length === 4
    }

    "getDisplayName_kjempeTall" in {
      val svar = getDisplayName(6386363673636335366L).futureValue
      svar mustBe None
    }

    "getDisplayName_Riktig" in {
      val svar = getDisplayName(2).futureValue
      svar mustBe Some("Kniv7")
    }

    "getDisplayName_TalletNull" in {
      val svar = getDisplayName(0).futureValue
      svar mustBe None
    }

    "getDisplayID_kjempeTall" in {
      val svar = getDisplayID(6386363673636335366L).futureValue
      svar mustBe None
    }

    "getDisplayID_Riktig" in {
      val svar = getDisplayID(2).futureValue
      svar mustBe Some("C2")
    }

    "getDisplayID_TalletNull" in {
      val svar = getDisplayID(0).futureValue
      svar mustBe None
    }

    "getById_kjempeTall" in {
      val svar = getById(6386363673636335366L).futureValue
      svar mustBe None
    }

    "getById__Riktig" in {
      val svar = getById(1).futureValue
      svar.contains(MusitThing(Some(1), "C1", "Øks5", Some(Seq(LinkService.self("/v1/1")))))
    }

    "getById__TalletNull" in {
      val svar = getById(0).futureValue
      svar mustBe None
    }
  }
}
