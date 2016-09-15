package no.uio.musit.microservice.storagefacility.resource

import no.uio.musit.microservice.storagefacility.domain.storage._
import no.uio.musit.test.TestConfigs
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsNumber, JsObject, JsString, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Created by ellenjo on 5/27/16.
 */
class StorageUnitIntegrationSpec extends PlaySpec with OneServerPerSuite with ScalaFutures {
  override lazy val port: Int = 19002
  implicit override lazy val app = new GuiceApplicationBuilder()
    .configure(TestConfigs.inMemoryDatabaseConfig()).build()


  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  val unknownStorageUnitMsg = (id: Long) => s"Unknown storageUnit with id: $id"

  def createStorageUnit(json: String) = {
    wsUrl("/v1/storagenodes").post(Json.parse(json))
  }

  def updateStorageUnit(id: Long, json: String) = {
    wsUrl(s"/v1/storagenodes/$id").post(Json.parse(json))
  }

  def deleteStorageUnit(id: Long) = {
    wsUrl(s"/v1/storagenodes/$id").delete
  }

  def getStorageUnit(id: Long) = wsUrl(s"/v1/storagenodes/$id").get

  def getRoomAsObject(id: Long): Future[Room] = {
    for {
      resp <- getStorageUnit(id)
      room = Json.parse(resp.body).validate[StorageNode].get.asInstanceOf[Room]
    } yield room
  }

  def getBuildingAsObject(id: Long): Future[Building] = {
    for {
      resp <- getStorageUnit(id)
      room = Json.parse(resp.body).validate[StorageNode].get.asInstanceOf[Building]
    } yield room
  }


