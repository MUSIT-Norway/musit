package no.uio.musit.microservice.actor.resource

import no.uio.musit.microservice.actor.testdata.ActorJsonGenerator._
import no.uio.musit.test.MusitSpecWithServerPerSuite
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.http.Status
import play.api.libs.json._
import play.api.libs.ws.WSResponse

import scala.concurrent.Future

class OrganizationIntegrationSpec extends MusitSpecWithServerPerSuite {

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

  def getOrganization(id: Long): Future[WSResponse] = {
    wsUrl(s"/v1/organization/$id").get
  }

  "OrganizationIntegration " must {
    "successfully get Organization by id" in {
      val res = getOrganization(1).futureValue
      res.status mustBe Status.OK
      (res.json \ "id").as[Int] mustBe 1
    }

    "not find Organization with invalid id " in {
      val res = getOrganization(9999).futureValue
      res.status mustBe Status.NOT_FOUND
    }

    "return bad request when no search criteria is specified" in {
      val res = wsUrl("/v1/organization?museumId=0").get().futureValue
      res.status mustBe Status.BAD_REQUEST
    }

    "successfully search for organization" in {
      val res = wsUrl("/v1/organization?museumId=0&search=[KHM]").get().futureValue
      val orgs = res.json.as[JsArray].value
      orgs.length mustBe 1
      (orgs.head \ "fn").as[String] mustBe "Kulturhistorisk museum - Universitetet i Oslo"
    }

    "successfully create organization" in {
      val reqBody = orgJson(None, "Foo Bar", "FB", "12345678", "http://www.foo.bar")
      val res = postOrganization(reqBody).futureValue
      res.status mustBe Status.CREATED

      (res.json \ "fn").as[String] mustBe "Foo Bar"
      (res.json \ "nickname").as[String] mustBe "FB"
      (res.json \ "tel").as[String] mustBe "12345678"
      (res.json \ "web").as[String] mustBe "http://www.foo.bar"
    }

    "not create organization with illegal input" in {
      val response = postOrganization(orgIllegalJson).futureValue
      response.status mustBe Status.BAD_REQUEST
    }

    "successfully update organization" in {
      val addJson = orgJson(None, "Foo Barcode", "FB", "22334455", "http://www.foo.barcode.com")
      val res1 = postOrganization(addJson).futureValue
      res1.status mustBe Status.CREATED

      val id1 = (res1.json \ "id").as[Long]
      val updJson = Json.obj(
        "id" -> id1,
        "fn" -> "Foo Barcode 123",
        "nickname" -> "FB 123",
        "tel" -> "12345123",
        "web" -> "http://www.foo123.bar"
      )

      val res2 = putOrganization(id1, updJson).futureValue
      res2.status mustBe Status.OK
      (res2.json \ "message").as[String] mustBe "Record was updated!"

      val res3 = getOrganization(id1).futureValue
      val updOrgJson = res3.json.as[JsObject]

      updJson.as[JsObject].fieldSet.diff(updOrgJson.fieldSet) mustBe empty
      addJson.as[JsObject].fieldSet.diff(updOrgJson.fieldSet) must not be empty

      (res3.json \ "id").as[Int] mustBe id1
      (res3.json \ "fn").as[String] mustBe "Foo Barcode 123"
      (res3.json \ "nickname").as[String] mustBe "FB 123"
      (res3.json \ "tel").as[String] mustBe "12345123"
      (res3.json \ "web").as[String] mustBe "http://www.foo123.bar"
    }

    "not update organization with illegal input" in {
      val addJson = orgJson(None, "Foo Barcode", "FB", "22334455", "http://www.foo.barcode.com")
      val res1 = postOrganization(addJson).futureValue
      res1.status mustBe Status.CREATED
      val id = (res1.json \ "id").as[Long]

      val res2 = putOrganization(id, orgIllegalJson).futureValue
      res2.status mustBe Status.BAD_REQUEST
    }

    "not update organization with illegal id" in {
      val js = orgIllegalJson.as[JsObject] ++ Json.obj("id" -> 999999)
      putOrganization(999999, js).futureValue.status mustBe Status.BAD_REQUEST
    }

    "successfully delete organization" in {
      val crJson = orgJson(None, "Foo Barcode999", "FB", "22334499", "http://www.foo.barcode999.com")
      val res1 = postOrganization(crJson).futureValue
      res1.status mustBe Status.CREATED
      val id = (res1.json \ "id").as[Long]

      val res2 = deleteOrganization(id).futureValue
      res2.status mustBe Status.OK
      (res2.json \ "message").as[String] mustBe "Deleted 1 record(s)."
    }
  }

}
