package no.uio.musit.microservice.actor.resource

import no.uio.musit.microservice.actor.domain.OrganizationAddress
import no.uio.musit.microservice.actor.testdata.ActorJsonGenerator._
import no.uio.musit.microservices.common.PlayTestDefaults
import no.uio.musit.microservices.common.domain.{MusitError, MusitStatusMessage}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.WSResponse

import scala.concurrent.Future

class OrganizationAddressIntegrationSpec extends PlaySpec with OneServerPerSuite with ScalaFutures {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  override lazy val port: Int = 19008

  implicit override lazy val app =
    new GuiceApplicationBuilder()
      .configure(PlayTestDefaults.inMemoryDatabaseConfig())
      .build()

  def postOrganizationAddress(orgId: Int, json: JsValue): Future[WSResponse] = {
    wsUrl(s"/v1/organization/$orgId/address").post(json)
  }

  "OrganizationAddressIntegration " must {

    "get by id" in {
      val response = wsUrl("/v1/organization/1/address/1").get().futureValue
      val addr = Json.parse(response.body).validate[OrganizationAddress].get
      addr.id mustBe Some(1)
      addr.organizationId mustBe Some(1)
    }
    "negative get by id" in {
      val response = wsUrl("/v1/organization/1/address/9999").get().futureValue
      val error = Json.parse(response.body).validate[MusitError].get
      error.message mustBe "Did not find object with id: 9999"
    }
    "get all addresses for an organization" in {
      val response = wsUrl("/v1/organization/1/address").get().futureValue
      val orgs = Json.parse(response.body).validate[Seq[OrganizationAddress]].get
      orgs.length mustBe 1
    }
    "create address" in {
      val reqBody = organizationAddressJson
      val response = postOrganizationAddress(1, reqBody).futureValue
      response.status mustBe Status.CREATED

      val addr = Json.parse(response.body).validate[OrganizationAddress]
      addr.isSuccess mustBe true

      addr.get.id must not be None
      addr.get.organizationId mustBe Some(1)
      addr.get.addressType mustBe "TEST"
      addr.get.streetAddress mustBe "Foo street 2"
    }

    "not create organizationAddress with illegal input" in {
      val reqBody = organizationAddressIllegalJson
      val response = postOrganizationAddress(1, reqBody).futureValue
      response.status mustBe Status.BAD_REQUEST
    }

    "update address" in {
      val reqBody = organizationAddressJson.as[JsObject] +
        ("id" -> Json.toJson(2)) +
        ("organizationId" -> Json.toJson(1)) +
        ("streetAddress" -> Json.toJson("Foo street 3")) +
        ("locality" -> Json.toJson("Bar place")) +
        ("postalCode" -> Json.toJson("0001")) +
        ("countryName" -> Json.toJson("Norway")) +
        ("latitude" -> Json.toJson(70)) +
        ("longitude" -> Json.toJson(12))

      val response = wsUrl("/v1/organization/1/address/2").put(reqBody).futureValue
      val status = Json.parse(response.body).validate[MusitStatusMessage].get
      status.message mustBe "Record was updated!"
    }

    "not update address with illegal id" in {
      val reqBody = organizationAddressJson.as[JsObject] +
        ("id" -> Json.toJson(999))

      val response = wsUrl("/v1/organization/1/address/999").put(reqBody).futureValue
      response.status mustBe Status.BAD_REQUEST
    }

    "not update address with illegal json" in {
      val reqBody = Json.toJson(
        OrganizationAddress(Some(2), Some(1), "TEST", "Foo street 3", "Bar place", "0001", "Norway", 0.0, 0.0)
      )
      val response = wsUrl("/v1/organization/1/address/2").put(reqBody).futureValue
      val status = Json.parse(response.body).validate[MusitStatusMessage].get
      status.message mustBe "Record was updated!"
    }

    "delete address" in {
      val response = wsUrl("/v1/organization/1/address/2").delete().futureValue
      val msm = Json.parse(response.body).validate[MusitStatusMessage].get
      msm.message mustBe "Deleted 1 record(s)."
    }
  }

}
