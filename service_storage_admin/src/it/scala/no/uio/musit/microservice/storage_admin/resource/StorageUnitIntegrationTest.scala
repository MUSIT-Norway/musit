package no.uio.musit.microservice.storage_admin.resource

import no.uio.musit.microservice.storageAdmin.domain._
import no.uio.musit.microservices.common.PlayTestDefaults
import no.uio.musit.microservices.common.domain.MusitError
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WS
import no.uio.musit.microservices.common.extensions.PlayExtensions._

/**
  * Created by ellenjo on 5/27/16.
  */
class StorageUnitIntegrationTest extends PlaySpec with OneServerPerSuite  with ScalaFutures {
  val timeout = PlayTestDefaults.timeout
  override lazy val port: Int = 19002
  implicit override lazy val app = new GuiceApplicationBuilder().configure(PlayTestDefaults.inMemoryDatabaseConfig()).build()

  "StorageUnitIntegration " must {
    "postCreate some IDs" in {
      val makeMyJSon ="""{"storageType":"Room","storageUnitName":"ROM1"}"""
      val future = WS.url(s"http://localhost:$port/v1/storageunit").postJsonString(makeMyJSon)
      whenReady(future, timeout) { response =>
        val storageUnit = Json.parse(response.body).validate[StorageUnit].get
        val storageRoom = Json.parse(response.body).validate[StorageRoom].get
        storageUnit.getId mustBe 1
        storageUnit.storageKind mustBe Room
        storageUnit.storageUnitName mustBe "ROM1"
      }
    }
    "postCreate a building" in {
      val makeMyJSon ="""{"id":-1, "storageType":"Building","storageUnitName":"KHM", "links":[]}"""
      val future = WS.url(s"http://localhost:$port/v1/storageunit").postJsonString(makeMyJSon)
      whenReady(future, timeout) { response =>
        val storageUnit = Json.parse(response.body).validate[StorageUnit].get
        val storageBuilding = Json.parse(response.body).validate[StorageBuilding].get
        storageUnit.id mustBe Some(2)
        storageUnit.storageKind mustBe Building
        storageUnit.storageUnitName mustBe "KHM"
      }
    }
    "get by id" in {
      val future = WS.url(s"http://localhost:$port/v1/storageunit/1").get()
      whenReady(future, timeout) { response =>
        val storageUnit = Json.parse(response.body).validate[StorageUnit].get
        storageUnit.getId mustBe 1
      }
    }
    "negative get by id" in {
      val future = WS.url(s"http://localhost:$port/v1/storageunit/9999").get()
      whenReady(future, timeout) { response =>
        val error = Json.parse(response.body).validate[MusitError].get
        error.message mustBe "Did not find storage unit with id: 9999"
      }
    }
    "get all nodes" in {
      val future = WS.url(s"http://localhost:$port/v1/storageunit").get()
      whenReady(future, timeout) { response =>
        val storageUnits = Json.parse(response.body).validate[Seq[StorageUnit]].get
        storageUnits.length mustBe 2
      }
    }
    "update roomName" in {
      val myJSonRoom ="""{"storageType":"Room","storageUnitName":"ROM1"}"""
      val future = WS.url(s"http://localhost:$port/v1/storageunit/1").put(myJSonRoom)
      whenReady(future, timeout) { response =>
      /*  val storageUnits = Json.parse(response.body).validate[Seq[StorageUnit]].get
        storageUnits.length mustBe 1*/
      }
    }

    "update room should fail with bad id" in {
      val myJSonRoom ="""{"storageType":"Room","storageUnitName":"ROM1"}"""
      val future = WS.url(s"http://localhost:$port/v1/storageunit/125254764").put(myJSonRoom)
      whenReady(future, timeout) { response =>
        val error = Json.parse(response.body).validate[MusitError].get
        error.message mustBe "Unknown storageUnit with ID: 125254764"
      }
    }







  }
}

