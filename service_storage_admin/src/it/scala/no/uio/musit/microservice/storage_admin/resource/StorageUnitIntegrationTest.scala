package no.uio.musit.microservice.storage_admin.resource

import no.uio.musit.microservice.storageAdmin.dao.StorageUnitDao
import no.uio.musit.microservice.storageAdmin.domain._
import no.uio.musit.microservices.common.PlayTestDefaults
import no.uio.musit.microservices.common.PlayTestDefaults._
import no.uio.musit.microservices.common.domain.MusitError
import no.uio.musit.microservices.common.extensions.PlayExtensions._
import no.uio.musit.microservices.common.utils.Misc._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsNumber, JsObject, JsString, Json}
import play.api.libs.ws.WS

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by ellenjo on 5/27/16.
  */
class StorageUnitIntegrationTest extends PlaySpec with OneServerPerSuite with ScalaFutures {
  //val timeout = PlayTestDefaults.timeout
  override lazy val port: Int = 19002
  implicit override lazy val app = new GuiceApplicationBuilder().configure(PlayTestDefaults.inMemoryDatabaseConfig()).build()


  def unknownStorageUnitMsg(id: Long) = StorageUnitDao.unknownStorageUnitMsg(id)

  def createStorageUnit(json: String) = {
    WS.url(s"http://localhost:$port/v1/storageunit").postJsonString(json)
  }

  def updateStorageUnit(id: Long, json: String) = {
    WS.url(s"http://localhost:$port/v1/storageunit/$id").putJsonString(json)
  }

  def deleteStorageUnit(id: Long) = {
    WS.url(s"http://localhost:$port/v1/storageunit/$id").delete
  }

  def getStorageUnit(id: Long) = WS.url(s"http://localhost:$port/v1/storageunit/$id").get

  def getRoomAsObject(id: Long): Future[Room] = {
    for {
      resp <- getStorageUnit(id)
      room = Json.parse(resp.body).validate[Storage].get.asInstanceOf[Room]
    } yield room
  }

  def getBuildingAsObject(id: Long): Future[Building] = {
    for {
      resp <- getStorageUnit(id)
      room = Json.parse(resp.body).validate[Storage].get.asInstanceOf[Building]
    } yield room
  }


  def getStorageUnitAsObject(id: Long): Future[StorageUnit] = {
    for {
      resp <- getStorageUnit(id)
      stUnit = Json.parse(resp.body).validate[Storage].get.asInstanceOf[StorageUnit]
    } yield stUnit
  }

  val veryLongUnitName =
    """
       12345678901234567890123456789012345678901234567890
       12345678901234567890123456789012345678901234567890
       12345678901234567890123456789012345678901234567890
       12345678901234567890123456789012345678901234567890
       12345678901234567890123456789012345678901234567890
       12345678901234567890123456789012345678901234567890
       12345678901234567890123456789012345678901234567890
       12345678901234567890123456789012345678901234567890
       12345678901234567890123456789012345678901234567890
       12345678901234567890123456789012345678901234567890
       12345678901234567890123456789012345678901234567890
       12345678901234567890123456789012345678901234567890
    """.replace('\n', ' ')


