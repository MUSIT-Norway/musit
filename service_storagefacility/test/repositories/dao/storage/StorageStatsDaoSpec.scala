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

package repositories.dao.storage

import no.uio.musit.models.{NodePath, StorageNodeId}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.Application
import utils.testhelpers.NodeGenerators

/**
 * ¡¡¡This spec relies on objects being inserted in the evolution script under
 * {{{src/test/resources/evolutions/default/1.sql script.}}}.
 * This is acheived by using relaxed constraints on primary and foreign key
 * references in comparison to the proper schema!!!
 */
class StorageStatsDaoSpec extends MusitSpecWithAppPerSuite with NodeGenerators {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  val statsDao: StorageStatsDao = {
    val instance = Application.instanceCache[StorageStatsDao]
    instance(app)
  }

  val museumId = 2

  "StorageStatsDao" should {

    "return the number of direct child nodes" in {
      val basePath = NodePath(",1,2,3,4,")

      val insId = storageUnitDao.insert(museumId, createStorageUnit(path = basePath)).futureValue
      insId mustBe a[StorageNodeId]

      val childPath = basePath.appendChild(insId)

      for (i <- 1 to 10) {
        val nodeId = storageUnitDao.insert(museumId, createStorageUnit(
          partOf = Some(insId),
          path = childPath
        )).futureValue
        nodeId mustBe a[StorageNodeId]
      }

      statsDao.childCount(insId).futureValue mustBe 10
    }

    "return the number of objects on a node" in {
      statsDao.directObjectCount(StorageNodeId(5)).futureValue mustBe 5
    }

    "return the total number of objects i a node hierarchy" in {
      statsDao.totalObjectCount(NodePath(",1,")).futureValue mustBe 13
    }

  }

}
