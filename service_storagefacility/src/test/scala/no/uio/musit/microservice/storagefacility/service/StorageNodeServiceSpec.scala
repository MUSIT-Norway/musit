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

import no.uio.musit.microservice.storagefacility.domain.event.move.MoveNode
import no.uio.musit.microservice.storagefacility.domain.storage.{Root, StorageNodeId}
import no.uio.musit.microservice.storagefacility.domain.{Interval, Move}
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
    val room = createRoom()
    val ins = service.addRoom(room).futureValue
    ins.isSuccess mustBe true
    ins.get must not be None

    val inserted = ins.get.get
    inserted.id must not be None
    inserted.environmentRequirement must not be None
    inserted.environmentRequirement.get mustBe defaultEnvironmentRequirement

    val res = service.getRoomById(inserted.id.get).futureValue
    res.isSuccess mustBe true
    res.get must not be None
    res.get.get.id must not be None
    res.get.get.environmentRequirement must not be None
    res.get.get.environmentRequirement.get mustBe defaultEnvironmentRequirement
  }

  "successfully update a building with new environment requirements" in {
    val building = createBuilding()
    val ins = service.addBuilding(building).futureValue
    ins.isSuccess mustBe true
    ins.get must not be None

    val inserted = ins.get.get
    inserted.id must not be None
    inserted.environmentRequirement must not be None
    inserted.environmentRequirement.get mustBe defaultEnvironmentRequirement

    val someEnvReq = Some(initEnvironmentRequirement(
      hypoxic = Some(Interval[Double](44.4, Some(55)))
    ))
    val ub = inserted.copy(environmentRequirement = someEnvReq)

    val res = service.updateBuilding(inserted.id.get, ub).futureValue
    res.isSuccess mustBe true
    res.get must not be None

    val updated = res.get.get
    updated.id mustBe inserted.id
    updated.environmentRequirement mustBe someEnvReq
  }

  "successfully update a storage unit and fetch as StorageNode" in {
    val su = createStorageUnit()
    val ins = service.addStorageUnit(su).futureValue
    ins.isSuccess mustBe true
    ins.get must not be None

    val inserted = ins.get.get
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

  "successfully mark a node as deleted" in {
    val su = createStorageUnit()
    val ins = service.addStorageUnit(su).futureValue
    ins.isSuccess mustBe true
    ins.get must not be None

    val inserted = ins.get.get
    inserted.id must not be None

    val deleted = service.deleteNode(inserted.id.get).futureValue
    deleted.isSuccess mustBe true

    val notAvailable = service.getNodeById(inserted.id.get).futureValue
    notAvailable.isSuccess mustBe true
    notAvailable.get mustBe None
  }

  "not remove a node that has children" in {
    val su1 = createStorageUnit()
    val ins1 = service.addStorageUnit(su1).futureValue
    ins1.isSuccess mustBe true
    ins1.get must not be None

    val inserted1 = ins1.get.get
    inserted1.id must not be None

    val su2 = createStorageUnit(partOf = inserted1.id)
    val ins2 = service.addStorageUnit(su2).futureValue
    ins2.isSuccess mustBe true
    ins2.get must not be None

    val inserted2 = ins2.get.get
    inserted2.id must not be None

    val notDeleted = service.deleteNode(inserted1.id.get).futureValue
    notDeleted.isSuccess mustBe true
    notDeleted.get must not be None
    notDeleted.get.get mustBe -1
  }

  "successfully move a node and all its children" in {
    val r1 = service.addRoot().futureValue
    r1.isSuccess mustBe true
    r1.get must not be None
    val root1 = r1.get.get
    root1.id must not be None

    val b1 = createBuilding(name = "Building1", partOf = root1.id)
    val br1 = service.addBuilding(b1).futureValue
    br1.isSuccess mustBe true
    br1.get must not be None
    val building1 = br1.get.get
    building1.id must not be None

    val b2 = createBuilding(name = "Building2", partOf = root1.id)
    val br2 = service.addBuilding(b2).futureValue
    br2.isSuccess mustBe true
    br2.get must not be None
    val building2 = br2.get.get
    building2.id must not be None

    val su1 = createStorageUnit(name = "Unit1", partOf = building1.id)
    val u1 = service.addStorageUnit(su1).futureValue
    u1.isSuccess mustBe true
    u1.get must not be None
    val unit1 = u1.get.get
    unit1.id must not be None

    val su2 = createStorageUnit(name = "Unit2", partOf = unit1.id)
    val u2 = service.addStorageUnit(su2).futureValue
    u2.isSuccess mustBe true
    u2.get must not be None
    val unit2 = u2.get.get
    unit2.id must not be None

    val su3 = createStorageUnit(name = "Unit3", partOf = unit1.id)
    val u3 = service.addStorageUnit(su3).futureValue
    u3.isSuccess mustBe true
    u3.get must not be None
    val unit3 = u3.get.get
    unit3.id must not be None

    val su4 = createStorageUnit(name = "Unit4", partOf = unit3.id)
    val u4 = service.addStorageUnit(su4).futureValue
    u4.isSuccess mustBe true
    u4.get must not be None
    val unit4 = u4.get.get
    unit4.id must not be None

    val children = service.getChildren(unit1.id.get).futureValue
    val childIds = children.map(_.id)
    val grandChildIds = childIds.flatMap { id =>
      service.getChildren(id.get).futureValue.map(_.id)
    }

    val mostChildren = childIds ++ grandChildIds

    val move = Move[StorageNodeId](
      doneBy = 123,
      destination = building2.id.get,
      items = Seq(unit1.id.get)
    )

    val event = MoveNode.fromCommand("foobar", move).head

    val m = service.moveNode(unit1.id.get, event).futureValue
    m.isSuccess mustBe true

    mostChildren.map { id =>
      service.getNodeById(id.get).futureValue.map { n =>
        n must not be None
        n.get.path must not be None
        n.get.path.path must startWith(building2.path.path)
      }
    }

  }

  // TODO: MORE TESTING!!!!!

}