  "StorageUnitIntegration " must {
    "postCreate some IDs" in {
      val makeMyJSon ="""{"type":"Room","name":"UkjentRom", "sikringSkallsikring": true}"""
      val response = createStorageUnit(makeMyJSon) |> waitFutureValue
      val storageUnit = Json.parse(response.body).validate[Storage].get.asInstanceOf[Room]
      storageUnit.id mustBe Some(1)
      storageUnit.`type` mustBe Room.storageType
      storageUnit.name mustBe "UkjentRom"

    }


    "postCreate a building" in {
      val makeMyJSon ="""{"id":-1, "type":"Building","name":"KHM", "links":[]}"""
      val response = createStorageUnit(makeMyJSon) |> waitFutureValue
      val storageUnit = Json.parse(response.body).validate[Storage].get.asInstanceOf[Building]
      storageUnit.id mustBe Some(2)
      storageUnit.`type` mustBe Building.storageType
      storageUnit.name mustBe "KHM"

    }

    "get by id" in {
      val response = getStorageUnit(1) |> waitFutureValue
      val storageUnit = Json.parse(response.body).validate[Storage].get
      storageUnit.id mustBe Some(1)
    }
    "negative get by id" in {
      val response = getStorageUnit(9999) |> waitFutureValue
      val error = Json.parse(response.body).validate[MusitError].get
      error.message mustBe unknownStorageUnitMsg(9999)
    }

    "get all nodes" in {
      val response = WS.url(s"http://localhost:$port/v1/storageunit").get() |> waitFutureValue
      val storageUnits = Json.parse(response.body).validate[Seq[Storage]].get
      storageUnits.length mustBe 2

    }

    "update storageUnit" in {
      val myJSon ="""{"type":"StorageUnit","name":"hylle2","areaTo":125}"""
      val response = createStorageUnit(myJSon) |> waitFutureValue
      val storageUnit = Json.parse(response.body).validate[Storage].get
      storageUnit.name mustBe "hylle2"

      storageUnit.id.isDefined mustBe true
      val id = storageUnit.id.get
      val areaTo = storageUnit.areaTo
      areaTo mustBe Some(125)

      val storageJson = Json.parse(response.body).asInstanceOf[JsObject]
        .+("name" -> JsString("hylle3"))
        .+("areaTo" -> JsNumber(130))
        .+("heightTo" -> JsNumber(230))

      val antUpdated = updateStorageUnit(id, storageJson.toString()) |> waitFutureValue
      assert(antUpdated.status == 200)
      val updatedObjectResponse = getStorageUnit(id) |> waitFutureValue
      val updatedObject = Json.parse(updatedObjectResponse.body).validate[Storage].get

      updatedObject.name mustBe "hylle3"
      updatedObject.areaTo mustBe Some(130)
    }

    "update storageRoom" in {
      val myJSon ="""{"type":"Room","name":"Rom1", "sikringSkallsikring": false}"""
      val future = createStorageUnit(myJSon)
      val response = future.futureValue
      val storageUnit = Json.parse(response.body).validate[Storage].get.asInstanceOf[Room]
      storageUnit.sikringSkallsikring mustBe Some(false)

      storageUnit.id.isDefined mustBe true
      val id = storageUnit.id.get

      storageUnit.name mustBe "Rom1"
      storageUnit.sikringSkallsikring mustBe Some(false)

      val udateRoomJson = s"""{"type":"Room","id": $id, "name":"RomNyttNavn", "sikringSkallsikring": true}"""
      val res = (for {
        _ <- updateStorageUnit(id, udateRoomJson)
        room <- getRoomAsObject(id)
      } yield room) |> waitFutureValue
      res.name mustBe "RomNyttNavn"
      res.sikringSkallsikring mustBe Some(true)

    val myJSonRoom = s"""{"type":"Room","id": $id, "name":"ROM1"}"""

      val future2 = for {
        oppdat <- updateStorageUnit(id, myJSonRoom)
        stUnit <- getRoomAsObject(id)
      } yield stUnit
      val stUnit2 = future2 |> waitFutureValue
      assert(stUnit2.name == "ROM1")
    }

    "update storageBuilding" in {
      val myJSon ="""{"type":"Building","name":"Bygning0", "address": "vet ikke"}"""
      val future = createStorageUnit(myJSon)
      val response = future |> waitFutureValue
      val storageUnit = Json.parse(response.body).validate[Storage].get.asInstanceOf[Building]
      storageUnit.address mustBe Some("vet ikke")
      val id = storageUnit.id.get
      storageUnit.name mustBe "Bygning0"

      val udateJson = s"""{"type":"Building","id": $id, "name":"NyBygning", "address": "OrdentligAdresse"}"""
      val res = (for {
        res <- updateStorageUnit(id, udateJson)
        room <- getBuildingAsObject(id)
      } yield (room, res)) |> waitFutureValue
      res._1.name mustBe "NyBygning"
      res._1.address mustBe Some("OrdentligAdresse")
    }

    "update room should fail with bad id" in {
      val myJSonRoom ="""{"type":"Room","name":"ROM1"}"""
      val response = updateStorageUnit(125254764, myJSonRoom) |> waitFutureValue

      val error = Json.parse(response.body).validate[MusitError].get
      error.message mustBe unknownStorageUnitMsg(125254764)
    }

    "postCreate should not be able to insert too long field value" in {
      val makeMyJSon =s"""{"type":"Room","name":"$veryLongUnitName", "sikringSkallsikring": true}"""
      val response = createStorageUnit(makeMyJSon) |> waitFutureValue

      val error = Json.parse(response.body).validate[MusitError].get

      error.getDeveloperMessage must include("Value too long")
    }


    "create room transaction should not create a storageUnit in the database if the room doesn't get created. (Transaction failure)" in {
      val makeMyJSon ="""{"type":"Room","name":"UkjentRom2", "sikringSkallsikring": true}"""
      val response = createStorageUnit(makeMyJSon) |> waitFutureValue
      val storageUnit = Json.parse(response.body).validate[Storage].get.asInstanceOf[Room]

      storageUnit.id.isDefined mustBe true
      val id = storageUnit.id.get //Just to know which is the current id, the next is supposed to fail....

      val jsonWhichShouldFail =s"""{"type":"Room","name":"$veryLongUnitName", "sikringSkallsikring": false}"""
      val response2 = createStorageUnit(jsonWhichShouldFail) |> waitFutureValue
      val error = Json.parse(response2.body).validate[MusitError].get

      error.getDeveloperMessage must include("Value too long")

      val getResponse = getStorageUnit(id + 1) |> waitFutureValue

      val errorOnGet = Json.parse(getResponse.body).validate[MusitError].get
      errorOnGet.message mustBe unknownStorageUnitMsg(id + 1)

    }

    "create and delete room" in {
      val makeMyJSon ="""{"type":"Room","name":"UkjentRom2", "sikringSkallsikring": true}"""
      val response = createStorageUnit(makeMyJSon) |> waitFutureValue
      val storageUnit = Json.parse(response.body).validate[Storage].get.asInstanceOf[Room]
      response.status mustBe 201 //Successfully created the room

      storageUnit.id.isDefined mustBe true
      val id = storageUnit.id.get //Just to know which is the current id, the next is supposed to fail....

      val responsDel = deleteStorageUnit(id) |> waitFutureValue
      responsDel.status mustBe 200 //Successfully deleted the room

      val responsGet = getStorageUnit(id) |> waitFutureValue
      responsGet.status mustBe 404 //Shouldn't find a deleted room

      val responsDel2 = deleteStorageUnit(id) |> waitFutureValue
      responsDel2.status mustBe 404 //Shouldn't be able to delete a deleted room
    }


    "not be able to delete a storageUnit which has never existed" in {
      val responsDel = deleteStorageUnit(12345678) |> waitFutureValue
      responsDel.status mustBe 404
    }

    "not be able to update a deleted storageUnit" in {

      val json ="""{"type":"StorageUnit","name":"UkjentUnit"}"""
      val response = createStorageUnit(json) |> waitFutureValue
      println("deleted storageUnit " + response.body)
      response.status mustBe 201 //Successfully created the room
      val storageUnit = Json.parse(response.body).validate[Storage].get.asInstanceOf[StorageUnit]

      storageUnit.id.isDefined mustBe true
      val id = storageUnit.id.get

      val responsDel = deleteStorageUnit(id) |> waitFutureValue
      println("deleted storageUnit " + responsDel.body)
      responsDel.status mustBe 200 //Successfully deleted

      val updateJson = s"""{"type":"StorageUnit","id": $id, "name":"NyUkjentUnit"}"""
      val updateResponse = updateStorageUnit(id, updateJson) |> waitFutureValue
      println("deleted storageUnit " + updateResponse.body)
      updateResponse.status mustBe 404 //Should not be able to update a deleted object
    }

    "not be able to update a deleted room" in {

      val json ="""{"type":"Room","name":"UkjentRom"}"""
      val response = createStorageUnit(json) |> waitFutureValue
      println("deleted room " + response.body)
      response.status mustBe 201 //Successfully created the room
      val storageUnit = Json.parse(response.body).validate[Storage].get.asInstanceOf[Room]

      storageUnit.id.isDefined mustBe true
      val id = storageUnit.id.get

      val responsDel = deleteStorageUnit(id) |> waitFutureValue
      println("deleted room " + responsDel.body)
      responsDel.status mustBe 200 //Successfully deleted

      val updateJson = """{"type":"Room","name":"NyttRom", "sikringSkallsikring": true}"""
      val updateResponse = updateStorageUnit(id, updateJson) |> waitFutureValue
      println("deleted room " + updateResponse.body)
      updateResponse.status mustBe 404 //Should not be able to update a deleted object
    }

    "not be able to update a deleted building" in {

      val json ="""{"type":"Building","name":"UkjentBygning"}"""
      val response = createStorageUnit(json) |> waitFutureValue
      println("deleted building " + response.body)
      response.status mustBe 201 //Successfully created the room
      val storageUnit = Json.parse(response.body).validate[Storage].get.asInstanceOf[Building]

      storageUnit.id.isDefined mustBe true
      val id = storageUnit.id.get

      val responsDel = deleteStorageUnit(id) |> waitFutureValue
      println("deleted building " + responsDel.body)
      responsDel.status mustBe 200 //Successfully deleted

      val updateJson = """{"type":"Building","name":"NyBygning", "address": "OrdentligAdresse"}"""
      val updateResponse = updateStorageUnit(id, updateJson) |> waitFutureValue
      println("deleted building " + updateResponse.body)
      updateResponse.status mustBe 404 //Should not be able to update a deleted object
    }




    "update should fail (with Conflict=409) if inconsistent storage types" in {
      val json ="""{"type":"Room","name":"UkjentRom2", "sikringSkallsikring": true}"""
      val response = createStorageUnit(json) |> waitFutureValue
      response.status mustBe 201 //Created

      val storageUnit = Json.parse(response.body).validate[Storage].get.asInstanceOf[Room]

      storageUnit.id.isDefined mustBe true
      val id = storageUnit.id.get

      val updatedJson ="""{"type":"Building", "name":"Ukjent bygning", "address":"HelloAddress"}"""
      val responseUpdate = updateStorageUnit(id, updatedJson) |> waitFutureValue
      responseUpdate.status mustBe 409 //Conflict
    }

    "create should fail with invalid input data" in {
      val json ="""{"type":"Room","name":"UkjentRom2", "sikringSkallsikring": 1}"""
      val response = createStorageUnit(json) |> waitFutureValue
      response.status mustBe 400
    }
  }
}
