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
import no.uio.musit.microservice.storagefacility.domain.{Interval, Move, MuseumId}
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
    val mid = 5
    val room = createRoom()
    val inserted = service.addRoom(mid, room).futureValue
    inserted.id must not be None
    inserted.environmentRequirement must not be None
    inserted.environmentRequirement.get mustBe defaultEnvironmentRequirement

    val res = service.getRoomById(mid, inserted.id.get).futureValue
    res.isSuccess mustBe true
    res.get must not be None
    res.get.get.id must not be None
    res.get.get.environmentRequirement must not be None
    res.get.get.environmentRequirement.get mustBe defaultEnvironmentRequirement
  }

  "successfully update a building with new environment requirements" in {
    val mid = 5
    val building = createBuilding()
    val inserted = service.addBuilding(mid, building).futureValue
    inserted.id must not be None
    inserted.environmentRequirement must not be None
    inserted.environmentRequirement.get mustBe defaultEnvironmentRequirement

    val someEnvReq = Some(initEnvironmentRequirement(
      hypoxic = Some(Interval[Double](44.4, Some(55)))
    ))
    val ub = building.copy(environmentRequirement = someEnvReq)

    val res = service.updateBuilding(mid, inserted.id.get, ub).futureValue
    res.isSuccess mustBe true
    res.get must not be None

    val updated = res.get.get
    updated.id mustBe inserted.id
    updated.environmentRequirement mustBe someEnvReq
  }

  "successfully update a storage unit and fetch as StorageNode" in {
    val mid = 5
    val su = createStorageUnit()
    val inserted = service.addStorageUnit(mid, su).futureValue
    inserted.id must not be None

    val res = storageUnitDao.getById(mid, inserted.id.get).futureValue
    res must not be None
    res.get.storageType mustBe su.storageType
    res.get.name mustBe su.name

    val upd = res.get.copy(name = "UggaBugga", areaTo = Some(4.0))

    val updRes = service.updateStorageUnit(mid, res.get.id.get, upd).futureValue
    updRes.isSuccess mustBe true
    updRes.get must not be None
    updRes.get.get.name mustBe "UggaBugga"
    updRes.get.get.areaTo mustBe Some(4.0)

    val again = service.getNodeById(mid, inserted.id.get).futureValue
    again.isSuccess mustBe true
    again.get must not be None
    again.get.get.name mustBe "UggaBugga"
    again.get.get.areaTo mustBe Some(4.0)
  }

  "successfully mark a node as deleted" in {
    val mid = MuseumId(5)
    val su = createStorageUnit()
    val inserted = service.addStorageUnit(mid, su).futureValue
    inserted.id must not be None

    val deleted = service.deleteNode(mid, inserted.id.get).futureValue
    deleted.isSuccess mustBe true

    val notAvailable = service.getNodeById(mid, inserted.id.get).futureValue
    notAvailable.isSuccess mustBe true
    notAvailable.get mustBe None
  }

  "not remove a node that has children" in {
    val mid = MuseumId(5)
    val su1 = createStorageUnit()
    val inserted1 = service.addStorageUnit(mid, su1).futureValue
    inserted1.id must not be None

    val su2 = createStorageUnit(partOf = inserted1.id)
    val inserted2 = service.addStorageUnit(mid, su2).futureValue
    inserted2.id must not be None

    val notDeleted = service.deleteNode(mid, inserted1.id.get).futureValue
    notDeleted.isSuccess mustBe true
    notDeleted.get must not be None
    notDeleted.get.get mustBe -1
  }

  "successfully move a node and all its children" in {
    val mid = MuseumId(5)
    val root1 = service.addRoot(mid, Root()).futureValue
    root1.id must not be None

    val b1 = createBuilding(name = "Building1", partOf = root1.id)
    val building1 = service.addBuilding(mid, b1).futureValue
    building1.id must not be None

    val b2 = createBuilding(name = "Building2", partOf = root1.id)
    val building2 = service.addBuilding(mid, b2).futureValue
    building2.id must not be None

    val su1 = createStorageUnit(name = "Unit1", partOf = building1.id)
    val unit1 = service.addStorageUnit(mid, su1).futureValue
    unit1.id must not be None

    val su2 = createStorageUnit(name = "Unit2", partOf = unit1.id)
    val unit2 = service.addStorageUnit(mid, su2).futureValue
    unit2.id must not be None

    val su3 = createStorageUnit(name = "Unit3", partOf = unit1.id)
    val unit3 = service.addStorageUnit(mid, su3).futureValue
    unit3.id must not be None

    val su4 = createStorageUnit(name = "Unit4", partOf = unit3.id)
    val unit4 = service.addStorageUnit(mid, su4).futureValue
    unit4.id must not be None

    val children = service.getChildren(mid, unit1.id.get).futureValue
    val childIds = children.map(_.id)
    val grandChildIds = childIds.flatMap { id =>
      service.getChildren(mid, id.get).futureValue.map(_.id)
    }

    val mostChildren = childIds ++ grandChildIds

    val move = Move[StorageNodeId](
      doneBy = 123,
      destination = building2.id.get,
      items = Seq(unit1.id.get)
    )

    val event = MoveNode.fromCommand("foobar", move).head

    val m = service.moveNode(mid, unit1.id.get, event).futureValue
    m.isSuccess mustBe true

    mostChildren.map { id =>
      service.getNodeById(mid, id.get).futureValue.map { n =>
        n must not be None
        n.get.path must not be None
        n.get.path.get.path must startWith(building2.path.get.path)
      }
    }

  }

  "Not move a node and all its children with wrong museum" in {
    val mid = MuseumId(5)
    val root1 = service.addRoot(mid, Root()).futureValue
    root1.id must not be None

    val b1 = createBuilding(name = "Building1", partOf = root1.id)
    val building1 = service.addBuilding(mid, b1).futureValue
    building1.id must not be None

    val b2 = createBuilding(name = "Building2", partOf = root1.id)
    val building2 = service.addBuilding(mid, b2).futureValue
    building2.id must not be None

    val su1 = createStorageUnit(name = "Unit1", partOf = building1.id)
    val unit1 = service.addStorageUnit(mid, su1).futureValue
    unit1.id must not be None

    val su2 = createStorageUnit(name = "Unit2", partOf = unit1.id)
    val unit2 = service.addStorageUnit(mid, su2).futureValue
    unit2.id must not be None

    val su3 = createStorageUnit(name = "Unit3", partOf = unit1.id)
    val unit3 = service.addStorageUnit(mid, su3).futureValue
    unit3.id must not be None

    val su4 = createStorageUnit(name = "Unit4", partOf = unit3.id)
    val unit4 = service.addStorageUnit(mid, su4).futureValue
    unit4.id must not be None

    val children = service.getChildren(mid, unit1.id.get).futureValue
    val childIds = children.map(_.id)
    val grandChildIds = childIds.flatMap { id =>
      service.getChildren(mid, id.get).futureValue.map(_.id)
    }

    val mostChildren = childIds ++ grandChildIds

    val move = Move[StorageNodeId](
      doneBy = 123,
      destination = building2.id.get,
      items = Seq(unit1.id.get)
    )

    val event = MoveNode.fromCommand("foobar", move).head
    val wrongMid = MuseumId(4)
    val m = service.moveNode(wrongMid, unit1.id.get, event).futureValue
    m.isSuccess mustBe true

    mostChildren.map { id =>
      service.getNodeById(wrongMid, id.get).futureValue.map { n =>
        n mustBe None
      }
    }
  }
  "not remove a node when childnode has another MuseumId" in {
    val mid = MuseumId(5)
    val su1 = createStorageUnit()
    val inserted1 = service.addStorageUnit(mid, su1).futureValue
    inserted1.id must not be None
    val wrongMid = MuseumId(4)
    val su2 = createStorageUnit(partOf = inserted1.id)
    val inserted2 = service.addStorageUnit(wrongMid, su2).futureValue
    inserted2.id must not be None
    val notDeleted = service.deleteNode(mid, inserted1.id.get).futureValue
    notDeleted.isSuccess mustBe true
    notDeleted.get must not be None
    notDeleted.get.get mustBe -1
  }
  "not remove a node with wrong museumId that has a child " in {
    val mid = MuseumId(5)
    val su1 = createStorageUnit()
    val inserted1 = service.addStorageUnit(mid, su1).futureValue
    inserted1.id must not be None
    val su2 = createStorageUnit(partOf = inserted1.id)
    val inserted2 = service.addStorageUnit(mid, su2).futureValue
    inserted2.id must not be None
    val wrongMid = MuseumId(4)
    val notDeleted = service.deleteNode(wrongMid, inserted1.id.get).futureValue
    notDeleted.isSuccess mustBe true
    notDeleted.get mustBe None
  }

  "not mark a node as deleted when it has wrong museumId" in {
    val mid = MuseumId(5)
    val su = createStorageUnit()
    val inserted = service.addStorageUnit(mid, su).futureValue
    inserted.id must not be None
    val wrongMid = MuseumId(4)
    val deleted = service.deleteNode(wrongMid, inserted.id.get).futureValue
    deleted.isSuccess mustBe true

    val StillAvailable = service.getNodeById(mid, inserted.id.get).futureValue
    StillAvailable.isSuccess mustBe true
    StillAvailable.get.get.id mustBe inserted.id
  }
  "Not update a storage unit and fetch as StorageNode with same data than before" in {
    val mid =  MuseumId(5)
    val su = createStorageUnit()
    val inserted = service.addStorageUnit(mid, su).futureValue
    inserted.id must not be None

    val res = storageUnitDao.getById(mid, inserted.id.get).futureValue
    res must not be None
    res.get.storageType mustBe su.storageType
    res.get.name mustBe su.name

    val upd = res.get.copy(name = "UggaBugga", areaTo = Some(4.0))
    val wrongMid = MuseumId(4)
    val updRes = service.updateStorageUnit(wrongMid, res.get.id.get, upd).futureValue
    updRes.isSuccess mustBe true
    updRes.get must not be None
    updRes.get.get.name mustBe "UggaBugga"
    updRes.get.get.areaTo mustBe Some(4.0)

    val again = service.getNodeById(mid, inserted.id.get).futureValue
    again.isSuccess mustBe true
    again.get must not be None
    //again.get.get.name mustBe "UggaBugga"
    //again.get.get.areaTo mustBe Some(4.0)
  }


  // TODO: MORE TESTING!!!!!

}
