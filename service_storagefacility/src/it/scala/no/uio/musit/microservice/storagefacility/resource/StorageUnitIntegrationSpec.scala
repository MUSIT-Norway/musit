package no.uio.musit.microservice.storagefacility.resource

import no.uio.musit.microservice.storagefacility.domain.storage._
import no.uio.musit.test.TestConfigs
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json._
import no.uio.musit.microservice.storagefacility.testdata.StorageNodeJsonGenerator._
import play.api.libs.ws.WSResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.reflect.ClassTag


class StorageUnitIntegrationSpec extends PlaySpec
  with OneServerPerSuite
  with ScalaFutures {

  override lazy val port: Int = 19002

  implicit override lazy val app = new GuiceApplicationBuilder()
    .configure(TestConfigs.inMemoryDatabaseConfig()).build()

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

//  val unknownStorageUnitMsg = (id: Long) => s"Unknown storageNode with id: $id"

  def postStorageNode(json: JsValue): Future[WSResponse] = {
    wsUrl("/v1/storagenodes").post(json)
  }

  def putStorageNode(id: Long, json: JsValue): Future[WSResponse] = {
    wsUrl(s"/v1/storagenodes/$id").post(json)
  }

  def deleteStorageUnit(id: Long): Future[WSResponse] = {
    wsUrl(s"/v1/storagenodes/$id").delete
  }

  def getStorageNode(id: Long): Future[WSResponse] =
    wsUrl(s"/v1/storagenodes/$id").get

  def getRoom(id: Long): Future[Room] = {
    for {
      resp <- getStorageNode(id)
      room = Json.parse(resp.body).validate[StorageNode].get.asInstanceOf[Room]
    } yield room
  }

  def getBuilding(id: Long): Future[Building] = {
    for {
      resp <- getStorageNode(id)
      room = Json.parse(resp.body).validate[StorageNode].get.asInstanceOf[Building]
    } yield room
  }

  def getStorageUnit(id: Long): Future[StorageUnit] = {
    for {
      resp <- getStorageNode(id)
      stUnit = Json.parse(resp.body).validate[StorageNode].get.asInstanceOf[StorageUnit]
    } yield stUnit
  }

  val veryLongUnitName =
    """12345678901234567890123456789012345678901234567890
      |12345678901234567890123456789012345678901234567890
      |12345678901234567890123456789012345678901234567890
      |12345678901234567890123456789012345678901234567890
      |12345678901234567890123456789012345678901234567890
      |12345678901234567890123456789012345678901234567890
      |12345678901234567890123456789012345678901234567890
      |12345678901234567890123456789012345678901234567890
      |12345678901234567890123456789012345678901234567890
      |12345678901234567890123456789012345678901234567890
      |12345678901234567890123456789012345678901234567890
      |12345678901234567890123456789012345678901234567890
      |""".stripMargin.replace('\n', ' ')


  def verify[T <: StorageNode](
    response: WSResponse,
    expectedStorageType: StorageType,
    expectedName: String,
    expectedId: Long,
    expectedPartOf: Option[Long] = None
  )(implicit m: Manifest[T]) = {

    val parsed = Json.parse(response.body).validate[StorageNode]
    parsed.isSuccess mustBe true

    val storageNode = parsed.get
    // verifying common attributes across all storage node types
    storageNode mustBe a[T]
    storageNode.id mustBe Some(StorageNodeId(expectedId))
    storageNode.storageType mustBe expectedStorageType
    storageNode.isPartOf mustBe expectedPartOf.map(StorageNodeId.apply)
    storageNode.name mustBe expectedName
  }


  "Running the storage facility service" when {

    "interacting with the StorageUnitResource endpoints" should {

      "successfully create an organisation node" in {
        val makeMyJSon = organisationJson("My Org1")
        val response = postStorageNode(makeMyJSon).futureValue
        response.status mustBe 201

        verify[Organisation](response, StorageType.Organisation, "My Org1", 1)
      }

      "successfully create a building node" in {
        val makeMyJSon = buildingJson("My Building1", StorageNodeId(1))
        val response = postStorageNode(makeMyJSon).futureValue
        response.status mustBe 201

        verify[Building](response, StorageType.Building, "My Building1", 2, Some(1))
      }

      "successfully create a room node" in {
        val makeMyJSon = roomJson("My Room1", StorageNodeId(2))
        val response = postStorageNode(makeMyJSon).futureValue
        response.status mustBe 201

        verify[Room](response, StorageType.Room, "My Room1", 3, Some(2))
      }

      "successfully create a storage unit node" in {
        val makeMyJSon = storageUnitJson("My Shelf1", StorageNodeId(3))
        val response = postStorageNode(makeMyJSon).futureValue
        response.status mustBe 201

        verify[StorageUnit](response, StorageType.StorageUnit, "My Shelf1", 4, Some(3))
      }

      "successfully get an organisation" in {
        val response = getStorageNode(1).futureValue
        response.status mustBe 200

        verify[Organisation](response, StorageType.Organisation, "My Org1", 1)
      }

      "successfully get a building" in {
        val response = getStorageNode(2).futureValue
        response.status mustBe 200

        verify[Building](response, StorageType.Building, "My Building1", 2, Some(1))
      }

      "successfully get a room" in {
        val response = getStorageNode(3).futureValue
        response.status mustBe 200

        verify[Room](response, StorageType.Room, "My Room1", 3, Some(2))
      }

      "successfully get a storage unit" in {
        val response = getStorageNode(4).futureValue
        response.status mustBe 200

        verify[StorageUnit](response, StorageType.StorageUnit, "My Shelf1", 4, Some(3))
      }

//      "return HTTP 404 if a storage node cannot be found" in {
//        val response = getStorageNode(9999).futureValue
//        response.status mustBe 404
//      }
//
//      "successfully update a storage unit" in {
//        val myJSon ="""{"type":"StorageUnit","name":"hylle2","areaTo":125}"""
//        val response = postStorageNode(myJSon).futureValue
//        val storageNode = Json.parse(response.body).validate[StorageNode].get.asInstanceOf[StorageUnit]
//        storageNode.name mustBe "hylle2"
//
//        storageNode.id.isDefined mustBe true
//        val id = storageNode.id.get
//        val areaTo = storageNode.areaTo
//        areaTo mustBe Some(125)
//
//        val storageJson = Json.parse(response.body).asInstanceOf[JsObject]
//          .+("name" -> JsString("hylle3"))
//          .+("areaTo" -> JsNumber(130))
//          .+("heightTo" -> JsNumber(230))
//
//        val antUpdated = putStorageNode(id, storageJson.toString()).futureValue
//        assert(antUpdated.status == 200)
//        val updatedObjectResponse = getStorageNode(id).futureValue
//        val updatedObject = Json.parse(updatedObjectResponse.body).validate[StorageNode].get.asInstanceOf[StorageUnit]
//
//        updatedObject.name mustBe "hylle3"
//        updatedObject.areaTo mustBe Some(130)
//      }
//
//      "successfully update a room" in {
//        val myJSon ="""{"type":"Room","name":"Rom1", "sikringSkallsikring": false}"""
//        val future = postStorageNode(myJSon)
//        val response = future.futureValue
//        val storageNode = Json.parse(response.body).validate[StorageNode].get.asInstanceOf[Room]
//        storageNode.sikringSkallsikring mustBe Some(false)
//
//        storageNode.id.isDefined mustBe true
//        val id = storageNode.id.get
//
//        storageNode.name mustBe "Rom1"
//        storageNode.sikringSkallsikring mustBe Some(false)
//
//        val udateRoomJson = s"""{"type":"Room","id": $id, "name":"RomNyttNavn", "sikringSkallsikring": true}"""
//        val res = (for {
//          _ <- putStorageNode(id, udateRoomJson)
//          room <- getRoomAsObject(id)
//        } yield room).futureValue
//        res.name mustBe "RomNyttNavn"
//        res.sikringSkallsikring mustBe Some(true)
//
//        val myJSonRoom = s"""{"type":"Room","id": $id, "name":"ROM1"}"""
//
//        val future2 = for {
//          oppdat <- putStorageNode(id, myJSonRoom)
//          stUnit <- getRoomAsObject(id)
//        } yield stUnit
//        val stUnit2 = future2.futureValue
//        assert(stUnit2.name == "ROM1")
//      }
//
//      "successfully update a building" in {
//        val myJSon ="""{"type":"Building","name":"Bygning0", "address": "vet ikke"}"""
//        val future = postStorageNode(myJSon)
//        val response = future.futureValue
//        val storageNode = Json.parse(response.body).validate[StorageNode].get.asInstanceOf[Building]
//        storageNode.address mustBe Some("vet ikke")
//        val id = storageNode.id.get
//        storageNode.name mustBe "Bygning0"
//
//        val udateJson = s"""{"type":"Building","id": $id, "name":"NyBygning", "address": "OrdentligAdresse"}"""
//        val res = (for {
//          res <- putStorageNode(id, udateJson)
//          room <- getBuildingAsObject(id)
//        } yield (room, res)).futureValue
//        res._1.name mustBe "NyBygning"
//        res._1.address mustBe Some("OrdentligAdresse")
//      }
//
//      "fail when trying to update a storage node that doesn't exist" in {
//        val myJSonRoom ="""{"type":"Room","name":"ROM1"}"""
//        val response = putStorageNode(125254764, myJSonRoom).futureValue
//
//        response.status mustBe 404
//      }
//
//      //    "postCreate should not be able to insert too long field value" in {
//      //      // TODO: What is the max-length of such a name?
//      //      val makeMyJSon =s"""{"type":"Room","name":"$veryLongUnitName", "sikringSkallsikring": true}"""
//      //      val response = createStorageUnit(makeMyJSon).futureValue
//      //
//      //      val error = Json.parse(response.body).validate[MusitError].get
//      //
//      //      error.getDeveloperMessage must include("Value too long")
//      //    }
//      //
//      //
//      //    "create room transaction should not create a storageNode in the database if the room doesn't get created. (Transaction failure)" in {
//      //      val makeMyJSon ="""{"type":"Room","name":"UkjentRom2", "sikringSkallsikring": true}"""
//      //      val response = createStorageUnit(makeMyJSon).futureValue
//      //      val storageNode = Json.parse(response.body).validate[StorageNode].get.asInstanceOf[Room]
//      //
//      //      storageNode.id.isDefined mustBe true
//      //      val id = storageNode.id.get //Just to know which is the current id, the next is supposed to fail....
//      //
//      //      // TODO: What is the max-length of such a name?
//      //      val jsonWhichShouldFail =s"""{"type":"Room","name":"$veryLongUnitName", "sikringSkallsikring": false}"""
//      //      val response2 = createStorageUnit(jsonWhichShouldFail).futureValue
//      //      val error = Json.parse(response2.body).validate[MusitError].get
//      //
//      //      error.getDeveloperMessage must include("Value too long")
//      //
//      //      val getResponse = getStorageUnit(id + 1).futureValue
//      //
//      //      val errorOnGet = Json.parse(getResponse.body).validate[MusitError].get
//      //      errorOnGet.message mustBe unknownStorageUnitMsg(id + 1)
//      //
//      //    }
//
//      "successfully create and delete room" in {
//        val makeMyJSon ="""{"type":"Room","name":"UkjentRom2", "sikringSkallsikring": true}"""
//        val response = postStorageNode(makeMyJSon).futureValue
//        val storageNode = Json.parse(response.body).validate[StorageNode].get.asInstanceOf[Room]
//        response.status mustBe 201 //Successfully created the room
//
//        storageNode.id.isDefined mustBe true
//        val id = storageNode.id.get //Just to know which is the current id, the next is supposed to fail....
//
//        val responsDel = deleteStorageUnit(id).futureValue
//        responsDel.status mustBe 200 //Successfully deleted the room
//
//        val responsGet = getStorageNode(id).futureValue
//        responsGet.status mustBe 404 //Shouldn't find a deleted room
//
//        val responsDel2 = deleteStorageUnit(id).futureValue
//        responsDel2.status mustBe 404 //Shouldn't be able to delete a deleted room
//      }
//
//
//      "return a HTTP 404 when trying to delete a node that doesn't exist" in {
//        val responsDel = deleteStorageUnit(12345678).futureValue
//        responsDel.status mustBe 404
//      }
//
//      "fail when trying to update a deleted storage unit" in {
//        val json ="""{"type":"StorageUnit","name":"UkjentUnit"}"""
//        val response = postStorageNode(json).futureValue
//        println("deleted storageNode " + response.body)
//        response.status mustBe 201 //Successfully created the room
//        val storageNode = Json.parse(response.body).validate[StorageNode].get.asInstanceOf[StorageUnit]
//
//        storageNode.id.isDefined mustBe true
//        val id = storageNode.id.get
//
//        val responsDel = deleteStorageUnit(id).futureValue
//        println("deleted storageNode " + responsDel.body)
//        responsDel.status mustBe 200 //Successfully deleted
//
//        val updateJson = s"""{"type":"StorageUnit","id": $id, "name":"NyUkjentUnit"}"""
//        val updateResponse = putStorageNode(id, updateJson).futureValue
//        println("deleted storageNode " + updateResponse.body)
//        updateResponse.status mustBe 404 //Should not be able to update a deleted object
//      }
//
//      "fail when trying to update a deleted room" in {
//        val json ="""{"type":"Room","name":"UkjentRom"}"""
//        val response = postStorageNode(json).futureValue
//        println("deleted room " + response.body)
//        response.status mustBe 201 //Successfully created the room
//        val storageNode = Json.parse(response.body).validate[StorageNode].get.asInstanceOf[Room]
//
//        storageNode.id.isDefined mustBe true
//        val id = storageNode.id.get
//
//        val responsDel = deleteStorageUnit(id).futureValue
//        println("deleted room " + responsDel.body)
//        responsDel.status mustBe 200 //Successfully deleted
//
//        val updateJson = """{"type":"Room","name":"NyttRom", "sikringSkallsikring": true}"""
//        val updateResponse = putStorageNode(id, updateJson).futureValue
//        println("deleted room " + updateResponse.body)
//        updateResponse.status mustBe 404 //Should not be able to update a deleted object
//      }
//
//      "fail when trying to update a deleted building" in {
//        val json ="""{"type":"Building","name":"UkjentBygning"}"""
//        val response = postStorageNode(json).futureValue
//        println("deleted building " + response.body)
//        response.status mustBe 201 //Successfully created the building
//        val storageNode = Json.parse(response.body).validate[StorageNode].get.asInstanceOf[Building]
//
//        storageNode.id.isDefined mustBe true
//        val id = storageNode.id.get
//
//        val responsDel = deleteStorageUnit(id).futureValue
//        println("deleted building " + responsDel.body)
//        responsDel.status mustBe 200 //Successfully deleted
//
//        val updateJson = """{"type":"Building","name":"NyBygning", "address": "OrdentligAdresse"}"""
//        val updateResponse = putStorageNode(id, updateJson).futureValue
//        println("deleted building " + updateResponse.body)
//        updateResponse.status mustBe 404 //Should not be able to update a deleted object
//      }
//
//      // FIXME: Should fail with 400 BadRequest...409 is if you try to add something with the same name.
//      "update should fail (with Conflict=409) if inconsistent storage types" in {
//        val json ="""{"type":"Room","name":"UkjentRom2", "sikringSkallsikring": true}"""
//        val response = postStorageNode(json).futureValue
//        response.status mustBe 201 //Created
//
//        val storageNode = Json.parse(response.body).validate[StorageNode].get.asInstanceOf[Room]
//
//        storageNode.id.isDefined mustBe true
//        val id = storageNode.id.get
//
//        val updatedJson ="""{"type":"Building", "name":"Ukjent bygning", "address":"HelloAddress"}"""
//        val responseUpdate = putStorageNode(id, updatedJson).futureValue
//        responseUpdate.status mustBe 409 //Conflict
//      }
//
//      "create should fail with invalid input data" in {
//        val json ="""{"type":"Room","name":"UkjentRom2", "sikringSkallsikring": 1}"""
//        val response = postStorageNode(json).futureValue
//        response.status mustBe 400
//      }
    }
  }
}
