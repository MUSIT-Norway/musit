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
import play.api.libs.ws.{WS, WSResponse}
import no.uio.musit.microservices.common.extensions.PlayExtensions._

import scala.concurrent.Future


/**
  * Created by ellenjo on 5/27/16.
  */
class StorageUnitIntegrationTest extends PlaySpec with OneServerPerSuite with ScalaFutures {
  val timeout = PlayTestDefaults.timeout
  override lazy val port: Int = 19002
  implicit override lazy val app = new GuiceApplicationBuilder().configure(PlayTestDefaults.inMemoryDatabaseConfig()).build()

  def futureSucceeded[A](fut: Future[A]) = {
    val res = fut.futureValue
    assert(fut.isCompleted)

    val resb = fut.value.get.isSuccess
    println(s"futureSucceeded: $resb")
    resb

  }

  def createStorageUnit(json: String) = {
    WS.url(s"http://localhost:$port/v1/storageunit").postJsonString(json)
  }

  def updateStorageUnit(id: Long, json: String) = {
    WS.url(s"http://localhost:$port/v1/storageunit/${id}").putJsonString(json)
  }

  def getStorageUnit(id: Long) = WS.url(s"http://localhost:$port/v1/storageunit/${id}").get

  def getRoomAsObject(id: Long): Future[StorageRoom] = {
    for {
      resp <- getStorageUnit(id)
      room = Json.parse(resp.body).validate[StorageRoom].get
    } yield room
  }

  def getStorageUnitAsObject(id: Long): Future[StorageUnit] = {
    for {
      resp <- getStorageUnit(id)
      stUnit = Json.parse(resp.body).validate[StorageUnit].get
    } yield stUnit
  }

