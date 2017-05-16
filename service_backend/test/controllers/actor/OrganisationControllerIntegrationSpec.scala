package controllers.actor

import no.uio.musit.security.BearerToken
import no.uio.musit.test.{FakeUsers, MusitSpecWithServerPerSuite}
import play.api.http.Status
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import utils.testdata.ActorJsonGenerator._

import scala.concurrent.Future

class OrganisationControllerIntegrationSpec extends MusitSpecWithServerPerSuite {

  val fakeToken = BearerToken(FakeUsers.testUserToken)

  def postOrganization(json: JsValue): Future[WSResponse] = {
    wsUrl("/organisation").withHeaders(fakeToken.asHeader).post(json)
  }

  def putOrganization(id: Long, json: JsValue): Future[WSResponse] = {
    wsUrl(s"/organisation/$id").withHeaders(fakeToken.asHeader).put(json)
  }

  def deleteOrganization(id: Long): Future[WSResponse] = {
    wsUrl(s"/organisation/$id").withHeaders(fakeToken.asHeader).delete
  }

  def getOrganization(id: Long): Future[WSResponse] = {
    wsUrl(s"/organisation/$id").withHeaders(fakeToken.asHeader).get
  }

  "The OrganizationController" must {
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
      val res = wsUrl("/organisation?museumId=0")
        .withHeaders(fakeToken.asHeader)
        .get()
        .futureValue
      res.status mustBe Status.BAD_REQUEST
    }

    "successfully search for organisation" in {
      val res = wsUrl("/organisation?museumId=0&search=[KHM]")
        .withHeaders(fakeToken.asHeader)
        .get()
        .futureValue
      val orgs = res.json.as[JsArray].value
      orgs.length mustBe 1
      (orgs.head \ "fn")
        .as[String] mustBe "Kulturhistorisk museum - Universitetet i Oslo"
    }

    "successfully create organisation" in {
      val reqBody = orgJson(
        None,
        "Foo Bar",
        "12345678",
        "http://www.foo.bar",
        Some(Seq("FooCode")),
        Some(Seq("storage_facility"))
      )
      val res = postOrganization(reqBody).futureValue
      res.status mustBe Status.CREATED

      (res.json \ "fn").as[String] mustBe "Foo Bar"
      (res.json \ "tel").as[String] mustBe "12345678"
      (res.json \ "web").as[String] mustBe "http://www.foo.bar"
      (res.json \ "synonyms").as[Seq[String]] mustBe Seq("FooCode")
      (res.json \ "serviceTags").as[Seq[String]] mustBe Seq("storage_facility")
    }

    "not create organisation with illegal input" in {
      val response = postOrganization(orgIllegalJson).futureValue
      response.status mustBe Status.BAD_REQUEST
    }

    "successfully update organisation" in {
      val addJson = orgJson(
        None,
        "Foo Barcode",
        "22334455",
        "http://www.foo.barcode.com",
        Some(Seq("FooCode")),
        Some(Seq("storage_facility"))
      ) // scalastyle:ignore
      val res1 = postOrganization(addJson).futureValue
      res1.status mustBe Status.CREATED

      val id1 = (res1.json \ "id").as[Long]
      val updJson = Json.obj(
        "id"          -> id1,
        "fn"          -> "Foo Barcode 123",
        "tel"         -> "12345123",
        "web"         -> "http://www.foo123.bar",
        "synonyms"    -> Seq("FooCode"),
        "serviceTags" -> Seq("storage_facility")
      )

      val res2 = putOrganization(id1, updJson).futureValue
      res2.status mustBe Status.OK
      (res2.json \ "message").as[String] mustBe "1 records were updated!"

      val res3       = getOrganization(id1).futureValue
      val updOrgJson = res3.json.as[JsObject]

      updJson.as[JsObject].fieldSet.diff(updOrgJson.fieldSet) mustBe empty
      addJson.as[JsObject].fieldSet.diff(updOrgJson.fieldSet) must not be empty

      (res3.json \ "id").as[Int] mustBe id1
      (res3.json \ "fn").as[String] mustBe "Foo Barcode 123"
      (res3.json \ "tel").as[String] mustBe "12345123"
      (res3.json \ "web").as[String] mustBe "http://www.foo123.bar"
      (res3.json \ "synonyms").as[Seq[String]] mustBe Seq("FooCode")
      (res3.json \ "serviceTags").as[Seq[String]] mustBe Seq("storage_facility")
    }

    "not update organisation with illegal input" in {
      val addJson = orgJson(
        None,
        "Foo Barcode",
        "22334455",
        "http://www.foo.barcode.com",
        Some(Seq("FooCode")),
        Some(Seq("storage_facility"))
      ) // scalastyle:ignore
      val res1 = postOrganization(addJson).futureValue
      res1.status mustBe Status.CREATED
      val id = (res1.json \ "id").as[Long]

      val res2 = putOrganization(id, orgIllegalJson).futureValue
      res2.status mustBe Status.BAD_REQUEST
    }

    "not update organisation with illegal id" in {
      val js = orgIllegalJson.as[JsObject] ++ Json.obj("id" -> 999999)
      putOrganization(999999, js).futureValue.status mustBe Status.BAD_REQUEST
    }

    "successfully delete organisation" in {
      val crJson = orgJson(
        None,
        "Foo Barcode999",
        "22334499",
        "http://www.foo.barcode999.com",
        Some(Seq("FooCode")),
        Some(Seq("storage_facility"))
      ) // scalastyle:ignore
      val res1 = postOrganization(crJson).futureValue
      res1.status mustBe Status.CREATED
      val id = (res1.json \ "id").as[Long]

      val res2 = deleteOrganization(id).futureValue
      res2.status mustBe Status.OK
      (res2.json \ "message").as[String] mustBe "Deleted 1 record(s)."
    }
  }

}
