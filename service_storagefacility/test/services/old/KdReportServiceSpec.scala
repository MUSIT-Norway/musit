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

package services.old

import models.report.KdReport
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues
import services.storage.KdReportService
import utils.testhelpers.NodeGenerators

class KdReportServiceSpec
    extends MusitSpecWithAppPerSuite
    with NodeGenerators
    with MusitResultValues {

  val service: StorageNodeService    = fromInstanceCache[StorageNodeService]
  val reportService: KdReportService = fromInstanceCache[KdReportService]

  "The KdReportService" should {

    val baseNodes  = bootstrapBaseStructure()
    val buildingId = baseNodes.last._1

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

      val expected = KdReport(
        totalArea = 117.6,
        perimeterSecurity = 17,
        theftProtection = 102.6,
        fireProtection = 2.0,
        waterDamageAssessment = 100.6,
        routinesAndContingencyPlan = 115.6
      )

      report.successValue mustBe expected
    }

    "not include deleted room nodes in the report" in {
      val cId = deleteMe.successValue.value.id.value

      val res = service.deleteNode(defaultMuseumId, cId).futureValue

      val reportAfterDelete = reportService.getReport(defaultMuseumId).futureValue
      val expected = KdReport(
        totalArea = 110.1,
        perimeterSecurity = 9.5,
        theftProtection = 102.6,
        fireProtection = 2.0,
        waterDamageAssessment = 100.6,
        routinesAndContingencyPlan = 108.1
      )
      reportAfterDelete.successValue mustBe expected
    }
  }
}