  "StorageUnitIntegration " must {
    "postCreate some IDs" in {
      val makeMyJSon ="""{"storageType":"Room","storageUnitName":"UkjentRom"}"""
      val future = WS.url(s"http://localhost:$port/v1/storageunit").postJsonString(makeMyJSon)
      whenReady(future, timeout) { response =>
        val storageUnit = Json.parse(response.body).validate[StorageUnit].get
        val storageRoom = Json.parse(response.body).validate[StorageRoom].get
        storageUnit.getId mustBe 1
        storageUnit.storageKind mustBe Room
        storageUnit.storageUnitName mustBe "UkjentRom"
      }
      assert(futureSucceeded(future))

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
      assert(futureSucceeded(future))
      //future.futureValue
    }

    "get by id" in {
      val future = WS.url(s"http://localhost:$port/v1/storageunit/1").get()
      whenReady(future, timeout) { response =>
        val storageUnit = Json.parse(response.body).validate[StorageUnit].get
        storageUnit.getId mustBe 1
      }
      assert(futureSucceeded(future))
//      future.futureValue
    }
    "negative get by id" in {
      val future = WS.url(s"http://localhost:$port/v1/storageunit/9999").get()
      whenReady(future, timeout) { response =>
        val error = Json.parse(response.body).validate[MusitError].get
        error.message mustBe "Did not find storage unit with id: 9999"
      }
      future.futureValue
    }

    "get all nodes" in {
      val future = WS.url(s"http://localhost:$port/v1/storageunit").get()
      whenReady(future, timeout) { response =>
        val storageUnits = Json.parse(response.body).validate[Seq[StorageUnit]].get
        storageUnits.length mustBe 2
      }
      future.futureValue
    }
    "update roomName" in {
      val myJSonRoom ="""{"storageType":"Room","storageUnitName":"ROM1"}"""

      val future = for {
        oppdat <- updateStorageUnit(1, myJSonRoom)
        stUnit <- getStorageUnitAsObject(1)
      } yield stUnit


      whenReady(future, timeout) { stUnit =>

        assert(stUnit.storageUnitName=="ROM1")
      }
      //future.futureValue mustBe 1
      assert(futureSucceeded(future))
    }


    "update storageUnit" in {
      val myJSon ="""{"storageType":"storageunit","storageUnitName":"hylle2"}"""
      println("Skal create/poste hylle2")
      val future = createStorageUnit(myJSon)

      future.map {
        response => {

          val storageUnit = Json.parse(response.body).validate[StorageUnit].get
          storageUnit.storageUnitName mustBe "hylle2"
          println(s"Update - ID: ${storageUnit.getId} storageUnitName: ${storageUnit.storageUnitName}")
          val storageJson = Json.parse(response.body).asInstanceOf[JsObject].+("storageUnitName" -> JsString("hylle3"))
          println(s"Skal oppdatere hylle2 til hylle3: $storageJson")
          val res = for {
            future2 <- updateStorageUnit(storageUnit.getId, storageJson.toString())
            updatedObjectResponse <- getStorageUnit(storageUnit.getId)
            updatedObject = Json.parse(updatedObjectResponse.body).validate[StorageUnit].get

          } yield updatedObject

          whenReady(res, timeout) { updatedObject2 =>
            println(s"hYLLA!:${updatedObject2.storageUnitName}")

            //En god del av nedenstående bør feile, men kan ikke se at dette skjer???!!! Men vi ser at println på linja over skjer...??!!!
            updatedObject2.storageUnitName mustBe Some("nei!")
            updatedObject2.storageUnitName mustBe "nei!"
            updatedObject2.storageUnitName must === ("nei!")
            updatedObject2.storageUnitName must not be "nei!"
            assert(updatedObject2.storageUnitName=="nei!")
            assert(false)
          }


          val future2 = WS.url(s"http://localhost:$port/v1/storageunit/${storageUnit.getId}").put(storageJson)
          //val future3 = future2.map(fut2 =>


          whenReady(future2, timeout) { response =>
            /*  val storageUnits = Json.parse(response.body).validate[Seq[StorageUnit]].get
            storageUnits.length mustBe 1*/
          }
          assert(futureSucceeded(res))
          assert(futureSucceeded(future2))
          future2.futureValue mustBe a [WSResponse]
          res.futureValue mustBe a [StorageUnit]
          res.futureValue mustBe be (null)
        }
      }
      assert(futureSucceeded(future))
    }



    "update storageRoom" in {
      val myJSon ="""{"storageType":"room","storageUnitName":"Rom1", "sikringSkallsikring": "0"}"""
      val future = createStorageUnit(myJSon)

      val roomFut = future.map {
        response => {

          val storageUnit = Json.parse(response.body).validate[StorageUnit].get
          val storageRoom = Json.parse(response.body).validate[StorageRoom].get
          storageRoom.sikringSkallsikring mustBe Some("0")
          val id = storageUnit.getId
          println(s"ID: $id Skallsikring før oppdatering: ${storageRoom.sikringSkallsikring}")
          storageUnit.storageUnitName mustBe "Rom1"

          val json2 = """{"storageType":"storageroom","storageUnitName":"RomNyttNavn", "sikringSkallsikring": "1"}"""
          val res = for {
            _ <- updateStorageUnit(id, json2)
            room <- getRoomAsObject(id)
            stUnit <- getStorageUnitAsObject(id)
          } yield (stUnit, room)
          println("inni storageRoom")
          whenReady(res, timeout) { pair =>
              val room = pair._2
            val stUnit = pair._1
            stUnit.storageUnitName mustBe "RomNyttNavn"
            println(s"Rom, skallsikring etter oppdatering: ${room.sikringSkallsikring}  Rommet som helhet: $room")
            println(s"stUnit etter oppdat: $stUnit")
            room.sikringSkallsikring mustBe Some("1")



          }
        }

      }
      future.futureValue
      roomFut.futureValue
    }



    "update room should fail with bad id" in {
      val myJSonRoom ="""{"storageType":"Room","storageUnitName":"ROM1"}"""
      val jsValue = Json.parse(myJSonRoom)
      val future = WS.url(s"http://localhost:$port/v1/storageunit/125254764").put(jsValue /*myJSonRoom*/)
      whenReady(future, timeout) { response =>
        val error = Json.parse(response.body).validate[MusitError].get
        error.message mustBe "Unknown storageUnit with ID: 125254764"
      }
      future.futureValue
    }
  }
}

