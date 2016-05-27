package no.uio.musit.microservice.storage_admin.resource

import no.uio.musit.microservice.storageAdmin.domain.{Room, StorageRoom, StorageUnit}
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
      val makeMyJSon ="""{"id":-1, "storageType":"Room","storageUnitName":"ROM1", "links":[]}"""  //
       /* Json.toJson(StorageUnit(-1, "Room", "ROM1", Some(10), Some("1"), None, Some(20),
        Some("skriv"), Some("les"), Seq.empty))*/
      val future =WS.url(s"http://localhost:$port/v1/storageunit").postJsonString(makeMyJSon)
      whenReady(future, timeout) { response =>
        val storageUnit = Json.parse(response.body).validate[StorageUnit].get
        val storageRoom = Json.parse(response.body).validate[StorageRoom].get
        storageUnit.id mustBe 1
        storageUnit.storageKind mustBe Room
        storageUnit.storageUnitName mustBe "ROM1"
      }
    }
    "get by id" in {
      val future = WS.url(s"http://localhost:$port/v1/storageunit/1").get()
      whenReady(future, timeout) { response =>
        val storageUnit = Json.parse(response.body).validate[StorageUnit].get
        storageUnit.id mustBe 1
      }
    }
    "negative get by id" in {
      val future = WS.url(s"http://localhost:$port/v1/storageunit/9999").get()
      whenReady(future, timeout) { response =>
        val error = Json.parse(response.body).validate[MusitError].get
        error.message mustBe "Did not find storage unit with id: 9999"
      }
    }
    /*"search on StorageUnit" in {
      val future = WS.url(s"http://localhost:$port/v1/storageunit?search=[And]").get()
      whenReady(future, timeout) { response =>
        val stUnits = Json.parse(response.body).validate[Seq[StorageUnit]].get
        stUnits.length mustBe 1
        stUnits.head.fn mustBe "And, Arne1"
      }
    }*/
  }
}

