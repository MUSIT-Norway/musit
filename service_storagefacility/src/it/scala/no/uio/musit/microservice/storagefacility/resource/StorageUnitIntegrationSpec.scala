package no.uio.musit.microservice.storagefacility.resource

import no.uio.musit.microservice.storagefacility.domain.storage.StorageType._
import no.uio.musit.microservice.storagefacility.domain.storage._
import no.uio.musit.microservice.storagefacility.testdata.StorageNodeJsonGenerator._
import no.uio.musit.test.TestConfigs
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json._
import play.api.libs.ws.WSResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class StorageUnitIntegrationSpec extends PlaySpec
  with OneServerPerSuite
  with ScalaFutures
  with TestConfigs {

  override lazy val port: Int = 19002

  implicit override lazy val app = new GuiceApplicationBuilder()
    .configure(slickWithInMemoryH2(dbName = "storage-unit-it-spec"))
    .build()

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  val veryLongString =
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
      | """.stripMargin.replace('\n', ' ')

  val baseUri = "/v1/storagenodes"

  def postStorageNode(json: JsValue): Future[WSResponse] = {
    wsUrl(baseUri).post(json)
  }

  def putStorageNode(id: Long, json: JsValue): Future[WSResponse] = {
    wsUrl(s"$baseUri/$id").put(json)
  }

  def deleteStorageUnit(id: Long): Future[WSResponse] = {
    wsUrl(s"$baseUri/$id").delete
  }

  def getStorageNode(id: Long): Future[WSResponse] =
    wsUrl(s"$baseUri/$id").get

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

  def verifyNode[T <: StorageNode](
    response: WSResponse,
    expStorageType: StorageType,
    expName: String,
    expId: Long,
    expPartOf: Option[Long] = None
  )(implicit manifest: Manifest[T]): T = {
    val storageNode = parseAndVerifyResponse(response)
    // verifying common attributes across all storage node types
    storageNode.id mustBe Some(StorageNodeId(expId))
    storageNode.storageType mustBe expStorageType
    storageNode.isPartOf mustBe expPartOf.map(StorageNodeId.apply)
    storageNode.name mustBe expName
    storageNode mustBe a[T]

    storageNode.asInstanceOf[T]
  }

  def parseAndVerifyResponse(response: WSResponse): StorageNode = {
    val parsed = Json.parse(response.body).validate[StorageNode]
    parsed.isSuccess mustBe true
    parsed.get
  }


  "Running the storage facility service" when {

    "interacting with the StorageUnitResource endpoints" should {
      "successfully create an organisation node" in {
        val json = organisationJson("My Org1")
        val response = postStorageNode(json).futureValue
        response.status mustBe Status.CREATED

        val organisation = verifyNode[Organisation](
          response, OrganisationType, "My Org1", 1
        )
        organisation mustBe an[Organisation]
      }

      "successfully create a building node" in {
        val json = buildingJson("My Building1", StorageNodeId(1))
        val response = postStorageNode(json).futureValue
        response.status mustBe Status.CREATED

        val building = verifyNode[Building](
          response, BuildingType, "My Building1", 2, Some(1)
        )
        building mustBe a[Building]
      }

      "successfully create a room node" in {
        val json = roomJson("My Room1", StorageNodeId(2))
        val response = postStorageNode(json).futureValue
        response.status mustBe Status.CREATED

        val room = verifyNode[Room](
          response, RoomType, "My Room1", 3, Some(2)
        )
        room mustBe a[Room]
      }

      "successfully create a storage unit node" in {
        val json = storageUnitJson("My Shelf1", StorageNodeId(3))
        val response = postStorageNode(json).futureValue
        response.status mustBe Status.CREATED

        val su = verifyNode[StorageUnit](
          response, StorageUnitType, "My Shelf1", 4, Some(3)
        )
        su mustBe a[StorageUnit]
      }

      "not allow creating a storage node with a name over 500 chars" in {
        val json = storageUnitJson(veryLongString, StorageNodeId(3))
        val response = postStorageNode(json).futureValue

        response.status mustBe Status.BAD_REQUEST
      }

      "not allow creating a building with an address over 500 chars" in {
        val bjs = buildingJson("fail", StorageNodeId(3))
        val json = bjs.as[JsObject] ++ Json.obj("address" -> veryLongString)

        val response = postStorageNode(json).futureValue

        response.status mustBe Status.BAD_REQUEST
      }

      "successfully get an organisation" in {
        val response = getStorageNode(1).futureValue
        response.status mustBe Status.OK

        val organisation = verifyNode[Organisation](
          response, OrganisationType, "My Org1", 1
        )
        organisation mustBe an[Organisation]
      }

      "successfully get a building" in {
        val response = getStorageNode(2).futureValue
        response.status mustBe Status.OK

        val building = verifyNode[Building](
          response, BuildingType, "My Building1", 2, Some(1)
        )
        building mustBe a[Building]
      }

      "successfully get a room" in {
        val response = getStorageNode(3).futureValue
        response.status mustBe Status.OK

        val room = verifyNode[Room](
          response, RoomType, "My Room1", 3, Some(2)
        )
        room mustBe a[Room]
        room.environmentRequirement must not be None
      }

      "successfully get a storage unit" in {
        val response = getStorageNode(4).futureValue
        response.status mustBe Status.OK

        val su = verifyNode[StorageUnit](
          response, StorageUnitType, "My Shelf1", 4, Some(3)
        )
        su mustBe a[StorageUnit]
        su.environmentRequirement must not be None
      }

      "not find a storage node with an invalid Id" in {
        val response = getStorageNode(9999).futureValue
        response.status mustBe Status.NOT_FOUND
      }

      "successfully update a storage unit" in {
        val json = storageUnitJson("My Shelf2", StorageNodeId(3))
        val response = postStorageNode(json).futureValue
        response.status mustBe Status.CREATED
        val su = verifyNode[StorageUnit](
          response, StorageUnitType, "My Shelf2", 5, Some(3)
        )
        su mustBe a[StorageUnit]
        su.areaTo mustBe Some(.5)
        su.heightTo mustBe Some(.6)

        val updatedJson = {
          Json.parse(response.body).as[JsObject] ++ Json.obj(
            "name" -> "My Shelf2b",
            "areaTo" -> JsNumber(.8),
            "heightTo" -> JsNumber(.8)
          )
        }

        val updRes = putStorageNode(su.id.get, updatedJson).futureValue
        updRes.status mustBe Status.OK
        val updated = verifyNode[StorageUnit](
          updRes, StorageUnitType, "My Shelf2b", su.id.get, Some(3)
        )

        updated mustBe a[StorageUnit]
        updated.areaTo mustBe Some(.8)
        updated.heightTo mustBe Some(.8)
      }

      "successfully update a room" in {
        val json = roomJson("My Room2", StorageNodeId(2))
        val response = postStorageNode(json).futureValue
        response.status mustBe Status.CREATED
        val room = verifyNode[Room](
          response, RoomType, "My Room2", 6, Some(2)
        )
        room mustBe a[Room]
        room.areaTo mustBe Some(21.0)
        room.heightTo mustBe Some(2.6)

        val updatedJson = {
          Json.parse(response.body).as[JsObject] ++ Json.obj(
            "name" -> "My Room2b",
            "lightingCondition" -> true
          )
        }

        val updRes = putStorageNode(room.id.get, updatedJson).futureValue
        updRes.status mustBe Status.OK
        val updated = verifyNode[Room](
          updRes, RoomType, "My Room2b", room.id.get, Some(2)
        )

        updated mustBe a[Room]
        updated.environmentAssessment.lightingCondition mustBe Some(true)
      }

      "successfully update a building with environment requirements" in {
        val json = buildingJson("My Building2", StorageNodeId(1))
        val response = postStorageNode(json).futureValue
        response.status mustBe Status.CREATED
        val building = verifyNode[Building](
          response, BuildingType, "My Building2", 7, Some(1)
        )
        building mustBe a[Building]
        building.areaTo mustBe Some(210.0)
        building.heightTo mustBe Some(3.5)

        val updatedJson = {
          Json.parse(response.body).as[JsObject] ++ Json.obj(
            "address" -> "Fjære Åker Øya 21, 2341 Huttiheita, Norge",
            "environmentRequirement" -> Json.parse(envReqJson("Filthy"))
          )
        }

        val updRes = putStorageNode(building.id.get, updatedJson).futureValue
        updRes.status mustBe Status.OK
        val updated = verifyNode[Building](
          updRes, BuildingType, "My Building2", building.id.get, Some(1)
        )

        updated mustBe a[Building]
        updated.address mustBe Some("Fjære Åker Øya 21, 2341 Huttiheita, Norge")
        updated.environmentRequirement.isEmpty must not be true
        updated.environmentRequirement.get.cleaning mustBe Some("Filthy")
      }

      "successfully update an organisation" in {
        val json = organisationJson("My Organisation2")
        val response = postStorageNode(json).futureValue
        response.status mustBe Status.CREATED
        val organisation = verifyNode[Organisation](
          response, OrganisationType, "My Organisation2", 8
        )
        organisation mustBe an[Organisation]
        organisation.areaTo mustBe Some(2100)
        organisation.heightTo mustBe Some(3.5)

        val updatedJson = {
          Json.parse(response.body).as[JsObject] ++ Json.obj(
            "address" -> "Fjære Åker Øya 21, 2341 Huttiheita, Norge"
          )
        }

        val updRes = putStorageNode(organisation.id.get, updatedJson).futureValue
        updRes.status mustBe Status.OK
        val updated = verifyNode[Organisation](
          updRes, OrganisationType, "My Organisation2", organisation.id.get
        )

        updated mustBe an[Organisation]
        updated.address mustBe Some("Fjære Åker Øya 21, 2341 Huttiheita, Norge")
      }

      "respond with 404 when trying to update a node that doesn't exist" in {
        val json = storageUnitJson("Non existent", StorageNodeId(3))

        val failedUpdate = putStorageNode(StorageNodeId(12), json).futureValue
        failedUpdate.status mustBe Status.NOT_FOUND
      }

      "successfully delete a storage node" in {
        val json = storageUnitJson("Remove me", StorageNodeId(3))
        val res = postStorageNode(json).futureValue
        val created = verifyNode[StorageUnit](
          res, StorageUnitType, "Remove me", 9, Some(3)
        )

        created mustBe a[StorageUnit]

        val rmRes = deleteStorageUnit(created.id.get).futureValue
        rmRes.status mustBe Status.OK

        val notFound = getStorageNode(created.id.get).futureValue
        notFound.status mustBe Status.NOT_FOUND
      }

      "respond with 404 when deleting a node that doesn't exist" in {
        val rmRes = deleteStorageUnit(54).futureValue
        rmRes.status mustBe Status.NOT_FOUND
      }

      "respond with 404 when deleting a node that is already deleted" in {
        val rmRes = deleteStorageUnit(9).futureValue
        rmRes.status mustBe Status.NOT_FOUND
      }

      "respond with 404 when updating a node that is deleted" in {
        val json = {
          storageUnitJson("Remove me", StorageNodeId(3)).as[JsObject] ++ Json.obj(
            "id" -> 9,
            "name" -> "Hakuna Matata"
          )
        }

        val failedUpdate = putStorageNode(StorageNodeId(9), json).futureValue
        failedUpdate.status mustBe Status.NOT_FOUND
      }

      "update should fail if type doesn't match" in {
//        val json = buildingJson("My Building10", StorageNodeId(1))
//        val response = postStorageNode(json).futureValue
//        response.status mustBe Status.CREATED
//        val building = verifyNode[Building](
//          response, BuildingType, "My Building10", 10, Some(1)
//        )
//        building mustBe a[Building]
//        building.areaTo mustBe Some(210.0)
//        building.heightTo mustBe Some(3.5)
//
//        val updatedJson = {
//          Json.parse(response.body).as[JsObject] ++ Json.obj(
//            "type" -> "Organisation"
//          )
//        }
//
//        val updRes = putStorageNode(building.id.get, updatedJson).futureValue
//        updRes.status mustBe Status.BAD_REQUEST
//
//        // TODO: This test is pending until it's been clarified if NotFound is
//        // a proper response in this case. I would argue that it is. Since there
//        // is no node in the DB matching the ID with the wrong type
        pending
      }
    }
  }
}
