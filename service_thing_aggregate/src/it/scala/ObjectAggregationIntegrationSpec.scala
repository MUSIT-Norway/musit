/**
  * Created by ellenjo on 4/15/16.
  */

import models.{NodeId, ObjectAggregation, ObjectId}
import no.uio.musit.microservices.common.PlayTestDefaults
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json._

import scala.concurrent.duration._
import scala.language.postfixOps

class ObjectAggregationIntegrationSpec extends PlaySpec with OneServerPerSuite with ScalaFutures {

  val timeout = PlayTestDefaults.timeout
  implicit override lazy val app = new GuiceApplicationBuilder().configure(PlayTestDefaults.inMemoryDatabaseConfig()).build()

  override lazy val port: Int = 19010

  "ObjectAggregation integration" must {
    "get by nodeId" in {
      val nodeId = 1
      val response = wsUrl(s"/node/$nodeId/objects").get().futureValue(Timeout(30 seconds))
      val objects = Json.parse(response.body).validate[Seq[ObjectAggregation]].get
      objects.length mustBe 1
      val obj = objects(0)
      obj.id mustBe ObjectId(1)
      obj.name mustBe "Test"
      obj.nodeId mustBe NodeId(1)
    }
  }
}

