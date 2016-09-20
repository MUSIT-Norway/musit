package no.uio.musit.microservice.actor.resource

import no.uio.musit.microservice.actor.domain.OrganizationAddress
import no.uio.musit.microservices.common.PlayTestDefaults
import no.uio.musit.microservices.common.domain.{MusitError, MusitStatusMessage}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json

/**
  * Created by sveigl on 20.09.16.
  */
class OrganizationAddressIntegrationSpec extends PlaySpec with OneServerPerSuite with ScalaFutures {

  val timeout = PlayTestDefaults.timeout

  override lazy val port: Int = 19007

  implicit override lazy val app = new GuiceApplicationBuilder().configure(PlayTestDefaults.inMemoryDatabaseConfig()).build()

  "OrganizationAddressIntegration " must {

    "get by id" in {
      val future = wsUrl("/v1/organization/1/address/1").get()
      whenReady(future, timeout) { response =>
        val addr = Json.parse(response.body).validate[OrganizationAddress].get
        addr.id mustBe Some(1)
        addr.organizationId mustBe Some(1)
      }
    }
    "negative get by id" in {
      val future = wsUrl("/v1/organization/1/address/9999").get()
      whenReady(future, timeout) { response =>
        val error = Json.parse(response.body).validate[MusitError].get
        error.message mustBe "Did not find object with id: 9999"
      }
    }
    "get all addresses for an organization" in {
      val future = wsUrl("/v1/organization/1/address").get()
      whenReady(future, timeout) { response =>
        val orgs = Json.parse(response.body).validate[Seq[OrganizationAddress]].get
        orgs.length mustBe 1
      }
    }
    "create address" in {
      val reqBody = Json.toJson(
        OrganizationAddress(Some(2), Some(1), "TEST", "Foo street 2", "Bar place", "0001", "Norway", 0.0, 0.0, None)
      )
      val future = wsUrl("/v1/organization/1/address").post(reqBody)
      whenReady(future, timeout) { response =>
        val addr = Json.parse(response.body).validate[OrganizationAddress].get
        addr.id mustBe Some(2)
        addr.organizationId mustBe Some(1)
        addr.addressType mustBe "TEST"
        addr.streetAddress mustBe "Foo street 2"
      }
    }
    "update address" in {
      val reqBody = Json.toJson(
        OrganizationAddress(Some(2), Some(1), "TEST", "Foo street 3", "Bar place", "0001", "Norway", 0.0, 0.0, None)
      )
      val future = wsUrl("/v1/organization/1/address/2").put(reqBody)
      whenReady(future, timeout) { response =>
        val status = Json.parse(response.body).validate[MusitStatusMessage].get
        status.message mustBe "Record was updated!"
      }
    }
    "delete address" in {
      val future = wsUrl("/v1/organization/1/address/2").delete()
      whenReady(future, timeout) { response =>
        val msm = Json.parse(response.body).validate[MusitStatusMessage].get
        msm.message mustBe "Deleted 1 record(s)."
      }
    }
  }


}
