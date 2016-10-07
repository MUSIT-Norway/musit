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

        val json1 = roomJson("EllensWorkOutRoom", None)
        val response1 = wsUrl(StorageNodesUrl(mid)).post(json1).futureValue
        response1.status mustBe Status.CREATED

        val report = wsUrl(KdReportUrl(mid)).get.futureValue

        (report.json \ "totalArea").as[Int] mustBe 42
        (report.json \ "perimeterSecurity").as[Int] mustBe 42
        (report.json \ "theftProtection").as[Int] mustBe 42
        (report.json \ "fireProtection").as[Int] mustBe 42
        (report.json \ "waterDamageAssessment").as[Int] mustBe 0
        (report.json \ "routinesAndContingencyPlan").as[Int] mustBe 0

        val anotherMid = MuseumId(4)
        val json2 = roomJson("EllensLivingRoom", None)
        val response2 = wsUrl(StorageNodesUrl(anotherMid)).post(json2).futureValue
        response2.status mustBe Status.CREATED
        val anotherMuseum = wsUrl(KdReportUrl(anotherMid)).get.futureValue
        anotherMuseum.status mustBe Status.OK
        (anotherMuseum.json \ "totalArea").as[Int] mustBe 21
        (anotherMuseum.json \ "perimeterSecurity").as[Int] mustBe 21
        (anotherMuseum.json \ "theftProtection").as[Int] mustBe 21
        (anotherMuseum.json \ "fireProtection").as[Int] mustBe 21
        (anotherMuseum.json \ "waterDamageAssessment").as[Int] mustBe 0
        (anotherMuseum.json \ "routinesAndContingencyPlan").as[Int] mustBe 0

        val wrongMid = MuseumId(6)
        val wrongMuseum = wsUrl(KdReportUrl(wrongMid)).get.futureValue
        wrongMuseum.status mustBe Status.OK
        (wrongMuseum.json \ "totalArea").as[Int] mustBe 0
      }

    }
  }
}
