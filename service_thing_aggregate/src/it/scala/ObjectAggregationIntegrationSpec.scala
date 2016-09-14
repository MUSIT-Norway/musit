/**
  * Created by ellenjo on 4/15/16.
  */

import models.{MuseumIdentifier, ObjectAggregation, ObjectId}
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json._
import testHelpers.TestConfigs
import testHelpers.TestConfigs.WaitLonger

import scala.concurrent.duration._
import scala.language.postfixOps

class ObjectAggregationIntegrationSpec extends PlaySpec with OneServerPerSuite with ScalaFutures with WaitLonger {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  implicit override lazy val app = new GuiceApplicationBuilder().configure(TestConfigs.inMemoryDatabaseConfig()).build()

  override lazy val port: Int = 19010

  "ObjectAggregation integration" must {
    "get by nodeId that exists" in {
      val nodeId = 3
      val response = wsUrl(s"/node/$nodeId/objects").get().futureValue(Timeout(30 seconds))
      println(response.body)
      val objects = Json.parse(response.body).validate[Seq[ObjectAggregation]].get
      val obj = objects.head
      obj.id mustBe ObjectId(1)
      obj.displayName mustBe Some("Øks")
      obj.identifier mustBe MuseumIdentifier("C666", Some("34"))
    }
    "get by nodeId that does not exist" in {
      val nodeId = 99999
      val response = wsUrl(s"/node/$nodeId/objects").get().futureValue(Timeout(30 seconds))
      response.status mustBe 404
      response.body mustBe s"Did not find node with nodeId $nodeId"
    }
  }
}

