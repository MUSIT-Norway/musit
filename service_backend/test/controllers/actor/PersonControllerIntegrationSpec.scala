package controllers.actor

import java.util.UUID

import no.uio.musit.models.ActorId
import no.uio.musit.security.BearerToken
import no.uio.musit.test.{FakeUsers, MusitSpecWithServerPerSuite}
import play.api.http.Status
import play.api.libs.json.{JsArray, JsValue, Json}

class PersonControllerIntegrationSpec extends MusitSpecWithServerPerSuite {

  val fakeToken = BearerToken(FakeUsers.testUserToken)

  val andersAuthId = ActorId(UUID.fromString("12345678-adb2-4b49-bce3-320ddfe6c90f"))
  val andersAppId  = ActorId(UUID.fromString("41ede78c-a6f6-4744-adad-02c25fb1c97c"))
  val kalleAppId   = ActorId(UUID.fromString("5224f873-5fe1-44ec-9aaf-b9313db410c6"))

  "The PersonController" must {

    "fail getting person by id when there is no valid token" in {
      wsUrl(s"/person/${andersAppId.asString}")
        .get()
        .futureValue
        .status mustBe Status.UNAUTHORIZED
    }

    "get by id" in {
      val res = wsUrl(s"/person/${andersAppId.asString}")
        .withHeaders(fakeToken.asHeader)
        .get()
        .futureValue
      res.status mustBe Status.OK
      (res.json \ "id").as[Int] mustBe 1
    }

    "not find a user if the ID doesn't exist" in {
      val res = wsUrl(s"/person/${UUID.randomUUID().toString}")
        .withHeaders(fakeToken.asHeader)
        .get()
        .futureValue
      res.status mustBe Status.NOT_FOUND
    }

    "search on person" in {
      val res = wsUrl("/person?museumId=99&search=[And]")
        .withHeaders(fakeToken.asHeader)
        .get()
        .futureValue
      res.status mustBe Status.OK
      val js = res.json.as[JsArray].value
      js.length mustBe 1
      (js.head \ "fn").as[String] mustBe "And, Arne1"
    }

    "search on person case insensitive" in {
      val res = wsUrl("/person?museumId=99&search=[and]")
        .withHeaders(fakeToken.asHeader)
        .get()
        .futureValue
      res.status mustBe Status.OK
      val js = res.json.as[JsArray].value
      js.length mustBe 1
      (js.head \ "fn").as[String] mustBe "And, Arne1"
    }

    "return bad request when no search criteria is specified" in {
      val res =
        wsUrl("/person?museumId=0").withHeaders(fakeToken.asHeader).get().futureValue
      res.status mustBe Status.BAD_REQUEST
    }

    "get person details from Actor and UserInfo" in {
      val jsStr   = s"""["${andersAuthId.asString}", "${kalleAppId.asString}"]"""
      val reqBody = Json.parse(jsStr)
      val res = wsUrl("/person/details")
        .withHeaders(fakeToken.asHeader)
        .post(reqBody)
        .futureValue
      res.status mustBe Status.OK
      val js = res.json.as[JsArray].value
      js.length mustBe 2
      (js.last \ "fn").as[String] mustBe "Fred Flintstone"
      (js.head \ "fn").as[String] mustBe "Kanin, Kalle1"
    }

    "get person details with extra ids" in {
      val (id1, id2)       = (ActorId.generate(), ActorId.generate())
      val jsStr            = s"""["${id1.asString}", "${kalleAppId.asString}", "${id2.asString}"]"""
      val reqBody: JsValue = Json.parse(jsStr)
      val res = wsUrl("/person/details")
        .withHeaders(fakeToken.asHeader)
        .post(reqBody)
        .futureValue
      res.status mustBe Status.OK
      val js = res.json.as[JsArray].value
      js.length mustBe 1
      (js.head \ "fn").as[String] mustBe "Kanin, Kalle1"
    }

    "not get person details with illegal json" in {
      val reqBody = Json.parse("[12,99999999999999999999999999999999999]")
      val res = wsUrl("/person/details")
        .withHeaders(fakeToken.asHeader)
        .post(reqBody)
        .futureValue
      res.status mustBe Status.BAD_REQUEST
    }
  }
}
