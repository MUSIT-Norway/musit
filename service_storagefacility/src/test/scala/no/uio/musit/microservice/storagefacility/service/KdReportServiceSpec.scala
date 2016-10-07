package no.uio.musit.microservice.storagefacility.service

import no.uio.musit.microservice.storagefacility.domain.report.KdReport
import no.uio.musit.microservice.storagefacility.testhelpers.NodeGenerators
import no.uio.musit.test.MusitSpecWithAppPerSuite
import org.scalatest.time.{Millis, Seconds, Span}

class KdReportServiceSpec extends MusitSpecWithAppPerSuite with NodeGenerators {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  val service: StorageNodeService = fromInstanceCache[StorageNodeService]
  val reportService: KdReportService = fromInstanceCache[KdReportService]

  val mid = 5

  "successfully get a report on storageNode Room" in {
    val room = createRoomWithDifferentArea(1, perimeter = true, theftProtection = true, fireProtection = true)
    val c = createRoomWithDifferentArea(7.5, perimeter = true, routinesAndContingencyPlan = true)
    val c1 = createRoomWithDifferentArea(50.3, theftProtection = true, waterDamage = true, routinesAndContingencyPlan = true)
    service.addRoom(mid, room)("dummyUser").futureValue
    service.addRoom(mid, c)("dummyUser").futureValue
    service.addRoom(mid, c1)("dummyUser").futureValue

    val report = reportService.getReport(mid).futureValue
    report.isSuccess mustBe true
    val reportRes = report.get

    val reportfasit = KdReport(
      totalArea = 58.8,
      perimeterSecurity = 8.5,
      theftProtection = 51.3,
      fireProtection = 1,
      waterDamageAssessment = 50.3,
      routinesAndContingencyPlan = 57.8
    )
    reportRes mustBe reportfasit
  }
}
