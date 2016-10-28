package no.uio.musit.microservice.actor.resource

import no.uio.musit.test.MusitSpecWithServerPerSuite
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.http.Status
import play.api.libs.json.{JsArray, JsValue, Json}

class LegacyPersonIntegrationSpec extends MusitSpecWithServerPerSuite {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  "LegacyPersonIntegration " must {
    "get by id" in {
      val res = wsUrl("/v1/person/1").get().futureValue
      res.status mustBe Status.OK
      (res.json \ "id").as[Int] mustBe 1
    }

    "negative get by id" in {
      val res = wsUrl("/v1/person/9999").get().futureValue
      res.status mustBe Status.NOT_FOUND
      (res.json \ "message").as[String] mustBe "Did not find object with id: 9999"
    }

    "search on person" in {
      val res = wsUrl("/v1/person?museumId=0&search=[And]").get().futureValue
      res.status mustBe Status.OK
      val js = res.json.as[JsArray].value
      js.length mustBe 1
      (js.head \ "fn").as[String] mustBe "And, Arne1"
    }

    "search on person case insensitive" in {
      val res = wsUrl("/v1/person?museumId=0&search=[and]").get().futureValue
      res.status mustBe Status.OK
      val js = res.json.as[JsArray].value
      js.length mustBe 1
      (js.head \ "fn").as[String] mustBe "And, Arne1"
    }

    "return bad request when no search criteria is specified" in {
      val res = wsUrl("/v1/person?museumId=0").get().futureValue
      res.status mustBe Status.BAD_REQUEST
    }

    "get person details" in {
      val reqBody: JsValue = Json.parse("[1,2]")
      val res = wsUrl("/v1/person/details").post(reqBody).futureValue
      res.status mustBe Status.OK
      val js = res.json.as[JsArray].value
      js.length mustBe 2
      (js.head \ "fn").as[String] mustBe "And, Arne1"
      (js.last \ "fn").as[String] mustBe "Kanin, Kalle1"
    }

    "get person details with extra ids" in {
      val reqBody: JsValue = Json.parse("[1234567,2,9999]")
      val res = wsUrl("/v1/person/details").post(reqBody).futureValue
      res.status mustBe Status.OK
      val js = res.json.as[JsArray].value
      js.length mustBe 1
      (js.head \ "fn").as[String] mustBe "Kanin, Kalle1"
    }

    "not get person details with illegal json" in {
      val reqBody = Json.parse("[12,99999999999999999999999999999999999]")
      val res = wsUrl("/v1/person/details").post(reqBody).futureValue
      res.status mustBe Status.BAD_REQUEST
    }
  }
}
