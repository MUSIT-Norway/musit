package no.uio.musit.microservice.storagefacility.resource

import no.uio.musit.microservice.storagefacility.domain.storage.StorageType._
import no.uio.musit.microservice.storagefacility.domain.storage._
import no.uio.musit.microservice.storagefacility.test.StorageNodeJsonGenerator._
import no.uio.musit.microservice.storagefacility.test._
import no.uio.musit.test.MusitSpecWithServerPerSuite
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.http.Status
import play.api.libs.json._
import play.api.libs.ws.WSResponse

class KdReportResourceIntegrationSpec extends MusitSpecWithServerPerSuite {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  def verifyNode[T <: StorageNode](
    response: WSResponse,
    expStorageType: StorageType,
    expName: String,
    expId: Long,
    expPartOf: Option[Long] = None
  )(implicit manifest: Manifest[T]): T = {
    val storageNode = parseAndVerifyResponse[T](response)
    storageNode.id mustBe Some(StorageNodeId(expId))
    storageNode.storageType mustBe expStorageType
    storageNode.isPartOf mustBe expPartOf.map(StorageNodeId.apply)
    storageNode.name mustBe expName
    storageNode mustBe a[T]

    storageNode
  }

  def parseAndVerifyResponse[T](response: WSResponse): T = {
    val json = Json.parse(response.body)
    val parsed = json.validate[StorageNode]
    parsed.isSuccess mustBe true
    parsed.get.asInstanceOf[T]
  }

  "Running the storage facility service" when {
    "interacting with the StorageUnitResource endpoints" should {

      "successfully get kDReport for rooms with different museumId" in {
        val mid = 2
        val json = roomJson("EllensPersonalRoom", None)
        val response = wsUrl(StorageNodesUrl(mid)).post(json).futureValue
        response.status mustBe Status.CREATED
        (response.json \ "areaTo").as[Double] mustBe 21
        (response.json \ "heightTo").as[Double] mustBe 2.6

        val room = verifyNode[Room](
          response, RoomType, "EllensPersonalRoom", 7, None
        )
        room mustBe a[Room]
        room.areaTo mustBe Some(21)
        room.heightTo mustBe Some(2.6)

        val json1 = roomJson("EllensWorkOutRoom", None)
        val response1 = wsUrl(StorageNodesUrl(mid)).post(json1).futureValue
        response1.status mustBe Status.CREATED

        val report = wsUrl(KdReportUrl(mid)).get.futureValue

        (report.json \ "totalArea").as[Double] mustBe 41
        (report.json \ "perimeterSecurity").as[Double] mustBe 41
        (report.json \ "theftProtection").as[Double] mustBe 41
        (report.json \ "fireProtection").as[Double] mustBe 0
        (report.json \ "waterDamageAssessment").as[Double] mustBe 41
        (report.json \ "routinesAndContingencyPlan").as[Double] mustBe 41

      }
      "fail when try to get KdReport from a deleted room" in {
        val mid = 2
        val json = roomJson("EllensPersonalRoom", None)
        val response = wsUrl(StorageNodesUrl(mid)).post(json).futureValue
        response.status mustBe Status.CREATED

        val room1 = verifyNode[Room](
          response, RoomType, "EllensPersonalRoom", 9, None
        )
        room1 mustBe a[Room]
        room1.area mustBe Some(20.5)
        room1.heightTo mustBe Some(2.6)
        room1.id.get.underlying mustBe 9

        val json1 = roomJson("EllensWorkOutRoom", None)
        val response1 = wsUrl(StorageNodesUrl(mid)).post(json1).futureValue
        response1.status mustBe Status.CREATED

        val reportAfterInsertsOfTwoRoom = wsUrl(KdReportUrl(mid)).get.futureValue

        (reportAfterInsertsOfTwoRoom.json \ "totalArea").as[Double] mustBe 82
        (reportAfterInsertsOfTwoRoom.json \ "perimeterSecurity").as[Double] mustBe 82
        (reportAfterInsertsOfTwoRoom.json \ "theftProtection").as[Double] mustBe 82
        (reportAfterInsertsOfTwoRoom.json \ "fireProtection").as[Double] mustBe 0
        (reportAfterInsertsOfTwoRoom.json \ "waterDamageAssessment").as[Double] mustBe 82
        (reportAfterInsertsOfTwoRoom.json \ "routinesAndContingencyPlan").as[Double] mustBe 82

        val roomId = room1.id.get
        val deletedRoom = wsUrl(StorageNodeUrl(mid, roomId)).delete().futureValue
        val reportAfterDeleteOfOneRoom = wsUrl(KdReportUrl(mid)).get.futureValue

        (reportAfterDeleteOfOneRoom.json \ "totalArea").as[Double] mustBe 61.5
        (reportAfterDeleteOfOneRoom.json \ "perimeterSecurity").as[Double] mustBe 61.5
        (reportAfterDeleteOfOneRoom.json \ "theftProtection").as[Double] mustBe 61.5
        (reportAfterDeleteOfOneRoom.json \ "fireProtection").as[Double] mustBe 0
        (reportAfterDeleteOfOneRoom.json \ "waterDamageAssessment").as[Double] mustBe 61.5
        (reportAfterDeleteOfOneRoom.json \ "routinesAndContingencyPlan").as[Double] mustBe 61.5
      }

    }
  }
}
