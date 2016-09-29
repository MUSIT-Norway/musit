/**
 * Created by ellenjo on 4/15/16.
 */

import models.{MuseumIdentifier, ObjectAggregation, ObjectId}
import no.uio.musit.test.MusitSpecWithServerPerSuite
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.libs.json._

import scala.language.postfixOps

class ObjectAggregationIntegrationSpec extends MusitSpecWithServerPerSuite {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  "ObjectAggregation integration" must {
    "get by nodeId that exists" in {
      val nodeId = 3
      val mid = 2
      val response = wsUrl(s"/museum/$mid/node/$nodeId/objects").get().futureValue
      val objects = Json.parse(response.body).validate[Seq[ObjectAggregation]].get
      val obj = objects.head
      obj.id mustBe ObjectId(1)
      obj.displayName mustBe Some("Øks")
      obj.identifier mustBe MuseumIdentifier("C666", Some("34"))
    }
    "get by nodeId that does not exist" in {
      val nodeId = 99999
      val mid = 2
      val response = wsUrl(s"/museum/$mid/node/$nodeId/objects").get().futureValue
      response.status mustBe 404
      response.body must endWith (s"$nodeId")
    }
    "get by nodeId where nodeId is None " in {
      val nodeId = None
      val mid = 2
      val response = wsUrl(s"/museum/$mid/node/$nodeId/objects").get().futureValue
      response.status mustBe 400
      response.body must include (s"Cannot parse parameter nodeId as Long: For input string")
    }
    "get by nodeId with wrong museum and wrong nodeId" in {
      val nodeId = 99999
      val mid = 555
      val response = wsUrl(s"/museum/$mid/node/$nodeId/objects").get().futureValue
      response.status mustBe 400
      response.body must include (s"$mid")
    }
    "get by nodeId with no museumId and nodeId that exists" in {
      val nodeId = 3
      val mid = None
      val response = wsUrl(s"/museum/$mid/node/$nodeId/objects").get().futureValue
    response.status mustBe 400
    response.body must include (s"Cannot parse parameter mid as Int: For input string")
  }
  }
}

