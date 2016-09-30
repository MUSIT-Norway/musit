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

      "successfully get kDReport for rooms" in {
        val json = roomJson("EllensPersonalRoom", None)
        val response = wsUrl(StorageNodesUrl).post(json).futureValue
        response.status mustBe Status.CREATED

        val room = verifyNode[Room](
          response, RoomType, "EllensPersonalRoom", 1, None
        )
        room mustBe a[Room]
        room.areaTo mustBe Some(21)
        room.heightTo mustBe Some(2.6)

        val json1 = roomJson("EllensWorkOutRoom", None)
        val response1 = wsUrl(StorageNodesUrl).post(json1).futureValue
        response1.status mustBe Status.CREATED

        val report = wsUrl(KdReportUrl).get.futureValue

        println(Json.prettyPrint(report.json))

        (report.json \ "totalArea").as[Int] mustBe 42
        (report.json \ "perimeterSecurity").as[Int] mustBe 42
        (report.json \ "theftProtection").as[Int] mustBe 42
        (report.json \ "fireProtection").as[Int] mustBe 42
        (report.json \ "waterDamageAssessment").as[Int] mustBe 0
        (report.json \ "routinesAndContingencyPlan").as[Int] mustBe 0

      }

    }
  }
}
