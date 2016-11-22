/*
 * MUSIT is a museum database to archive natural and cultural history data.
 * Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package services

import models.report.KdReport
import no.uio.musit.security.{AuthenticatedUser, UserInfo}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import org.scalatest.time.{Millis, Seconds, Span}
import utils.testhelpers.NodeGenerators

class KdReportServiceSpec extends MusitSpecWithAppPerSuite with NodeGenerators {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  implicit val dummyUser = AuthenticatedUser(
    userInfo = UserInfo(
      id = defaultUserId,
      secondaryIds = Some(Seq("vader@starwars.com")),
      name = Some("Darth Vader"),
      email = None,
      picture = None
    ),
    groups = Seq.empty
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
