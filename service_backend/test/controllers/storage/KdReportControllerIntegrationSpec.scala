package controllers.storage

import models.storage.nodes.StorageType._
import models.storage.nodes.{StorageNode, StorageType}
import no.uio.musit.models.{MuseumId, StorageNodeDatabaseId}
import no.uio.musit.security.BearerToken
import no.uio.musit.test.{FakeUsers, MusitSpecWithServerPerSuite}
import org.scalatest.Inside
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._
import utils.testdata.StorageNodeJsonGenerator._

import scala.util.Try

class KdReportControllerIntegrationSpec extends MusitSpecWithServerPerSuite with Inside {

  def verifyNode[T <: StorageNode](
      response: WSResponse,
      expStorageType: StorageType,
      expName: String,
      expId: Long,
      expPartOf: Option[Long] = None
  )(implicit manifest: Manifest[T]): T = {
    val storageNode = parseAndVerifyResponse[T](response).value
    storageNode.id mustBe Some(StorageNodeDatabaseId(expId))
    storageNode.storageType mustBe expStorageType
    storageNode.isPartOf mustBe expPartOf.map(StorageNodeDatabaseId.apply)
    storageNode.name mustBe expName
    storageNode mustBe a[T]

    storageNode
  }

  def parseAndVerifyResponse[T](response: WSResponse): Option[T] = {
    val json   = Json.parse(response.body)
    val parsed = json.validate[StorageNode]
    parsed.asOpt.map(_.asInstanceOf[T])
  }

  val mid = MuseumId(99)

  // Will be properly initialised in beforeTests method. So any value should do.
  var buildingId: StorageNodeDatabaseId = StorageNodeDatabaseId(9)

  val readToken  = BearerToken(FakeUsers.testUserToken)
  val writeToken = BearerToken(FakeUsers.testWriteToken)
  val adminToken = BearerToken(FakeUsers.testAdminToken)
  val godToken   = BearerToken(FakeUsers.superUserToken)

  override def beforeTests(): Unit = {
    Try {
      val root = wsUrl(RootNodeUrl(mid))
        .withHttpHeaders(godToken.asHeader)
        .post(rootJson("daRoot"))
        .futureValue

      val rootId = (root.json \ "id").asOpt[StorageNodeDatabaseId]

      val org = wsUrl(StorageNodesUrl(mid))
        .withHttpHeaders(godToken.asHeader)
        .post(organisationJson("Hanky", rootId))
        .futureValue

      val orgId = (org.json \ "id").as[StorageNodeDatabaseId]

      val building = wsUrl(StorageNodesUrl(mid))
        .withHttpHeaders(godToken.asHeader)
        .post(buildingJson("Panky", orgId))
        .futureValue

      buildingId = (building.json \ "id").as[StorageNodeDatabaseId]
    }.recover {
      case t: Throwable =>
        println("Error occured when loading data") // scalastyle:ignore
        t.printStackTrace()
    }
  }

  "Running the storage facility service" when {

    "interacting with the StorageUnitResource endpoints" should {

      "successfully get kDReport for rooms in a museum" in {
        val js1 = roomJson("r00m", Some(StorageNodeDatabaseId(buildingId)))
        val res1 = wsUrl(StorageNodesUrl(mid))
          .withHttpHeaders(adminToken.asHeader)
          .post(js1)
          .futureValue
        res1.status mustBe CREATED

        val js2 = roomJson("rUUm", Some(StorageNodeDatabaseId(buildingId)))
        val res2 = wsUrl(StorageNodesUrl(mid))
          .withHttpHeaders(adminToken.asHeader)
          .post(js2)
          .futureValue
        res2.status mustBe CREATED

        val report =
          wsUrl(KdReportUrl(mid)).withHttpHeaders(readToken.asHeader).get().futureValue

        (report.json \ "totalArea").as[Double] mustBe 41
        (report.json \ "perimeterSecurity").as[Double] mustBe 41
        (report.json \ "theftProtection").as[Double] mustBe 41
        (report.json \ "fireProtection").as[Double] mustBe 0
        (report.json \ "waterDamageAssessment").as[Double] mustBe 41
        (report.json \ "routinesAndContingencyPlan").as[Double] mustBe 41

      }

      "fail when try to get KdReport from a deleted room" in {
        val js1 = roomJson("RooM", Some(StorageNodeDatabaseId(buildingId)))
        val res1 = wsUrl(StorageNodesUrl(mid))
          .withHttpHeaders(adminToken.asHeader)
          .post(js1)
          .futureValue
        res1.status mustBe CREATED

        val js2 = roomJson("RuuM", Some(StorageNodeDatabaseId(buildingId)))
        val res2 = wsUrl(StorageNodesUrl(mid))
          .withHttpHeaders(adminToken.asHeader)
          .post(js2)
          .futureValue
        res2.status mustBe CREATED

        val repAfterIns2 =
          wsUrl(KdReportUrl(mid)).withHttpHeaders(readToken.asHeader).get().futureValue

        (repAfterIns2.json \ "totalArea").as[Double] mustBe 82
        (repAfterIns2.json \ "perimeterSecurity").as[Double] mustBe 82
        (repAfterIns2.json \ "theftProtection").as[Double] mustBe 82
        (repAfterIns2.json \ "fireProtection").as[Double] mustBe 0
        (repAfterIns2.json \ "waterDamageAssessment").as[Double] mustBe 82
        (repAfterIns2.json \ "routinesAndContingencyPlan").as[Double] mustBe 82

        val roomId = (res1.json \ "nodeId").as[String]
        val deletedRoom = wsUrl(StorageNodeUrl(mid, roomId))
          .withHttpHeaders(adminToken.asHeader)
          .delete()
          .futureValue
        val repAfterDel1 =
          wsUrl(KdReportUrl(mid)).withHttpHeaders(readToken.asHeader).get().futureValue

        (repAfterDel1.json \ "totalArea").as[Double] mustBe 61.5
        (repAfterDel1.json \ "perimeterSecurity").as[Double] mustBe 61.5
        (repAfterDel1.json \ "theftProtection").as[Double] mustBe 61.5
        (repAfterDel1.json \ "fireProtection").as[Double] mustBe 0
        (repAfterDel1.json \ "waterDamageAssessment").as[Double] mustBe 61.5
        (repAfterDel1.json \ "routinesAndContingencyPlan").as[Double] mustBe 61.5
      }

      "not allow getting the report without READ permission to the museum" in {
        wsUrl(KdReportUrl(6))
          .withHttpHeaders(readToken.asHeader)
          .get()
          .futureValue
          .status mustBe FORBIDDEN
      }

    }
  }
}
