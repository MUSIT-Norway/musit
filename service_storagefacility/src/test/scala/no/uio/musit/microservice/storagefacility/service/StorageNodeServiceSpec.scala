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

import no.uio.musit.microservice.storagefacility.domain.event.move.{MoveNode, MoveObject}
import no.uio.musit.microservice.storagefacility.domain.storage.{StorageNodeId, StorageUnit}
import no.uio.musit.microservice.storagefacility.domain._
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
    val mid = MuseumId(5)
    val room = createRoom()
    val ins = service.addRoom(mid, room).futureValue
    ins.isSuccess mustBe true
    ins.get must not be None

    val inserted = ins.get.get
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
    val mid = MuseumId(5)
    val building = createBuilding()
    val ins = service.addBuilding(mid, building).futureValue
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

    val res = service.updateBuilding(mid, inserted.id.get, ub).futureValue
    res.isSuccess mustBe true
    res.get must not be None

    val updated = res.get.get
    updated.id mustBe inserted.id
    updated.environmentRequirement mustBe someEnvReq
  }

  "successfully update a storage unit and fetch as StorageNode" in {
    val mid = MuseumId(5)
    val su = createStorageUnit()
    val ins = service.addStorageUnit(mid, su).futureValue
    ins.isSuccess mustBe true
    ins.get must not be None

    val inserted = ins.get.get
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
    val ins = service.addStorageUnit(mid, su).futureValue
    ins.isSuccess mustBe true
    ins.get must not be None

    val inserted = ins.get.get
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
    val ins1 = service.addStorageUnit(mid, su1).futureValue
    ins1.isSuccess mustBe true
    ins1.get must not be None

    val inserted1 = ins1.get.get
    inserted1.id must not be None

    val su2 = createStorageUnit(partOf = inserted1.id)
    val ins2 = service.addStorageUnit(mid, su2).futureValue
    ins2.isSuccess mustBe true
    ins2.get must not be None

    val inserted2 = ins2.get.get
    inserted2.id must not be None

    val notDeleted = service.deleteNode(mid, inserted1.id.get).futureValue
    notDeleted.isSuccess mustBe true
    notDeleted.get must not be None
    notDeleted.get.get mustBe -1
  }

  "successfully move a node and all its children" in {
    val mid = MuseumId(5)
    val r1 = service.addRoot(mid).futureValue
    r1.get must not be None
    val root1 = r1.get.get
    root1.id must not be None

    val b1 = createBuilding(name = "Building1", partOf = root1.id)
    val br1 = service.addBuilding(mid, b1).futureValue
    br1.isSuccess mustBe true
    br1.get must not be None
    val building1 = br1.get.get
    building1.id must not be None

    val b2 = createBuilding(name = "Building2", partOf = root1.id)
    val br2 = service.addBuilding(mid, b2).futureValue
    br2.isSuccess mustBe true
    br2.get must not be None
    val building2 = br2.get.get
    building2.id must not be None

    val su1 = createStorageUnit(name = "Unit1", partOf = building1.id)
    val u1 = service.addStorageUnit(mid, su1).futureValue
    u1.isSuccess mustBe true
    u1.get must not be None
    val unit1 = u1.get.get
    unit1.id must not be None

    val su2 = createStorageUnit(name = "Unit2", partOf = unit1.id)
    val u2 = service.addStorageUnit(mid, su2).futureValue
    u2.isSuccess mustBe true
    u2.get must not be None
    val unit2 = u2.get.get
    unit2.id must not be None

    val su3 = createStorageUnit(name = "Unit3", partOf = unit1.id)
    val u3 = service.addStorageUnit(mid, su3).futureValue
    u3.isSuccess mustBe true
    u3.get must not be None
    val unit3 = u3.get.get
    unit3.id must not be None

    val su4 = createStorageUnit(name = "Unit4", partOf = unit3.id)
    val u4 = service.addStorageUnit(mid, su4).futureValue
    u4.isSuccess mustBe true
    u4.get must not be None
    val unit4 = u4.get.get
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
        n.get.path.path must startWith(building2.path.path)
      }
    }
  }

  "UnSuccessfully move a node and all its children with wrong museum" in {
    /* TODO: This test is pending until it's been clarified if this scenario will occur
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

    val AnotherMid = MuseumId(3)
    val su3 = createStorageUnit(name = "Unit3", partOf = unit1.id)
    val unit3 = service.addStorageUnit(AnotherMid, su3).futureValue
    unit3.id must not be None

    val su4 = createStorageUnit(name = "Unit4", partOf = unit3.id)
    val unit4 = service.addStorageUnit(mid, su4).futureValue
    unit4.id must not be None

    val children = service.getChildren(AnotherMid, unit1.id.get).futureValue
    val childIds = children.map(_.id)
    val grandChildIds = childIds.flatMap { id =>
      service.getChildren(AnotherMid, id.get).futureValue.map(_.id)
    }

    val mostChildren = childIds ++ grandChildIds

    val move = Move[StorageNodeId](
      doneBy = 123,
      destination = building2.id.get,
      items = Seq(unit1.id.get)
    )

    val event = MoveNode.fromCommand("foobar", move).head
    val wrongMid = MuseumId(4)
    val m = service.moveNode(mid, unit1.id.get, event).futureValue
    m.isSuccess mustBe true

    mostChildren.map { id =>
      service.getNodeById(AnotherMid, id.get).futureValue.map { n =>
        n must not be None
        println(s"nodenavn: ${n.get.name}")
       // n.get.name must include ("Unit")
      }
    }*/
    pending
  }
  "not remove a node when child node has another MuseumId" in {
    val mid = MuseumId(5)
    val su1 = createStorageUnit()
    val ins1 = service.addStorageUnit(mid, su1).futureValue
    ins1.isSuccess mustBe true
    ins1.get must not be None
    val inserted1 = ins1.get.get
    inserted1.id must not be None

    val anotherMid = MuseumId(4)
    val su2 = createStorageUnit(partOf = inserted1.id)
    val ins2 = service.addStorageUnit(anotherMid, su2).futureValue
    ins2.isSuccess mustBe true
    ins2.get must not be None
    val inserted2 = ins2.get.get
    inserted2.id must not be None

    val maybeDeletedNode = service.deleteNode(mid, inserted1.id.get).futureValue
    maybeDeletedNode.isSuccess mustBe true
    maybeDeletedNode.get must not be None
    maybeDeletedNode.get.get mustBe -1

    val stillNotDeleted = service.getNodeById(mid, inserted1.id.get).futureValue
    stillNotDeleted.get must not be None
    stillNotDeleted.get.get.id mustBe inserted1.id

  }
  "not remove a node with different museumId as input than the node and it's child" in {
    val mid = MuseumId(5)
    val su1 = createStorageUnit()
    val ins1 = service.addStorageUnit(mid, su1).futureValue
    ins1.isSuccess mustBe true
    ins1.get must not be None
    val inserted1 = ins1.get.get
    inserted1.id must not be None

    val su2 = createStorageUnit(partOf = inserted1.id)
    val ins2 = service.addStorageUnit(mid, su2).futureValue
    ins2.isSuccess mustBe true
    ins2.get must not be None
    val inserted2 = ins2.get.get
    inserted2.id must not be None

    val anotherMid = MuseumId(4)
    val maybeDeleted = service.deleteNode(anotherMid, inserted1.id.get).futureValue
    maybeDeleted.isSuccess mustBe true
    maybeDeleted.get mustBe None

    val stillNotDeleted = service.getNodeById(mid, inserted1.id.get).futureValue
    stillNotDeleted.get must not be None
    stillNotDeleted.get.get.id mustBe inserted1.id
  }

  "UnSuccessfully mark a node as deleted when it has wrong museumId" in {
    val mid = MuseumId(5)
    val su = createStorageUnit()
    val ins = service.addStorageUnit(mid, su).futureValue
    ins.isSuccess mustBe true
    ins.get must not be None
    val inserted = ins.get.get
    inserted.id must not be None

    val wrongMid = MuseumId(4)
    val deleted = service.deleteNode(wrongMid, inserted.id.get).futureValue
    deleted.isSuccess mustBe true

    val StillAvailable = service.getNodeById(mid, inserted.id.get).futureValue
    StillAvailable.isSuccess mustBe true
    StillAvailable.get.get.id mustBe inserted.id
  }
  "UnSuccessfully update a storage unit and fetch as StorageNode with same data than before" in {
    val mid = MuseumId(5)
    val su = createStorageUnit()
    val ins = service.addStorageUnit(mid, su).futureValue
    ins.isSuccess mustBe true
    ins.get must not be None
    val inserted = ins.get.get
    inserted.id must not be None

    val res = service.getNodeById(mid, inserted.id.get).futureValue
    val storageUnit = res.get.get.asInstanceOf[StorageUnit]
    storageUnit must not be None
    storageUnit.storageType mustBe su.storageType
    storageUnit.name mustBe su.name
    storageUnit.name must include("FooUnit")
    storageUnit.areaTo mustBe Some(2.0)

    val upd = storageUnit.copy(name = "UggaBugga", areaTo = Some(4.0))

    val wrongMid = MuseumId(4)
    val updRes = service.updateStorageUnit(wrongMid, storageUnit.id.get, upd).futureValue
    updRes.isSuccess mustBe true
    updRes.get mustBe None

    val again = service.getNodeById(mid, inserted.id.get).futureValue
    again.isSuccess mustBe true
    again.get must not be None
    val getAgain = again.get.get
    getAgain.name must include("FooUnit")
    getAgain.areaTo mustBe Some(2.0)
  }
  "UnSuccessfully update a building with new environment requirements and wrong MuseumID" in {
    val mid = MuseumId(5)
    val building = createBuilding()
    val ins = service.addBuilding(mid, building).futureValue
    ins.isSuccess mustBe true
    ins.get must not be None
    val inserted = ins.get.get
    inserted.id must not be None
    inserted.environmentRequirement must not be None
    inserted.environmentRequirement.get mustBe defaultEnvironmentRequirement
    inserted.address.get must include("Foo")

    val someEnvReq = Some(initEnvironmentRequirement(
      hypoxic = Some(Interval[Double](44.4, Some(55)))
    ))
    val ub = building.copy(environmentRequirement = someEnvReq, address = Some("BortIStaurOgVeggAddress"))
    val wrongMid = MuseumId(4)
    val res = service.updateBuilding(wrongMid, inserted.id.get, ub).futureValue
    res.isSuccess mustBe true
    res.get mustBe None

    val oldDataRes = service.getBuildingById(mid, inserted.id.get).futureValue
    oldDataRes.get.get.address.get must include("Foo")
  }
  "UnSuccessfully update a room with wrong or not existing MuseumID" in {
    val mid = MuseumId(5)
    val room = createRoom()
    val ins = service.addRoom(mid, room).futureValue
    ins.isSuccess mustBe true
    ins.get must not be None
    val inserted = ins.get.get
    inserted.id must not be None
    inserted.environmentAssessment.lightingCondition.get mustBe true
    inserted.securityAssessment.waterDamage.get mustBe false
    val secAss = inserted.securityAssessment.copy(waterDamage = Some(true))
    val uptRoom = room.copy(securityAssessment = secAss)
    val wrongMid = MuseumId(4)
    val res = service.updateRoom(wrongMid, inserted.id.get, uptRoom).futureValue
    res.isSuccess mustBe true
    res.get mustBe None

    val oldDataRes = service.getRoomById(mid, inserted.id.get).futureValue
    oldDataRes.get.get.securityAssessment.waterDamage mustBe Some(false)
  }
  "get currentLocation on a object" in {
    val mid = MuseumId(2)
    val oid = ObjectId(2)
    val aid = ActorId(3)
    val currLoc = service.getCurrentObjectLocation(mid, 2).futureValue
    currLoc.isSuccess mustBe true
    currLoc.get.get.id.get.underlying mustBe 4
    currLoc.get.get.path.toString must include(currLoc.get.get.id.get.underlying.toString)

    val moveObject = Move[Long](aid, StorageNodeId(3), Seq(oid))
    val moveSeq = MoveObject.fromCommand("Dummy", moveObject)
    service.moveObject(mid, oid, moveSeq.head).futureValue
    val newCurrLoc = service.getCurrentObjectLocation(mid, 2).futureValue
    newCurrLoc.isSuccess mustBe true
    newCurrLoc.get.get.id.get.underlying mustBe 3
    newCurrLoc.get.get.path.toString must include(newCurrLoc.get.get.id.get.underlying.toString)

    val anotherMid = MuseumId(4)
    val moveSameObject = Move[Long](aid, StorageNodeId(2), Seq(oid))
    val moveSameSeq = MoveObject.fromCommand("Dummy", moveSameObject)
    service.moveObject(anotherMid, oid, moveSameSeq.head).futureValue
    val SameCurrLoc = service.getCurrentObjectLocation(mid, 2).futureValue
    SameCurrLoc.isSuccess mustBe true
    SameCurrLoc.get.get.id.get.underlying mustBe 3
    SameCurrLoc.get.get.path.toString must include(newCurrLoc.get.get.id.get.underlying.toString)

  }
  // TODO: MORE TESTING!!!!!

}
