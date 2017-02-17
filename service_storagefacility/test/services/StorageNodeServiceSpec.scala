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

import models.event.EventType
import models.event.EventTypeRegistry.TopLevelEvents.MoveObjectType
import models.event.move.{MoveNode, MoveObject}
import models.storage.StorageUnit
import models.{Interval, Move}
import no.uio.musit.models.{ActorId, MuseumId, ObjectId, StorageNodeDatabaseId}
import no.uio.musit.security.{AuthenticatedUser, SessionUUID, UserInfo, UserSession}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import org.joda.time.DateTime
import utils.testhelpers.NodeGenerators

class StorageNodeServiceSpec extends MusitSpecWithAppPerSuite with NodeGenerators {

  implicit val dummyUser = AuthenticatedUser(
    session = UserSession(uuid = SessionUUID.generate()),
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

  "Using the StorageNodeService API" must {

    // Initialize base data
    val baseIds = bootstrapBaseStructure()
    val rootId = baseIds._1
    val orgId = baseIds._2
    val buildingId = baseIds._3

    "successfully create a new room node with environment requirements" in {
      // Setup new room data, without the partOf relation, which is not
      // interesting in this particular test.
      val room = createRoom(partOf = Some(buildingId))
      val ins = service.addRoom(defaultMuseumId, room).futureValue
      ins.isSuccess mustBe true
      ins.get must not be None
      ins.get.get.updatedBy.get mustBe defaultUserId
      ins.get.get.updatedDate.get.year().get() mustBe DateTime.now().year().get()

      val inserted = ins.get.get
      inserted.id must not be None
      inserted.environmentRequirement must not be None
      inserted.environmentRequirement.get mustBe defaultEnvironmentRequirement

      val res = service.getRoomById(defaultMuseumId, inserted.id.get).futureValue
      res.isSuccess mustBe true
      res.get must not be None
      res.get.get.id must not be None
      res.get.get.environmentRequirement must not be None
      res.get.get.environmentRequirement.get mustBe defaultEnvironmentRequirement
    }

    "successfully update a building with new environment requirements" in {
      val building = createBuilding(partOf = Some(orgId))
      val ins = service.addBuilding(defaultMuseumId, building).futureValue
      ins.isSuccess mustBe true
      ins.get must not be None

      val inserted = ins.get.get
      inserted.id must not be None
      inserted.environmentRequirement must not be None
      inserted.environmentRequirement.get mustBe defaultEnvironmentRequirement
      inserted.updatedBy.get mustBe defaultUserId
      inserted.updatedDate.get.year().get() mustBe DateTime.now().year().get()

      val someEnvReq = Some(initEnvironmentRequirement(
        hypoxic = Some(Interval[Double](44.4, Some(55)))
      ))
      val ub = inserted.copy(environmentRequirement = someEnvReq)

      val res = service.updateBuilding(defaultMuseumId, inserted.id.get, ub).futureValue
      res.isSuccess mustBe true
      res.get must not be None

      val updated = res.get.get
      updated.id mustBe inserted.id
      updated.environmentRequirement mustBe someEnvReq
      updated.updatedBy.get mustBe defaultUserId
      updated.updatedDate.get.year().get() mustBe DateTime.now().year().get()
    }

    "successfully update a storage unit and fetch as StorageNode" in {
      val su = createStorageUnit(partOf = Some(buildingId))
      val ins = service.addStorageUnit(defaultMuseumId, su).futureValue
      ins.isSuccess mustBe true
      ins.get must not be None

      val inserted = ins.get.get
      inserted.id must not be None
      inserted.updatedBy.get mustBe defaultUserId
      inserted.updatedDate.get.year().get() mustBe DateTime.now().year().get()

      val res = storageUnitDao.getById(defaultMuseumId, inserted.id.get).futureValue
      res must not be None
      res.get.storageType mustBe su.storageType
      res.get.name mustBe su.name

      val upd = res.get.copy(name = "UggaBugga", areaTo = Some(4.0))

      val updRes = service.updateStorageUnit(defaultMuseumId, res.get.id.get, upd).futureValue
      updRes.isSuccess mustBe true
      updRes.get must not be None
      updRes.get.get.name mustBe "UggaBugga"
      updRes.get.get.areaTo mustBe Some(4.0)

      val again = service.getNodeById(defaultMuseumId, inserted.id.get).futureValue
      again.isSuccess mustBe true
      again.get must not be None
      again.get.get.name mustBe "UggaBugga"
      again.get.get.areaTo mustBe Some(4.0)
      again.get.get.updatedBy.get mustBe defaultUserId
      again.get.get.updatedDate.get.year().get() mustBe DateTime.now().year().get()
    }

    "successfully mark a node as deleted" in {
      val su = createStorageUnit(partOf = Some(buildingId))
      val ins = service.addStorageUnit(defaultMuseumId, su).futureValue
      ins.isSuccess mustBe true
      ins.get must not be None

      val inserted = ins.get.get
      inserted.id must not be None

      val deleted = service.deleteNode(defaultMuseumId, inserted.id.get).futureValue
      deleted.isSuccess mustBe true

      val notAvailable = service.getNodeById(defaultMuseumId, inserted.id.get).futureValue
      notAvailable.isSuccess mustBe true
      notAvailable.get mustBe None
    }

    "not remove a node that has children" in {
      val su1 = createStorageUnit(partOf = Some(buildingId))
      val ins1 = service.addStorageUnit(defaultMuseumId, su1).futureValue
      ins1.isSuccess mustBe true
      ins1.get must not be None

      val inserted1 = ins1.get.get
      inserted1.id must not be None

      val su2 = createStorageUnit(partOf = inserted1.id)
      val ins2 = service.addStorageUnit(defaultMuseumId, su2).futureValue
      ins2.isSuccess mustBe true
      ins2.get must not be None

      val inserted2 = ins2.get.get
      inserted2.id must not be None

      val notDeleted = service.deleteNode(defaultMuseumId, inserted1.id.get).futureValue
      notDeleted.isSuccess mustBe true
      notDeleted.get must not be None
      notDeleted.get.get mustBe -1
    }

    "successfully move a node and all its children" in {
      // Setup a few nodes...
      val b1 = createBuilding(name = "Building1", partOf = Some(orgId))
      val br1 = service.addBuilding(defaultMuseumId, b1).futureValue
      br1.isSuccess mustBe true
      br1.get must not be None
      val building1 = br1.get.get
      building1.id must not be None

      val b2 = createBuilding(name = "Building2", partOf = Some(orgId))
      val br2 = service.addBuilding(defaultMuseumId, b2).futureValue
      br2.isSuccess mustBe true
      br2.get must not be None
      val building2 = br2.get.get
      building2.id must not be None

      val su1 = createStorageUnit(name = "Unit1", partOf = building1.id)
      val u1 = service.addStorageUnit(defaultMuseumId, su1).futureValue
      u1.isSuccess mustBe true
      u1.get must not be None
      val unit1 = u1.get.get
      unit1.id must not be None

      val su2 = createStorageUnit(name = "Unit2", partOf = unit1.id)
      val u2 = service.addStorageUnit(defaultMuseumId, su2).futureValue
      u2.isSuccess mustBe true
      u2.get must not be None
      val unit2 = u2.get.get
      unit2.id must not be None

      val su3 = createStorageUnit(name = "Unit3", partOf = unit1.id)
      val u3 = service.addStorageUnit(defaultMuseumId, su3).futureValue
      u3.isSuccess mustBe true
      u3.get must not be None
      val unit3 = u3.get.get
      unit3.id must not be None

      val su4 = createStorageUnit(name = "Unit4", partOf = unit3.id)
      val u4 = service.addStorageUnit(defaultMuseumId, su4).futureValue
      u4.isSuccess mustBe true
      u4.get must not be None
      val unit4 = u4.get.get
      unit4.id must not be None

      // Get children of storage unit 1
      val pr = service.getChildren(defaultMuseumId, unit1.id.get, 1, 10).futureValue
      val children = pr.matches
      val grandChildren = children.flatMap { c =>
        service.getChildren(defaultMuseumId, c.id.get, 1, 10).futureValue.matches
      }
      val mostChildren = children ++ grandChildren

      val move = Move[StorageNodeDatabaseId](
        destination = building2.id.get,
        items = Seq(unit1.id.get)
      )

      val event = MoveNode.fromCommand(defaultUserId, move).head

      val m = service.moveNodes(defaultMuseumId, building2.id.get, Seq(event)).futureValue
      m.isSuccess mustBe true

      mostChildren.map { c =>
        service.getNodeById(defaultMuseumId, c.id.get).futureValue.map { n =>
          n must not be None
          n.get.path must not be None
          n.get.path.path must startWith(building2.path.path)
        }
      }
    }

    "successfully move an object with a previous location" in {
      val oid = ObjectId(8)
      val dest = StorageNodeDatabaseId(23)

      val loc1 = service.currentObjectLocation(defaultMuseumId, oid).futureValue
      loc1.isSuccess mustBe true
      loc1.get must not be None
      loc1.get.get.id mustBe Some(StorageNodeDatabaseId(6))

      val event = MoveObject(
        id = None,
        doneBy = Some(defaultUserId),
        doneDate = DateTime.now,
        affectedThing = Some(oid),
        registeredBy = Some(defaultUserId),
        registeredDate = Some(DateTime.now),
        eventType = EventType.fromEventTypeId(MoveObjectType.id),
        from = Some(StorageNodeDatabaseId(6)),
        to = dest
      )

      val res = service.moveObjects(defaultMuseumId, dest, Seq(event)).futureValue
      res.isSuccess mustBe true

      val loc2 = service.currentObjectLocation(defaultMuseumId, oid).futureValue
      loc2.isSuccess mustBe true
      loc2.get must not be empty
      loc2.get.get.id mustBe Some(StorageNodeDatabaseId(23))
      loc2.get.get.pathNames must not be empty
    }

    "not register a move when current location and destination are the same" in {
      val oid = ObjectId(8)
      val dest = StorageNodeDatabaseId(23)

      val loc1 = service.currentObjectLocation(defaultMuseumId, oid).futureValue
      loc1.isSuccess mustBe true
      loc1.get must not be None
      loc1.get.get.id mustBe Some(StorageNodeDatabaseId(23))

      val event = MoveObject(
        id = None,
        doneBy = Some(defaultUserId),
        doneDate = DateTime.now,
        affectedThing = Some(oid),
        registeredBy = Some(defaultUserId),
        registeredDate = Some(DateTime.now),
        eventType = EventType.fromEventTypeId(MoveObjectType.id),
        from = Some(dest),
        to = dest
      )

      val res = service.moveObjects(defaultMuseumId, dest, Seq(event)).futureValue
      res.isFailure mustBe true

      val loc2 = service.currentObjectLocation(defaultMuseumId, oid).futureValue
      loc2.isSuccess mustBe true
      loc2.get must not be None
      loc2.get.get mustBe loc1.get.get
    }

    "successfully move an object with no previous location" in {
      val oid = ObjectId(22)
      val dest = StorageNodeDatabaseId(23)
      val event = MoveObject(
        id = None,
        doneBy = Some(defaultUserId),
        doneDate = DateTime.now,
        affectedThing = Some(oid),
        registeredBy = Some(defaultUserId),
        registeredDate = Some(DateTime.now),
        eventType = EventType.fromEventTypeId(MoveObjectType.id),
        from = None,
        to = dest
      )
      val res = service.moveObjects(defaultMuseumId, dest, Seq(event)).futureValue
      res.isSuccess mustBe true

      val loc = service.currentObjectLocation(defaultMuseumId, oid).futureValue
      loc.isSuccess mustBe true
      loc.get must not be None
      loc.get.get.id mustBe Some(StorageNodeDatabaseId(23))
    }

    "not mark a node as deleted when wrong museumId is used" in {
      val su = createStorageUnit(partOf = Some(buildingId))
      val ins = service.addStorageUnit(defaultMuseumId, su).futureValue
      ins.isSuccess mustBe true
      ins.get must not be None
      val inserted = ins.get.get
      inserted.id must not be None

      val wrongMid = MuseumId(4)
      val deleted = service.deleteNode(wrongMid, inserted.id.get).futureValue
      deleted.isSuccess mustBe true

      val stillAv = service.getNodeById(defaultMuseumId, inserted.id.get).futureValue
      stillAv.isSuccess mustBe true
      stillAv.get.get.id mustBe inserted.id
      stillAv.get.get.updatedBy.get mustBe defaultUserId
    }

    "not update a storage unit when using the wrong museumId" in {
      val su = createStorageUnit(partOf = Some(buildingId))
      val ins = service.addStorageUnit(defaultMuseumId, su).futureValue
      ins.isSuccess mustBe true
      ins.get must not be None
      val inserted = ins.get.get
      inserted.id must not be None

      val res = service.getNodeById(defaultMuseumId, inserted.id.get).futureValue
      val storageUnit = res.get.get.asInstanceOf[StorageUnit]
      storageUnit must not be None
      storageUnit.storageType mustBe su.storageType
      storageUnit.name mustBe su.name
      storageUnit.name must include("FooUnit")
      storageUnit.areaTo mustBe Some(2.0)

      val upd = storageUnit.copy(name = "UggaBugga", areaTo = Some(4.0))

      val wrongMid = MuseumId(4)
      val updRes = service.updateStorageUnit(wrongMid, storageUnit.id.get, upd).futureValue // scalastyle:ignore
      updRes.isSuccess mustBe true
      updRes.get mustBe None

      val again = service.getNodeById(defaultMuseumId, inserted.id.get).futureValue
      again.isSuccess mustBe true
      again.get must not be None
      val getAgain = again.get.get
      getAgain.name must include("FooUnit")
      getAgain.areaTo mustBe Some(2.0)
      getAgain.updatedBy mustBe Some(defaultUserId)
    }

    "not update a building or environment requirements when using wrong museumID" in {
      val building = createBuilding(partOf = Some(orgId))
      val ins = service.addBuilding(defaultMuseumId, building).futureValue
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
      val ub = building.copy(
        environmentRequirement = someEnvReq,
        address = Some("BortIStaurOgVeggAddress")
      )
      val wrongMid = MuseumId(4)
      val res = service.updateBuilding(wrongMid, inserted.id.get, ub).futureValue
      res.isSuccess mustBe true
      res.get mustBe None

      val orig = service.getBuildingById(defaultMuseumId, inserted.id.get).futureValue
      orig.get.get.address.get must include("Foo")
      orig.get.get.updatedBy mustBe Some(defaultUserId)
    }

    "not update a room when using wrong museumId" in {
      val room = createRoom(partOf = Some(buildingId))
      val ins = service.addRoom(defaultMuseumId, room).futureValue
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

      val orig = service.getRoomById(defaultMuseumId, inserted.id.get).futureValue
      orig.get.get.securityAssessment.waterDamage mustBe Some(false)
      orig.get.get.updatedBy mustBe Some(defaultUserId)
    }

    "get current location for an object" in {
      val oid = ObjectId(2)
      val aid = ActorId.generate()
      val currLoc = service.currentObjectLocation(defaultMuseumId, 2).futureValue
      currLoc.isSuccess mustBe true
      currLoc.get.get.id.get.underlying mustBe 5
      val currIdStr = currLoc.get.get.id.get.underlying.toString
      currLoc.get.get.path.toString must include(currIdStr)
    }

    "find the relevant rooms when searching with a valid MuseumId" in {
      val searchRoom = service.searchByName(defaultMuseumId, "FooRoom", 1, 25).futureValue
      searchRoom.isSuccess mustBe true
      searchRoom.get.head.name mustBe "FooRoom"
      searchRoom.get.size mustBe 5
    }

    "not find any rooms when searching with the wrong MuseumId" in {
      val theMid = MuseumId(4)
      val wrongRoom = service.searchByName(theMid, "FooRoom", 1, 25).futureValue
      wrongRoom.isSuccess mustBe true
      wrongRoom.get.size mustBe 0

    }

    "fail when searching for a room with no search criteria" in {
      val noSearchCriteria = service.searchByName(defaultMuseumId, "", 1, 25).futureValue
      noSearchCriteria.isSuccess mustBe false
    }

    "fail when searching for a room with less than 3 characters" in {
      val searchRoom = service.searchByName(defaultMuseumId, "Fo", 1, 25).futureValue
      searchRoom.isSuccess mustBe false
    }
  }

  "Validating a storage node destination" should {
    val baseIds = bootstrapBaseStructure()
    val rootId = baseIds._1
    val orgId = baseIds._2
    val buildingId = baseIds._3
    // Bootstrap some test strucutures
    val r1 = service.addRoom(defaultMuseumId, createRoom(partOf = Some(buildingId))).futureValue
    r1.isSuccess mustBe true
    r1.get must not be None
    val room1 = r1.get.get

    val r2 = service.addRoom(defaultMuseumId, createRoom(partOf = Some(buildingId))).futureValue
    r2.isSuccess mustBe true
    r2.get must not be None
    val room2 = r2.get.get

    val r3 = service.addRoom(defaultMuseumId, createRoom(partOf = Some(buildingId))).futureValue
    r3.isSuccess mustBe true
    r3.get must not be None
    val room3 = r3.get.get

    val u1 = service.addStorageUnit(defaultMuseumId, createStorageUnit(partOf = room1.id)).futureValue
    u1.isSuccess mustBe true
    u1.get must not be None
    val unit1 = u1.get.get

    val u2 = service.addStorageUnit(defaultMuseumId, createStorageUnit(partOf = room1.id)).futureValue
    u2.isSuccess mustBe true
    u2.get must not be None
    val unit2 = u2.get.get

    "not be valid when the destination is a child of the current node" in {
      service.isValidPosition(defaultMuseumId, room1, unit2.path).futureValue mustBe false
    }

    "be valid when the destination is not a child of the current node" in {
      service.isValidPosition(defaultMuseumId, unit1, room3.path).futureValue mustBe true
    }

  }

}
