package controllers.actor

import no.uio.musit.security.BearerToken
import no.uio.musit.test.{FakeUsers, MusitSpecWithServerPerSuite}
import play.api.http.Status
import play.api.libs.json.{JsArray, JsObject, JsValue, Json}
import play.api.libs.ws.WSResponse
import utils.testdata.ActorJsonGenerator._

import scala.concurrent.Future

class OrganisationAddressControllerIntegrationSpec extends MusitSpecWithServerPerSuite {

  val fakeToken = BearerToken(FakeUsers.testUserToken)

  def postOrganizationAddress(orgId: Int, json: JsValue): Future[WSResponse] = {
    wsUrl(s"/organisation/$orgId/address").withHttpHeaders(fakeToken.asHeader).post(json)
  }

  "The OrganisationAddressController" must {

    "get by id" in {
      val res = wsUrl("/organisation/355/address/1")
        .withHttpHeaders(fakeToken.asHeader)
        .get()
        .futureValue

      res.status mustBe Status.OK
      val addr = res.json
      (addr \ "id").as[Int] mustBe 1
      (addr \ "organisationId").as[Int] mustBe 355
    }
    "negative get by id" in {
      val res = wsUrl("/organisation/1/address/999")
        .withHttpHeaders(fakeToken.asHeader)
        .get()
        .futureValue
      (res.json \ "message").as[String] mustBe "Did not find object with id: 999"
    }
    "get all addresses for an organisation" in {
      val res = wsUrl("/organisation/355/address")
        .withHttpHeaders(fakeToken.asHeader)
        .get()
        .futureValue
      res.json.as[JsArray].value.length mustBe 1
    }
    "create address" in {
      val reqBody = orgAddressJson
      val res     = postOrganizationAddress(1, reqBody).futureValue
      res.status mustBe Status.CREATED

      (res.json \ "id").asOpt[Int] must not be None
      (res.json \ "organisationId").as[Int] mustBe 1
      (res.json \ "streetAddress").as[String] mustBe "Foo street 2"
      (res.json \ "streetAddress2").as[String] mustBe "Foo street 3"
    }

    "not create organisationAddress with illegal input" in {
      val res = postOrganizationAddress(1, orgAddressIllegalJson).futureValue
      res.status mustBe Status.BAD_REQUEST
    }

    "update address" in {
      val reqBody = orgAddressJson.as[JsObject] ++ Json.obj(
        "id"             -> 2,
        "organisationId" -> 356,
        "streetAddress"  -> "Foo street 4",
        "streetAddress2" -> "Foo street 5",
        "postalCode"     -> "0001",
        "countryName"    -> "Norway"
      )

      val res = wsUrl("/organisation/356/address/2")
        .withHttpHeaders(fakeToken.asHeader)
        .put(reqBody)
        .futureValue
      (res.json \ "message").as[String] mustBe "Record was updated!"
    }

    "not update of address with illegal id" in {
      val reqBody = orgAddressJson.as[JsObject] ++ Json.obj("id" -> 999)
      val res = wsUrl("/organisation/1/address/999")
        .withHttpHeaders(fakeToken.asHeader)
        .put(reqBody)
        .futureValue
      res.status mustBe Status.OK
      (res.json \ "message").as[String] mustBe "No records were updated!"
    }

    "not update address with illegal json" in {
      val reqBody = Json.obj(
        "id"             -> 2,
        "organisationId" -> 356,
        "stretAddress"   -> "Foo street 3",
        "stretAddress2"  -> "Foo street 4",
        "postalCode"     -> "0001",
        "countryName"    -> "Norway"
      )
      val res = wsUrl("/organisation/356/address/2")
        .withHttpHeaders(fakeToken.asHeader)
        .put(reqBody)
        .futureValue
      res.status mustBe Status.BAD_REQUEST
    }

    "delete address" in {
      val res = wsUrl("/organisation/357/address/3")
        .withHttpHeaders(fakeToken.asHeader)
        .delete()
        .futureValue
      (res.json \ "message").as[String] mustBe "Deleted 1 record(s)."
    }
  }

}
