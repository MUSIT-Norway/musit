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

package no.uio.musit.microservice.storagefacility.service

import no.uio.musit.microservice.storagefacility.domain.Interval
import no.uio.musit.microservice.storagefacility.testhelpers.NodeGenerators
import no.uio.musit.test.MusitSpecWithAppPerSuite
import org.scalatest.time.{Millis, Seconds, Span}

class StorageNodeServiceSpec extends MusitSpecWithAppPerSuite with NodeGenerators {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  implicit val DummyUser = "Bevel Lemelisk"

  val service: StorageNodeService = fromInstanceCache[StorageNodeService]

  "successfully create a new room node with environment requirements" in {
    // Setup new room data, without the partOf relation, which is not
    // interesting in this particular test.
    val room = createRoom(None)
    val inserted = service.addRoom(room).futureValue

    inserted.id must not be None
    inserted.environmentRequirement must not be None
    inserted.environmentRequirement.get mustBe defaultEnvironmentRequirement
  }

  "successfully update a building with new environment requirements" in {
    val building = createBuilding(None)
    val inserted = service.addBuilding(building).futureValue
    inserted.id must not be None
    inserted.environmentRequirement must not be None
    inserted.environmentRequirement.get mustBe defaultEnvironmentRequirement

    val someEnvReq = Some(initEnvironmentRequirement(
      hypoxic = Some(Interval[Double](44.4, Some(55)))
    ))
    val ub = building.copy(environmentRequirement = someEnvReq)

    val res = service.updateBuilding(inserted.id.get, ub).futureValue
    res.isSuccess mustBe true
    res.get must not be None

    val updated = res.get.get
    updated.id mustBe inserted.id
    updated.environmentRequirement mustBe someEnvReq
  }

  "successfully update a storage unit and fetch as StorageNode" in {
    val su = createStorageUnit()
    val inserted = service.addStorageUnit(su).futureValue
    inserted.id must not be None

    val res = storageUnitDao.getById(inserted.id.get).futureValue
    res must not be None
    res.get.storageType mustBe su.storageType
    res.get.name mustBe su.name

    val upd = res.get.copy(name = "UggaBugga", areaTo = Some(4.0))

    val updRes = service.updateStorageUnit(res.get.id.get, upd).futureValue
    updRes.isSuccess mustBe true
    updRes.get must not be None
    updRes.get.get.name mustBe "UggaBugga"
    updRes.get.get.areaTo mustBe Some(4.0)

    val again = service.getNodeById(inserted.id.get).futureValue
    again.isSuccess mustBe true
    again.get must not be None
    again.get.get.name mustBe "UggaBugga"
    again.get.get.areaTo mustBe Some(4.0)
  }

}
