package no.uio.musit.microservice.actor.resource

import no.uio.musit.microservice.actor.domain.Organization
import no.uio.musit.microservices.common.PlayTestDefaults
import no.uio.musit.microservices.common.domain.{MusitError, MusitStatusMessage}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import no.uio.musit.microservice.actor.testdata.ActorJsonGenerator._

/**
  * Created by sveigl on 20.09.16.
  */
class OrganizationIntegrationSpec extends PlaySpec with OneServerPerSuite with ScalaFutures {

  val timeout = PlayTestDefaults.timeout

  override lazy val port: Int = 19006

  implicit override lazy val app = new GuiceApplicationBuilder().configure(PlayTestDefaults.inMemoryDatabaseConfig()).build()


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
      val reqBody = organisationJson(None, "Foo Bar", "FB", "12345678", "http://www.foo.bar")
//        Json.toJson(
//        Organization(None, "Foo Bar", "FB", "12345678", "http://www.foo.bar", None)
//      )
      val future = wsUrl("/v1/organization").post(reqBody)
      whenReady(future, timeout) { response =>
        val org = Json.parse(response.body).validate[Organization].get
        org.id mustBe Some(2)
        org.fn mustBe "Foo Bar"
      }
    }
    "update organization" in {
      val reqBody = organisationJson(Some(2),"Foo Bar 123", "FB", "12345678", "http://www.foo.bar")
//      val reqBody = Json.toJson(
//        Organization(Some(2), "Foo Bar 123", "FB", "12345678", "http://www.foo.bar", None)
//      )
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



}
