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

  def deleteOrganization(id: Long): Future[WSResponse] = {
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
    "successfully get Organization by id" in {
      val org = getOrganization(1).futureValue
      org.id mustBe Some(1)
    }

    "not find Organization with invalid id " in {
      val response = getOrganizationRsp(9999).futureValue
      response.status mustBe Status.NOT_FOUND
    }

    "successfully get root" in {
      val future = wsUrl("/v1/organization").get()
      val response = future.futureValue
      val orgs = Json.parse(response.body).validate[Seq[Organization]].get
      orgs.seq.head.id.get mustBe 1
      assert(orgs.length >= 1)
    }

    "successfully search for organization" in {
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
      org.fn mustBe "Foo Bar"
      org.nickname mustBe "FB"
      org.tel mustBe "12345678"
      org.web mustBe "http://www.foo.bar"
    }

    "not create organization with illegal input" in {
      val reqBody = organisationIllegalJson
      val response = postOrganization(reqBody).futureValue
      response.status mustBe Status.BAD_REQUEST
    }

    "successfully update organization" in {
      val crJson = organisationJson(None, "Foo Barcode", "FB", "22334455", "http://www.foo.barcode.com")
      val crResponse = postOrganization(crJson).futureValue
      crResponse.status mustBe Status.CREATED
      val crOrg = Json.parse(crResponse.body).validate[Organization].get

      val updJson = organisationJson(Some(crOrg.id.get), "Foo Barcode 123", "FB 123", "12345123", "http://www.foo123.bar")
      val response = putOrganization(crOrg.id.get, updJson).futureValue
      response.status mustBe Status.OK

      val updOrg = getOrganization(crOrg.id.get).futureValue
      val message = Json.parse(response.body).validate[MusitStatusMessage].get
      message.message mustBe "Record was updated!"

      val updOrgJson = Json.toJson(updOrg).as[JsObject]

      updJson.as[JsObject].fieldSet.diff(updOrgJson.fieldSet).size mustBe 0
      assert(crJson.as[JsObject].fieldSet.diff(updOrgJson.fieldSet).size > 0)

      updOrg.id mustBe Some(crOrg.id.get)
      updOrg.fn mustBe "Foo Barcode 123"
      updOrg.nickname mustBe "FB 123"
      updOrg.tel mustBe "12345123"
      updOrg.web mustBe "http://www.foo123.bar"
    }

    "not update organization with illegal input" in {
      val crJson = organisationJson(None, "Foo Barcode", "FB", "22334455", "http://www.foo.barcode.com")
      val crResponse = postOrganization(crJson).futureValue
      crResponse.status mustBe Status.CREATED
      val crOrg = Json.parse(crResponse.body).validate[Organization].get

      val updJson = organisationIllegalJson
      val response = putOrganization(crOrg.id.get, updJson).futureValue
      response.status mustBe Status.BAD_REQUEST
    }

    "not update organization with illegal id" in {
      val updJson = organisationIllegalJson.as[JsObject] + ("id" -> Json.toJson(999999))
      val response = putOrganization(999999, updJson).futureValue
      response.status mustBe Status.BAD_REQUEST
    }

    "successfully delete organization" in {
      val crJson = organisationJson(None, "Foo Barcode999", "FB", "22334499", "http://www.foo.barcode999.com")
      val crResponse = postOrganization(crJson).futureValue
      crResponse.status mustBe Status.CREATED
      val createdOrg = Json.parse(crResponse.body).validate[Organization].get
      val createdOrgId = createdOrg.id.get

      val response = deleteOrganization(createdOrgId).futureValue
      response.status mustBe Status.OK
      val msm = Json.parse(response.body).validate[MusitStatusMessage].get
      msm.message mustBe "Deleted 1 record(s)."
    }
  }

}
