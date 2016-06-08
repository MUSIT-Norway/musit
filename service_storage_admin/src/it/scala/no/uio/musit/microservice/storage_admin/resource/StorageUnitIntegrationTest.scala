package no.uio.musit.microservice.storage_admin.resource

import no.uio.musit.microservice.storageAdmin.domain._
import no.uio.musit.microservice.storageAdmin.service.StorageUnitService
import no.uio.musit.microservices.common.PlayTestDefaults
import no.uio.musit.microservices.common.PlayTestDefaults._
import no.uio.musit.microservices.common.domain.MusitError
import no.uio.musit.microservices.common.extensions.PlayExtensions._
import no.uio.musit.microservices.common.utils.Misc._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, JsString, Json}
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


  def unknownStorageUnitMsg(id: Long) = StorageUnitService.unknownStorageUnitMsg(id)

  def createStorageUnit(json: String) = {
    WS.url(s"http://localhost:$port/v1/storageunit").postJsonString(json)
  }

  def updateStorageUnit(id: Long, json: String) = {
    WS.url(s"http://localhost:$port/v1/storageunit/${id}").putJsonString(json)
  }

  def deleteStorageUnit(id: Long) = {
    WS.url(s"http://localhost:$port/v1/storageunit/${id}").delete
  }

  def getStorageUnit(id: Long) = WS.url(s"http://localhost:$port/v1/storageunit/${id}").get

  def getRoomAsObject(id: Long): Future[StorageRoom] = {
    for {
      resp <- getStorageUnit(id)
      room = Json.parse(resp.body).validate[StorageRoom].get
    } yield room
  }

  def getBuildingAsObject(id: Long): Future[StorageBuilding] = {
    for {
      resp <- getStorageUnit(id)
      room = Json.parse(resp.body).validate[StorageBuilding].get
    } yield room
  }


  def getStorageUnitAsObject(id: Long): Future[StorageUnit] = {
    for {
      resp <- getStorageUnit(id)
      stUnit = Json.parse(resp.body).validate[StorageUnit].get
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
      val makeMyJSon ="""{"storageType":"Room","storageUnitName":"UkjentRom", "sikringSkallsikring": true}"""
      val response = createStorageUnit(makeMyJSon) |> waitFutureValue
      val storageUnit = Json.parse(response.body).validate[StorageUnit].get
      val storageRoom = Json.parse(response.body).validate[StorageRoom].get
      storageUnit.getId mustBe 1
      storageUnit.storageKind mustBe Room
      storageUnit.storageUnitName mustBe "UkjentRom"

    }


    "postCreate a building" in {
      val makeMyJSon ="""{"id":-1, "storageType":"Building","storageUnitName":"KHM", "links":[]}"""
      val response = createStorageUnit(makeMyJSon) |> waitFutureValue
      val storageUnit = Json.parse(response.body).validate[StorageUnit].get
      val storageBuilding = Json.parse(response.body).validate[StorageBuilding].get
      storageUnit.id mustBe Some(2)
      storageUnit.storageKind mustBe Building
      storageUnit.storageUnitName mustBe "KHM"

    }

    "get by id" in {
      val response = getStorageUnit(1) |> waitFutureValue
      val storageUnit = Json.parse(response.body).validate[StorageUnit].get
      storageUnit.getId mustBe 1
    }
    "negative get by id" in {
      val response = getStorageUnit(9999) |> waitFutureValue
      val error = Json.parse(response.body).validate[MusitError].get
      error.message mustBe unknownStorageUnitMsg(9999)
    }

    "get all nodes" in {
      val response = WS.url(s"http://localhost:$port/v1/storageunit").get() |> waitFutureValue
      val storageUnits = Json.parse(response.body).validate[Seq[StorageUnit]].get
      storageUnits.length mustBe 2

    }
    "update roomName" in {
      val myJSonRoom ="""{"storageType":"Room","storageUnitName":"ROM1"}"""

      val future = for {
        oppdat <- updateStorageUnit(1, myJSonRoom)
        stUnit <- getStorageUnitAsObject(1)
      } yield stUnit
      val stUnit = future |> waitFutureValue
      assert(stUnit.storageUnitName == "ROM1")
    }


    "update storageUnit" in {
      val myJSon ="""{"storageType":"storageunit","storageUnitName":"hylle2"}"""
      val response = createStorageUnit(myJSon) |> waitFutureValue
      val storageUnit = Json.parse(response.body).validate[StorageUnit].get
      storageUnit.storageUnitName mustBe "hylle2"
      val storageJson = Json.parse(response.body).asInstanceOf[JsObject].+("storageUnitName" -> JsString("hylle3"))

      val antUpdated = updateStorageUnit(storageUnit.getId, storageJson.toString()) |> waitFutureValue
      assert(antUpdated.status == 200)
      val updatedObjectResponse = getStorageUnit(storageUnit.getId) |> waitFutureValue
      val updatedObject = Json.parse(updatedObjectResponse.body).validate[StorageUnit].get

      updatedObject.storageUnitName mustBe ("hylle3")

    }

    "update storageRoom" in {
      val myJSon ="""{"storageType":"room","storageUnitName":"Rom1", "sikringSkallsikring": false}"""
      val future = createStorageUnit(myJSon)
      val response = future.futureValue
      val storageUnit = Json.parse(response.body).validate[StorageUnit].get
      val storageRoom = Json.parse(response.body).validate[StorageRoom].get
      storageRoom.sikringSkallsikring mustBe Some(false)
      val id = storageUnit.getId
      storageUnit.storageUnitName mustBe "Rom1"

      val udateRoomJson = s"""{"storageType":"room","storageUnitName":"RomNyttNavn", "sikringSkallsikring": true}"""
      val res = (for {
        _ <- updateStorageUnit(id, udateRoomJson)
        room <- getRoomAsObject(id)
        stUnit <- getStorageUnitAsObject(id)
      } yield (stUnit, room)) |> waitFutureValue
      res._1.storageUnitName mustBe "RomNyttNavn"
      res._2.sikringSkallsikring mustBe Some(true)
    }


    "update storageBuilding" in {
      val myJSon ="""{"storageType":"building","storageUnitName":"Bygning0", "address": "vet ikke"}"""
      val future = createStorageUnit(myJSon)
      val response = future |> waitFutureValue
      val storageUnit = Json.parse(response.body).validate[StorageUnit].get
      val storageBuilding = Json.parse(response.body).validate[StorageBuilding].get
      storageBuilding.address mustBe Some("vet ikke")
      val id = storageUnit.getId
      storageUnit.storageUnitName mustBe "Bygning0"

      val udateJson = """{"storageType":"building","storageUnitName":"NyBygning", "address": "OrdentligAdresse"}"""
      val res = (for {
        _ <- updateStorageUnit(id, udateJson)
        room <- getBuildingAsObject(id)
        stUnit <- getStorageUnitAsObject(id)
      } yield (stUnit, room)) |> waitFutureValue

      res._1.storageUnitName mustBe "NyBygning"
      res._2.address mustBe Some("OrdentligAdresse")
    }

    "update room should fail with bad id" in {
      val myJSonRoom ="""{"storageType":"Room","storageUnitName":"ROM1"}"""
      val response = updateStorageUnit(125254764, myJSonRoom) |> waitFutureValue

      val error = Json.parse(response.body).validate[MusitError].get
      error.message mustBe unknownStorageUnitMsg(125254764)
    }

    "postCreate should not be able to insert too long field value" in {
      val makeMyJSon =s"""{"storageType":"Room","storageUnitName":"$veryLongUnitName", "sikringSkallsikring": true}"""
      val response = createStorageUnit(makeMyJSon) |> waitFutureValue

      val error = Json.parse(response.body).validate[MusitError].get

      error.getDeveloperMessage must include("Value too long")
    }


    "create room transaction should not create a storageUnit in the database if the room doesn't get created. (Transaction failure)" in {
      val makeMyJSon ="""{"storageType":"Room","storageUnitName":"UkjentRom2", "sikringSkallsikring": true}"""
      val response = createStorageUnit(makeMyJSon) |> waitFutureValue
      val storageUnit = Json.parse(response.body).validate[StorageUnit].get

      val id = storageUnit.getId //Just to know which is the current id, the next is supposed to fail....

      val jsonWhichShouldFail =s"""{"storageType":"Room","storageUnitName":"$veryLongUnitName", "sikringSkallsikring": false}"""
      val response2 = createStorageUnit(jsonWhichShouldFail) |> waitFutureValue
      val error = Json.parse(response2.body).validate[MusitError].get

      error.getDeveloperMessage must include("Value too long")

      val getResponse = getStorageUnit(id + 1) |> waitFutureValue

      val errorOnGet = Json.parse(getResponse.body).validate[MusitError].get
      errorOnGet.message mustBe unknownStorageUnitMsg(id + 1)

    }

    "create and delete room" in {
      val makeMyJSon ="""{"storageType":"Room","storageUnitName":"UkjentRom2", "sikringSkallsikring": true}"""
      val response = createStorageUnit(makeMyJSon) |> waitFutureValue
      val storageUnit = Json.parse(response.body).validate[StorageUnit].get
      response.status mustBe 201 //Successfully created the room

      val id = storageUnit.getId //Just to know which is the current id, the next is supposed to fail....
      val responsDel = deleteStorageUnit(id) |> waitFutureValue
      responsDel.status mustBe 200 //Successfully deleted the room

      val responsGet = getStorageUnit(id) |> waitFutureValue
      responsGet.status mustBe 404 //Shouldn't find a deleted room

      val responsDel2 = deleteStorageUnit(id) |> waitFutureValue
      responsDel2.status mustBe 404 //Shouldn't be able to delete a deleted room
    }


    "should not be able to delete a storageUnit which has never existed" in {
      val responsDel = deleteStorageUnit(12345678) |> waitFutureValue
      responsDel.status mustBe 404
    }


    "should not be able to update a deleted storageUnit" in {

      val json ="""{"storageType":"StorageUnit","storageUnitName":"UkjentUnit"}"""
      val response = createStorageUnit(json) |> waitFutureValue
      response.status mustBe 201 //Successfully created the room
      val storageUnit = Json.parse(response.body).validate[StorageUnit].get
      val responsDel = deleteStorageUnit(storageUnit.getId) |> waitFutureValue
      responsDel.status mustBe 200 //Successfully deleted

      val updateJson = """{"storageType":"StorageUnit","storageUnitName":"NyUkjentUnit"}"""
      val updateResponse = updateStorageUnit(storageUnit.getId, updateJson) |> waitFutureValue
      updateResponse.status mustBe 404 //Should not be able to update a deleted object
    }




    "should not be able to update a deleted room" in {

      val json ="""{"storageType":"Room","storageUnitName":"UkjentRom"}"""
      val response = createStorageUnit(json) |> waitFutureValue
      response.status mustBe 201 //Successfully created the room
      val storageUnit = Json.parse(response.body).validate[StorageUnit].get
      val responsDel = deleteStorageUnit(storageUnit.getId) |> waitFutureValue
      responsDel.status mustBe 200 //Successfully deleted

      val updateJson = """{"storageType":"Room","storageUnitName":"NyttRom", "sikringSkallsikring": true}"""
      val updateResponse = updateStorageUnit(storageUnit.getId, updateJson) |> waitFutureValue
      updateResponse.status mustBe 404 //Should not be able to update a deleted object
    }

    "should not be able to update a deleted building" in {

      val json ="""{"storageType":"Building","storageUnitName":"UkjentBygning"}"""
      val response = createStorageUnit(json) |> waitFutureValue
      response.status mustBe 201 //Successfully created the room
      val storageUnit = Json.parse(response.body).validate[StorageUnit].get
      val responsDel = deleteStorageUnit(storageUnit.getId) |> waitFutureValue
      responsDel.status mustBe 200 //Successfully deleted

      val updateJson = """{"storageType":"building","storageUnitName":"NyBygning", "address": "OrdentligAdresse"}"""
      val updateResponse = updateStorageUnit(storageUnit.getId, updateJson) |> waitFutureValue
      updateResponse.status mustBe 404 //Should not be able to update a deleted object
    }




    "update should fail (with Conflict=409) if inconsistent storage types" in {
      val json ="""{"storageType":"Room","storageUnitName":"UkjentRom2", "sikringSkallsikring": true}"""
      val response = createStorageUnit(json) |> waitFutureValue
      response.status mustBe 201 //Created

      val storageUnit = Json.parse(response.body).validate[StorageUnit].get

      val updatedJson ="""{"storageType":"Building", "storageUnitName":"Ukjent bygning", "address":"HelloAddress"}"""
      val responseUpdate = updateStorageUnit(storageUnit.getId, updatedJson) |> waitFutureValue
      responseUpdate.status mustBe 409 //Conflict
    }

    "create should fail with invalid input data" in {
      val json ="""{"storageType":"Room","storageUnitName":"UkjentRom2", "sikringSkallsikring": "1"}"""
      val response = createStorageUnit(json) |> waitFutureValue
      response.status mustBe 400
    }


  }
}
