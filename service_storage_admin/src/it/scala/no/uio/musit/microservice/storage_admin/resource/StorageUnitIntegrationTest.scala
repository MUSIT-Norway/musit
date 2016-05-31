package no.uio.musit.microservice.storage_admin.resource

import com.google.gson.JsonObject
import no.uio.musit.microservice.storageAdmin.domain._
import no.uio.musit.microservices.common.PlayTestDefaults
import no.uio.musit.microservices.common.domain.MusitError
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.ExecutionContext.Implicits.global
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import play.api.libs.ws.WS
import no.uio.musit.microservices.common.extensions.PlayExtensions._

/**
  * Created by ellenjo on 5/27/16.
  */
class StorageUnitIntegrationTest extends PlaySpec with OneServerPerSuite with ScalaFutures {
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
    "update storageUnit" in {
      val myJSon ="""{"storageType":"storageunit","storageUnitName":"hylle2"}"""
      println("Skal create/poste hylle2")
      val future = WS.url(s"http://localhost:$port/v1/storageunit").postJsonString(myJSon)

      future.map { response => {

        val storageUnit = Json.parse(response.body).validate[StorageUnit].get
        assert(storageUnit.storageUnitName=="hylle2")
        println(s"Update - ID: ${storageUnit.getId} storageUnitName: ${storageUnit.storageUnitName}")
        val storageJson = Json.parse(response.body).asInstanceOf[JsObject].+("storageUnitName"->JsString("hylle3"))
        println(s"Skal oppdatere hylle2 til hylle3: $storageJson")
        val res = for {
          future2 <- WS.url(s"http://localhost:$port/v1/storageunit/${storageUnit.getId}").put(storageJson)
          updatedObjectResponse <- WS.url(s"http://localhost:$port/v1/storageunit/${storageUnit.getId}").get
          updatedObject = Json.parse(updatedObjectResponse.body).validate[StorageUnit].get

        } yield updatedObject // Json.parse(updatedObjectResponse.body).validate[StorageUnit].get

        whenReady(res, timeout) { updatedObject2 =>
          println(s"hYLLA:${updatedObject2.storageUnitName}")
          assert(updatedObject2.storageUnitName=="hylle5")
        }


        val future2 = WS.url(s"http://localhost:$port/v1/storageunit/${storageUnit.getId}").put(storageJson)
        //val future3 = future2.map(fut2 =>



        whenReady(future2, timeout) { response =>
          /*  val storageUnits = Json.parse(response.body).validate[Seq[StorageUnit]].get
          storageUnits.length mustBe 1*/
        }
      }
      }
    }


    "update room should fail with bad id" in {
      val myJSonRoom ="""{"storageType":"Room","storageUnitName":"ROM1"}"""
      val jsValue = Json.parse(myJSonRoom)
      val future = WS.url(s"http://localhost:$port/v1/storageunit/125254764").put(jsValue /*myJSonRoom*/)
      whenReady(future, timeout) { response =>
        val error = Json.parse(response.body).validate[MusitError].get
        error.message mustBe "Unknown storageUnit with ID: 125254764"
      }
    }


  }
}

