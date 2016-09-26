/**
 * Created by ellenjo on 4/15/16.
 */

import models.{MuseumIdentifier, ObjectAggregation, ObjectId}
import no.uio.musit.test.{MusitSpec, TestConfigs}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.OneServerPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json._

import scala.language.postfixOps

class ObjectAggregationIntegrationSpec extends MusitSpec
  with OneServerPerSuite
  with TestConfigs {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  implicit override lazy val app = {
    new GuiceApplicationBuilder()
      .configure(slickWithInMemoryH2(dbName = "obj-agr-it-spec"))
      .build()
  }

  override lazy val port: Int = 19010

  "ObjectAggregation integration" must {
    "get by nodeId that exists" in {
      val nodeId = 3
      val response = wsUrl(s"/node/$nodeId/objects").get().futureValue
      val objects = Json.parse(response.body).validate[Seq[ObjectAggregation]].get
      val obj = objects.head
      obj.id mustBe ObjectId(1)
      obj.displayName mustBe Some("Øks")
      obj.identifier mustBe MuseumIdentifier("C666", Some("34"))
    }
    "get by nodeId that does not exist" in {
      val nodeId = 99999
      val response = wsUrl(s"/node/$nodeId/objects").get().futureValue
      response.status mustBe 404
      response.body mustBe s"Did not find node with nodeId $nodeId"
    }
  }
}