  def getStorageUnitAsObject(id: Long): Future[StorageUnit] = {
    for {
      resp <- getStorageUnit(id)
      stUnit = Json.parse(resp.body).validate[StorageNode].get.asInstanceOf[StorageUnit]
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

  "Running the storage facility service" when {

    "interacting with the StorageUnitResource endpoints" should {

      "postCreate some IDs" in {
        val makeMyJSon ="""{"type":"Room","name":"UkjentRom", "sikringSkallsikring": true}"""
        val response = createStorageUnit(makeMyJSon).futureValue
        val storageUnit = Json.parse(response.body).validate[StorageNode].get.asInstanceOf[Room]
        storageUnit.id mustBe Some(1)
        storageUnit.storageType mustBe StorageType.Room
        storageUnit.name mustBe "UkjentRom"

      }


      "postCreate a building" in {
        val makeMyJSon ="""{"id":-1, "type":"Building","name":"KHM", "links":[]}"""
        val response = createStorageUnit(makeMyJSon).futureValue
        val storageUnit = Json.parse(response.body).validate[StorageNode].get.asInstanceOf[Building]
        storageUnit.id mustBe Some(2)
        storageUnit.storageType mustBe StorageType.Building
        storageUnit.name mustBe "KHM"

      }

      "get by id" in {
        val response = getStorageUnit(1).futureValue
        val storageUnit = Json.parse(response.body).validate[StorageNode].get
        storageUnit.id mustBe Some(1)
      }
      "negative get by id" in {
        val response = getStorageUnit(9999).futureValue
        response.status mustBe 404
      }

      "get all nodes" in {
        val response = wsUrl("/v1/storagenodes").get().futureValue
        val storageUnits = Json.parse(response.body).validate[Seq[StorageNode]].get
        storageUnits.length mustBe 2
      }

      "update storageUnit" in {
        val myJSon ="""{"type":"StorageUnit","name":"hylle2","areaTo":125}"""
        val response = createStorageUnit(myJSon).futureValue
        val storageUnit = Json.parse(response.body).validate[StorageNode].get
        storageUnit.name mustBe "hylle2"

        storageUnit.id.isDefined mustBe true
        val id = storageUnit.id.get
        val areaTo = storageUnit.areaTo
        areaTo mustBe Some(125)

        val storageJson = Json.parse(response.body).asInstanceOf[JsObject]
          .+("name" -> JsString("hylle3"))
          .+("areaTo" -> JsNumber(130))
          .+("heightTo" -> JsNumber(230))

        val antUpdated = updateStorageUnit(id, storageJson.toString()).futureValue
        assert(antUpdated.status == 200)
        val updatedObjectResponse = getStorageUnit(id).futureValue
        val updatedObject = Json.parse(updatedObjectResponse.body).validate[StorageNode].get

        updatedObject.name mustBe "hylle3"
        updatedObject.areaTo mustBe Some(130)
      }

      "update storageRoom" in {
        val myJSon ="""{"type":"Room","name":"Rom1", "sikringSkallsikring": false}"""
        val future = createStorageUnit(myJSon)
        val response = future.futureValue
        val storageUnit = Json.parse(response.body).validate[StorageNode].get.asInstanceOf[Room]
        storageUnit.sikringSkallsikring mustBe Some(false)

        storageUnit.id.isDefined mustBe true
        val id = storageUnit.id.get

        storageUnit.name mustBe "Rom1"
        storageUnit.sikringSkallsikring mustBe Some(false)

        val udateRoomJson = s"""{"type":"Room","id": $id, "name":"RomNyttNavn", "sikringSkallsikring": true}"""
        val res = (for {
          _ <- updateStorageUnit(id, udateRoomJson)
          room <- getRoomAsObject(id)
        } yield room).futureValue
        res.name mustBe "RomNyttNavn"
        res.sikringSkallsikring mustBe Some(true)

        val myJSonRoom = s"""{"type":"Room","id": $id, "name":"ROM1"}"""

        val future2 = for {
          oppdat <- updateStorageUnit(id, myJSonRoom)
          stUnit <- getRoomAsObject(id)
        } yield stUnit
        val stUnit2 = future2.futureValue
        assert(stUnit2.name == "ROM1")
      }

      "update storageBuilding" in {
        val myJSon ="""{"type":"Building","name":"Bygning0", "address": "vet ikke"}"""
        val future = createStorageUnit(myJSon)
        val response = future.futureValue
        val storageUnit = Json.parse(response.body).validate[StorageNode].get.asInstanceOf[Building]
        storageUnit.address mustBe Some("vet ikke")
        val id = storageUnit.id.get
        storageUnit.name mustBe "Bygning0"

        val udateJson = s"""{"type":"Building","id": $id, "name":"NyBygning", "address": "OrdentligAdresse"}"""
        val res = (for {
          res <- updateStorageUnit(id, udateJson)
          room <- getBuildingAsObject(id)
        } yield (room, res)).futureValue
        res._1.name mustBe "NyBygning"
        res._1.address mustBe Some("OrdentligAdresse")
      }

      "update room should fail with bad id" in {
        val myJSonRoom ="""{"type":"Room","name":"ROM1"}"""
        val response = updateStorageUnit(125254764, myJSonRoom).futureValue

        response.status mustBe 404
      }

      //    "postCreate should not be able to insert too long field value" in {
      //      // TODO: What is the max-length of such a name?
      //      val makeMyJSon =s"""{"type":"Room","name":"$veryLongUnitName", "sikringSkallsikring": true}"""
      //      val response = createStorageUnit(makeMyJSon).futureValue
      //
      //      val error = Json.parse(response.body).validate[MusitError].get
      //
      //      error.getDeveloperMessage must include("Value too long")
      //    }
      //
      //
      //    "create room transaction should not create a storageUnit in the database if the room doesn't get created. (Transaction failure)" in {
      //      val makeMyJSon ="""{"type":"Room","name":"UkjentRom2", "sikringSkallsikring": true}"""
      //      val response = createStorageUnit(makeMyJSon).futureValue
      //      val storageUnit = Json.parse(response.body).validate[StorageNode].get.asInstanceOf[Room]
      //
      //      storageUnit.id.isDefined mustBe true
      //      val id = storageUnit.id.get //Just to know which is the current id, the next is supposed to fail....
      //
      //      // TODO: What is the max-length of such a name?
      //      val jsonWhichShouldFail =s"""{"type":"Room","name":"$veryLongUnitName", "sikringSkallsikring": false}"""
      //      val response2 = createStorageUnit(jsonWhichShouldFail).futureValue
      //      val error = Json.parse(response2.body).validate[MusitError].get
      //
      //      error.getDeveloperMessage must include("Value too long")
      //
      //      val getResponse = getStorageUnit(id + 1).futureValue
      //
      //      val errorOnGet = Json.parse(getResponse.body).validate[MusitError].get
      //      errorOnGet.message mustBe unknownStorageUnitMsg(id + 1)
      //
      //    }

      "create and delete room" in {
        val makeMyJSon ="""{"type":"Room","name":"UkjentRom2", "sikringSkallsikring": true}"""
        val response = createStorageUnit(makeMyJSon).futureValue
        val storageUnit = Json.parse(response.body).validate[StorageNode].get.asInstanceOf[Room]
        response.status mustBe 201 //Successfully created the room

        storageUnit.id.isDefined mustBe true
        val id = storageUnit.id.get //Just to know which is the current id, the next is supposed to fail....

        val responsDel = deleteStorageUnit(id).futureValue
        responsDel.status mustBe 200 //Successfully deleted the room

        val responsGet = getStorageUnit(id).futureValue
        responsGet.status mustBe 404 //Shouldn't find a deleted room

        val responsDel2 = deleteStorageUnit(id).futureValue
        responsDel2.status mustBe 404 //Shouldn't be able to delete a deleted room
      }


      "not be able to delete a storageUnit which has never existed" in {
        val responsDel = deleteStorageUnit(12345678).futureValue
        responsDel.status mustBe 404
      }

      "not be able to update a deleted storageUnit" in {
        val json ="""{"type":"StorageUnit","name":"UkjentUnit"}"""
        val response = createStorageUnit(json).futureValue
        println("deleted storageUnit " + response.body)
        response.status mustBe 201 //Successfully created the room
        val storageUnit = Json.parse(response.body).validate[StorageNode].get.asInstanceOf[StorageUnit]

        storageUnit.id.isDefined mustBe true
        val id = storageUnit.id.get

        val responsDel = deleteStorageUnit(id).futureValue
        println("deleted storageUnit " + responsDel.body)
        responsDel.status mustBe 200 //Successfully deleted

        val updateJson = s"""{"type":"StorageUnit","id": $id, "name":"NyUkjentUnit"}"""
        val updateResponse = updateStorageUnit(id, updateJson).futureValue
        println("deleted storageUnit " + updateResponse.body)
        updateResponse.status mustBe 404 //Should not be able to update a deleted object
      }

      "not be able to update a deleted room" in {
        val json ="""{"type":"Room","name":"UkjentRom"}"""
        val response = createStorageUnit(json).futureValue
        println("deleted room " + response.body)
        response.status mustBe 201 //Successfully created the room
        val storageUnit = Json.parse(response.body).validate[StorageNode].get.asInstanceOf[Room]

        storageUnit.id.isDefined mustBe true
        val id = storageUnit.id.get

        val responsDel = deleteStorageUnit(id).futureValue
        println("deleted room " + responsDel.body)
        responsDel.status mustBe 200 //Successfully deleted

        val updateJson = """{"type":"Room","name":"NyttRom", "sikringSkallsikring": true}"""
        val updateResponse = updateStorageUnit(id, updateJson).futureValue
        println("deleted room " + updateResponse.body)
        updateResponse.status mustBe 404 //Should not be able to update a deleted object
      }

      "not be able to update a deleted building" in {
        val json ="""{"type":"Building","name":"UkjentBygning"}"""
        val response = createStorageUnit(json).futureValue
        println("deleted building " + response.body)
        response.status mustBe 201 //Successfully created the room
        val storageUnit = Json.parse(response.body).validate[StorageNode].get.asInstanceOf[Building]

        storageUnit.id.isDefined mustBe true
        val id = storageUnit.id.get

        val responsDel = deleteStorageUnit(id).futureValue
        println("deleted building " + responsDel.body)
        responsDel.status mustBe 200 //Successfully deleted

        val updateJson = """{"type":"Building","name":"NyBygning", "address": "OrdentligAdresse"}"""
        val updateResponse = updateStorageUnit(id, updateJson).futureValue
        println("deleted building " + updateResponse.body)
        updateResponse.status mustBe 404 //Should not be able to update a deleted object
      }

      // FIXME: Should fail with 400 BadRequest...409 is if you try to add something with the same name.
      "update should fail (with Conflict=409) if inconsistent storage types" in {
        val json ="""{"type":"Room","name":"UkjentRom2", "sikringSkallsikring": true}"""
        val response = createStorageUnit(json).futureValue
        response.status mustBe 201 //Created

        val storageUnit = Json.parse(response.body).validate[StorageNode].get.asInstanceOf[Room]

        storageUnit.id.isDefined mustBe true
        val id = storageUnit.id.get

        val updatedJson ="""{"type":"Building", "name":"Ukjent bygning", "address":"HelloAddress"}"""
        val responseUpdate = updateStorageUnit(id, updatedJson).futureValue
        responseUpdate.status mustBe 409 //Conflict
      }

      "create should fail with invalid input data" in {
        val json ="""{"type":"Room","name":"UkjentRom2", "sikringSkallsikring": 1}"""
        val response = createStorageUnit(json).futureValue
        response.status mustBe 400
      }
    }
  }
}
