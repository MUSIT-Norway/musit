package no.uio.musit.microservice.storagefacility.service

import no.uio.musit.microservice.storagefacility.domain.report.KdReport
import no.uio.musit.microservice.storagefacility.testhelpers.NodeGenerators
import no.uio.musit.test.MusitSpecWithAppPerSuite
import org.scalatest.time.{Millis, Seconds, Span}

class KdReportServiceSpec extends MusitSpecWithAppPerSuite with NodeGenerators {

  implicit val dummyUser = "dummyUser"

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  val service: StorageNodeService = fromInstanceCache[StorageNodeService]
  val reportService: KdReportService = fromInstanceCache[KdReportService]

  "The KdReportService" should {

    val baseNodes = bootstrapBaseStructure()
    val buildingId = baseNodes._3

    val room1 = createRoomWithDifferentArea(
      area = 1,
      perimeter = true,
      theftProtection = true,
      fireProtection = true,
      partOf = Some(buildingId)
    )
    val room2 = createRoomWithDifferentArea(
      area = 7.5,
      perimeter = true,
      routinesAndContingencyPlan = true,
      partOf = Some(buildingId)
    )
    val room3 = createRoomWithDifferentArea(
      area = 50.3,
      theftProtection = true,
      waterDamage = true,
      routinesAndContingencyPlan = true,
      partOf = Some(buildingId)
    )

    // Adding some rooms
    service.addRoom(defaultMuseumId, room1).futureValue
    service.addRoom(defaultMuseumId, room2).futureValue
    service.addRoom(defaultMuseumId, room3).futureValue
    service.addRoom(defaultMuseumId, room1).futureValue
    service.addRoom(defaultMuseumId, room3).futureValue
    val deleteMe = service.addRoom(defaultMuseumId, room2).futureValue

    "successfully get a report on storageNode Room" in {
      val report = reportService.getReport(defaultMuseumId).futureValue
      report.isSuccess mustBe true
      val reportRes = report.get

      val expected = KdReport(
        totalArea = 117.6,
        perimeterSecurity = 17,
        theftProtection = 102.6,
        fireProtection = 2.0,
        waterDamageAssessment = 100.6,
        routinesAndContingencyPlan = 115.6
      )

      reportRes mustBe expected
    }

    "not include deleted room nodes in the report" in {
      val cId = deleteMe.get.get.id.get

      val res = service.deleteNode(defaultMuseumId, cId).futureValue
      res.isSuccess mustBe true

      val reportAfterDelete = reportService.getReport(defaultMuseumId).futureValue
      reportAfterDelete.isSuccess mustBe true
      val reportAfterDeleteRes = reportAfterDelete.get

      val expected = KdReport(
        totalArea = 110.1,
        perimeterSecurity = 9.5,
        theftProtection = 102.6,
        fireProtection = 2.0,
        waterDamageAssessment = 100.6,
        routinesAndContingencyPlan = 108.1
      )
      reportAfterDeleteRes mustBe expected
    }
  }
}
