package no.uio.musit.microservice.actor.resource

import no.uio.musit.microservice.actor.domain.Organization
import no.uio.musit.microservices.common.PlayTestDefaults
import no.uio.musit.microservices.common.domain.{MusitError, MusitStatusMessage}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, JsValue, Json}
import no.uio.musit.microservice.actor.testdata.ActorJsonGenerator._
import play.api.http.Status
import play.api.libs.ws.WSResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by sveigl on 20.09.16.
  */
class OrganizationIntegrationSpec extends PlaySpec with OneServerPerSuite with ScalaFutures {

  val timeout = PlayTestDefaults.timeout

  override lazy val port: Int = 19009

  implicit override lazy val app = new GuiceApplicationBuilder().configure(PlayTestDefaults.inMemoryDatabaseConfig()).build()

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  def postOrganization(json: JsValue): Future[WSResponse] = {
    wsUrl("/v1/organization").post(json)
  }

  def putOrganization(id: Long, json: JsValue): Future[WSResponse] = {
    wsUrl(s"/v1/organization/$id").put(json)
  }

  def deleteOrganization(id: Long) :Future[WSResponse] = {
    wsUrl(s"/v1/organization/$id").delete
  }

  def getOrganizationRsp(id: Long): Future[WSResponse] = {
    wsUrl(s"/v1/organization/$id").get
  }

  def getOrganization(id: Long): Future[Organization] = {
    for {
      resp <- getOrganizationRsp(id)
      org = Json.parse(resp.body).validate[Organization].get
    } yield org
  }

  "OrganizationIntegration " must {
    "get by id" in {
      val org = getOrganization(1).futureValue
      org.id mustBe Some(1)
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

    "successfully create organization" in {
      val reqBody = organisationJson(None, "Foo Bar", "FB", "12345678", "http://www.foo.bar")
      val response = postOrganization(reqBody).futureValue
      response.status mustBe Status.CREATED

      val org = Json.parse(response.body).validate[Organization].get
      org.id mustBe Some(2)
      org.fn mustBe "Foo Bar"
      org.nickname mustBe "FB"
      org.tel mustBe "12345678"
      org.web mustBe "http://www.foo.bar"
    }

    "successfully update organization" in {
      val reqBody1 = organisationJson(None, "Foo Barcode", "FB", "22334455", "http://www.foo.barcode.com")
      val response1 = postOrganization(reqBody1).futureValue
      response1.status mustBe Status.CREATED
      val org1 = Json.parse(response1.body).validate[Organization].get

      val reqBody = organisationJson(Some(org1.id.get),"Foo Barcode 123", "FB 123", "12345123", "http://www.foo123.bar")
      val response = putOrganization(org1.id.get, reqBody).futureValue
      response.status mustBe Status.OK

      val org = getOrganization(org1.id.get).futureValue
      val message = Json.parse(response.body).validate[MusitStatusMessage].get
      message.message mustBe "Record was updated!"

      val updatedOrgJson = Json.toJson(org).as[JsObject]

      reqBody.as[JsObject].fieldSet.diff(updatedOrgJson.fieldSet).size mustBe 0
      assert(reqBody1.as[JsObject].fieldSet.diff(updatedOrgJson.fieldSet).size > 0)

      org.id mustBe Some(org1.id.get)
      org.fn mustBe "Foo Barcode 123"
      org.nickname mustBe "FB 123"
      org.tel mustBe "12345123"
      org.web mustBe "http://www.foo123.bar"

    }

    "delete organization" in {
      val reqBody1 = organisationJson(None, "Foo Barcode999", "FB", "22334499", "http://www.foo.barcode999.com")
      val response1 = postOrganization(reqBody1).futureValue
      response1.status mustBe Status.CREATED
      val created = Json.parse(response1.body).validate[Organization].get
      val createdOrgId = created.id.get

      val response = deleteOrganization(createdOrgId).futureValue
      response.status mustBe Status.OK
      val msm = Json.parse(response.body).validate[MusitStatusMessage].get
      msm.message mustBe "Deleted 1 record(s)."
    }
  }



}
