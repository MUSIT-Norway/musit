/**
 * Created by ellenjo on 4/15/16.
 */

import models.{MuseumIdentifier, ObjectId}
import no.uio.musit.test.MusitSpecWithServerPerSuite
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.http.Status
import play.api.libs.json._

import scala.language.postfixOps

class ObjectAggregationIntegrationSpec extends MusitSpecWithServerPerSuite {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  "ObjectAggregation integration" must {
    "find objects for nodeId that exists" in {
      val nodeId = 3
      val mid = 2
      val response = wsUrl(s"/museum/$mid/node/$nodeId/objects").get().futureValue
      response.status mustBe Status.OK

      val objects = response.json.as[JsArray].value
      objects must not be empty
      val obj = objects.head
      (obj \ "id").as[ObjectId] mustBe ObjectId(1)
      (obj \ "displayName").as[String] mustBe "Øks"
      (obj \ "identifier").as[MuseumIdentifier] mustBe MuseumIdentifier("C666", Some("34"))
    }
    "respond with 404 for nodeId that does not exist" in {
      val nodeId = 99999
      val mid = 2
      val response = wsUrl(s"/museum/$mid/node/$nodeId/objects").get().futureValue
      response.status mustBe Status.NOT_FOUND
      (response.json \ "message").as[String] must endWith(s"$nodeId")
    }
    "respond with 400 if the request URI is missing nodeId " in {
      val nodeId = None
      val mid = 2
      val response = wsUrl(s"/museum/$mid/node/$nodeId/objects").get().futureValue
      response.status mustBe Status.BAD_REQUEST
    }
    "respond with 400 if the museumId is invalid" in {
      val nodeId = 99999
      val mid = 555
      val response = wsUrl(s"/museum/$mid/node/$nodeId/objects").get().futureValue
      response.status mustBe Status.BAD_REQUEST
      (response.json \ "message").as[String] must include(s"$mid")
    }
    "respond with 400 if the museumId is missing from the request URI" in {
      val nodeId = 3
      val mid = None
      val response = wsUrl(s"/museum/$mid/node/$nodeId/objects").get().futureValue
      response.status mustBe Status.BAD_REQUEST
    }
    "respond with 400 if the museumId isn't a valid number" in {
      val nodeId = 3
      val mid = "blæBlæBlæ"
      val response = wsUrl(s"/museum/$mid/node/$nodeId/objects").get().futureValue
      response.status mustBe Status.BAD_REQUEST
    }
  }
}
