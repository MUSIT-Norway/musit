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

package no.uio.musit.microservice.storagefacility.dao.storage

import no.uio.musit.microservice.storagefacility.domain.NodePath
import no.uio.musit.microservice.storagefacility.testhelpers.NodeGenerators
import no.uio.musit.test.MusitSpecWithAppPerSuite
import org.scalatest.time.{ Millis, Seconds, Span }
import play.api.Application

class StorageStatsDaoSpec extends MusitSpecWithAppPerSuite with NodeGenerators {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  val statsDao: StorageStatsDao = {
    val instance = Application.instanceCache[StorageStatsDao]
    instance(app)
  }

  "StorageStatsDao" should {

    "return the number of direct child nodes" in {
      val basePath = Some(NodePath(",1,2,3,4,"))

      val inserted = storageUnitDao.insert(createStorageUnit(path = basePath)).futureValue
      inserted.id must not be None
      inserted.path mustBe basePath

      val childPath = basePath.map(_.appendChild(inserted.id.get))

      for (i <- 1 to 10) {
        val n = storageUnitDao.insert(createStorageUnit(partOf = inserted.id, path = childPath)).futureValue
        n.id must not be None
        n.path mustBe childPath
      }

      statsDao.childCount(inserted.id.get).futureValue mustBe 10
    }

    "return the number of objects on a node" in {
      //      val basePath = NodePath(",1,2,3,4,")
      //
      //      val inserted = storageUnitDao.insert(createStorageUnit(path = basePath)).futureValue
      //      inserted.id must not be None
      //      inserted.path mustBe basePath
      //
      //      val childPath = basePath.appendChild(inserted.id.get)

      // TODO: Implement me once move object has been modified to reflect path etc...
      pending
    }

    "return the total number of objects i a node hierarchy" in {
      // TODO: As above...
      pending
    }

  }

}
