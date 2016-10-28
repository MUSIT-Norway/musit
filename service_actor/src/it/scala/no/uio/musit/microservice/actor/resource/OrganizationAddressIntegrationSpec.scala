package no.uio.musit.microservice.actor.resource

import no.uio.musit.microservice.actor.testdata.ActorJsonGenerator._
import no.uio.musit.test.MusitSpecWithServerPerSuite
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.http.Status
import play.api.libs.json.{JsArray, JsObject, JsValue, Json}
import play.api.libs.ws.WSResponse

import scala.concurrent.Future

class OrganizationAddressIntegrationSpec extends MusitSpecWithServerPerSuite {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  def postOrganizationAddress(orgId: Int, json: JsValue): Future[WSResponse] = {
    wsUrl(s"/v1/organization/$orgId/address").post(json)
  }

  "OrganizationAddressIntegration " must {

    "get by id" in {
      val res = wsUrl("/v1/organization/1/address/1").get().futureValue
      res.status mustBe Status.OK
      val addr = res.json
      (addr \ "id").as[Int] mustBe 1
      (addr \ "organizationId").as[Int] mustBe 1
    }
    "negative get by id" in {
      val res = wsUrl("/v1/organization/1/address/999").get().futureValue
      (res.json \ "message").as[String] mustBe "Did not find object with id: 999"
    }
    "get all addresses for an organization" in {
      val res = wsUrl("/v1/organization/1/address").get().futureValue
      res.json.as[JsArray].value.length mustBe 1
    }
    "create address" in {
      val reqBody = orgAddressJson
      val res = postOrganizationAddress(1, reqBody).futureValue
      res.status mustBe Status.CREATED

      (res.json \ "id").asOpt[Int] must not be None
      (res.json \ "organizationId").as[Int] mustBe 1
      (res.json \ "addressType").as[String] mustBe "TEST"
      (res.json \ "streetAddress").as[String] mustBe "Foo street 2"
    }

    "not create organizationAddress with illegal input" in {
      val res = postOrganizationAddress(1, orgAddressIllegalJson).futureValue
      res.status mustBe Status.BAD_REQUEST
    }

    "update address" in {
      val reqBody = orgAddressJson.as[JsObject] ++ Json.obj(
        "id" -> 2,
        "organizationId" -> 1,
        "streetAddress" -> "Foo street 3",
        "locality" -> "Bar place",
        "postalCode" -> "0001",
        "countryName" -> "Norway",
        "latitude" -> 70,
        "longitude" -> 12
      )

      val res = wsUrl("/v1/organization/1/address/2").put(reqBody).futureValue
      (res.json \ "message").as[String] mustBe "Record was updated!"
    }

    "not update address with illegal id" in {
      val reqBody = orgAddressJson.as[JsObject] ++ Json.obj("id" -> 999)
      val res = wsUrl("/v1/organization/1/address/999").put(reqBody).futureValue
      res.status mustBe Status.OK
      (res.json \ "message").as[String] mustBe "No records were updated!"
    }

    "not update address with illegal json" in {
      val reqBody = Json.obj(
        "id" -> 2,
        "organizationId" -> 1,
        "adresseType" -> "TEST",
        "stretAddress" -> "Foo street 3",
        "locality" -> "Bar place",
        "postalCode" -> "0001",
        "countryName" -> "Norway",
        "latitude" -> 0.0,
        "longitude" -> 0.0
      )
      val res = wsUrl("/v1/organization/1/address/2").put(reqBody).futureValue
      res.status mustBe Status.BAD_REQUEST
    }

    "delete address" in {
      val res = wsUrl("/v1/organization/1/address/2").delete().futureValue
      (res.json \ "message").as[String] mustBe "Deleted 1 record(s)."
    }
  }

}
