package no.uio.musit.microservice.storagefacility.resource

import no.uio.musit.microservice.storagefacility.domain.MuseumId
import no.uio.musit.microservice.storagefacility.domain.storage.StorageType._
import no.uio.musit.microservice.storagefacility.domain.storage._
import no.uio.musit.microservice.storagefacility.test.StorageNodeJsonGenerator._
import no.uio.musit.microservice.storagefacility.test._
import no.uio.musit.test.MusitSpecWithServerPerSuite
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.http.Status
import play.api.libs.json._
import play.api.libs.ws.WSResponse

import scala.util.Try

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

  val mid = MuseumId(2)

  // Will be properly initialised in beforeTests method. So any value should do.
  var buildingId: StorageNodeId = StorageNodeId(9)

  override def beforeTests(): Unit = {
    Try {
      val root = wsUrl(RootNodeUrl(mid)).post(JsNull).futureValue
      val rootId = (root.json \ "id").asOpt[StorageNodeId]
      val org = wsUrl(StorageNodesUrl(mid)).post(organisationJson("Hanky", rootId)).futureValue
      val orgId = (org.json \ "id").as[StorageNodeId]
      val building = wsUrl(StorageNodesUrl(mid)).post(buildingJson("Panky", orgId)).futureValue
      buildingId = (building.json \ "id").as[StorageNodeId]
    }.recover {
      case t: Throwable =>
        println("Error occured when loading data") // scalastyle:ignore
    }
  }

  "Running the storage facility service" when {

    "interacting with the StorageUnitResource endpoints" should {

      "successfully get kDReport for rooms with different museumId" in {
        val js1 = roomJson("r00m", Some(StorageNodeId(buildingId)))
        val res1 = wsUrl(StorageNodesUrl(mid)).post(js1).futureValue
        res1.status mustBe Status.CREATED

        val js2 = roomJson("rUUm", Some(StorageNodeId(buildingId)))
        val res2 = wsUrl(StorageNodesUrl(mid)).post(js2).futureValue
        res2.status mustBe Status.CREATED

        val report = wsUrl(KdReportUrl(mid)).get.futureValue

        (report.json \ "totalArea").as[Double] mustBe 41
        (report.json \ "perimeterSecurity").as[Double] mustBe 41
        (report.json \ "theftProtection").as[Double] mustBe 41
        (report.json \ "fireProtection").as[Double] mustBe 0
        (report.json \ "waterDamageAssessment").as[Double] mustBe 41
        (report.json \ "routinesAndContingencyPlan").as[Double] mustBe 41

      }

      "fail when try to get KdReport from a deleted room" in {
        val js1 = roomJson("RooM", Some(StorageNodeId(buildingId)))
        val res1 = wsUrl(StorageNodesUrl(mid)).post(js1).futureValue
        res1.status mustBe Status.CREATED

        val js2 = roomJson("RuuM", Some(StorageNodeId(buildingId)))
        val res2 = wsUrl(StorageNodesUrl(mid)).post(js2).futureValue
        res2.status mustBe Status.CREATED

        val reportAfterInsertsOfTwoRoom = wsUrl(KdReportUrl(mid)).get.futureValue

        (reportAfterInsertsOfTwoRoom.json \ "totalArea").as[Double] mustBe 82
        (reportAfterInsertsOfTwoRoom.json \ "perimeterSecurity").as[Double] mustBe 82
        (reportAfterInsertsOfTwoRoom.json \ "theftProtection").as[Double] mustBe 82
        (reportAfterInsertsOfTwoRoom.json \ "fireProtection").as[Double] mustBe 0
        (reportAfterInsertsOfTwoRoom.json \ "waterDamageAssessment").as[Double] mustBe 82
        (reportAfterInsertsOfTwoRoom.json \ "routinesAndContingencyPlan").as[Double] mustBe 82

        val roomId = (res1.json \ "id").as[Long]
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
