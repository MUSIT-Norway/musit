package no.uio.musit.microservice.actor.resource

import no.uio.musit.microservice.actor.domain.{Organization, OrganizationAddress, Person}
import no.uio.musit.microservices.common.PlayTestDefaults
import no.uio.musit.microservices.common.domain.{MusitError, MusitStatusMessage}
import no.uio.musit.security.FakeSecurity
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WSRequest
import no.uio.musit.microservices.common.extensions.PlayExtensions.WSRequestImp

class ActorIntegrationSpec extends PlaySpec with OneServerPerSuite with ScalaFutures {

  val timeout = PlayTestDefaults.timeout

  override lazy val port: Int = 19006

  implicit override lazy val app = new GuiceApplicationBuilder().configure(PlayTestDefaults.inMemoryDatabaseConfig()).build()


  def withFakeUser(wsr: WSRequest, username: String) = {
    wsr.withBearerToken(FakeSecurity.fakeAccessTokenPrefix + username)
  }

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

  "OrganizationIntegration " must {
    "get by id" in {
      val future = wsUrl("/v1/organization/1").get()
      whenReady(future, timeout) { response =>
        val org = Json.parse(response.body).validate[Organization].get
        org.id mustBe Some(1)
      }
    }
    "negative get by id" in {
      val future = wsUrl("/v1/organization/9999").get()
      whenReady(future, timeout) { response =>
        val error = Json.parse(response.body).validate[MusitError].get
        error.message mustBe "Did not find object with id: 9999"
      }
    }
    "get root" in {
      val future = wsUrl("/v1/organization").get()
      whenReady(future, timeout) { response =>
        val orgs = Json.parse(response.body).validate[Seq[Organization]].get
        orgs.length mustBe 1
      }
    }
    "search on organization" in {
      val future = wsUrl("/v1/organization?search=[KHM]").get()
      whenReady(future, timeout) { response =>
        val orgs = Json.parse(response.body).validate[Seq[Organization]].get
        orgs.length mustBe 1
        orgs.head.fn mustBe "Kulturhistorisk museum - Universitetet i Oslo"
      }
    }
    "create organization" in {
      val reqBody = Json.toJson(
        Organization(None, "Foo Bar", "FB", "12345678", "http://www.foo.bar", None)
      )
      val future = wsUrl("/v1/organization").post(reqBody)
      whenReady(future, timeout) { response =>
        val org = Json.parse(response.body).validate[Organization].get
        org.id mustBe Some(2)
        org.fn mustBe "Foo Bar"
      }
    }
    "update organization" in {
      val reqBody = Json.toJson(
        Organization(Some(2), "Foo Bar 123", "FB", "12345678", "http://www.foo.bar", None)
      )
      val future = wsUrl("/v1/organization/2").put(reqBody)
      whenReady(future, timeout) { response =>
        val message = Json.parse(response.body).validate[MusitStatusMessage].get
        message.message mustBe "Record was updated!"
      }
    }
    "delete organization" in {
      val future = wsUrl("/v1/organization/2").delete()
      whenReady(future, timeout) { response =>
        val msm = Json.parse(response.body).validate[MusitStatusMessage].get
        msm.message mustBe "Deleted 1 record(s)."
      }
    }
  }

  "LegacyPersonIntegration " must {
    "get by id" in {
      val future = wsUrl("/v1/person/1").get()
      whenReady(future, timeout) { response =>
        val person = Json.parse(response.body).validate[Person].get
        person.id mustBe Some(1)
      }
    }
    "negative get by id" in {
      val future = wsUrl("/v1/person/9999").get()
      whenReady(future, timeout) { response =>
        val error = Json.parse(response.body).validate[MusitError].get
        error.message mustBe "Did not find object with id: 9999"
      }
    }
    "search on person" in {
      val future = wsUrl("/v1/person?search=[And]").get()
      whenReady(future, timeout) { response =>
        val persons = Json.parse(response.body).validate[Seq[Person]].get
        persons.length mustBe 1
        persons.head.fn mustBe "And, Arne1"
      }
    }
  }


  "Actor and dataporten integration" must {

    "get 401 when not providing token" in {
      val future = wsUrl("/v1/dataporten/currentUser").get()
      whenReady(future, timeout) { response =>
        response.status mustBe 401
      }
    }

    "get actor with matching dataportenId" in {
      val future = withFakeUser(wsUrl("/v1/dataporten/currentUser"), "jarle").get()
      whenReady(future, timeout) { response =>
        response.status mustBe 200
        val person = Json.parse(response.body).validate[Person].get
        person.fn mustBe "Jarle Stabell"
      }
    }
  }
}
