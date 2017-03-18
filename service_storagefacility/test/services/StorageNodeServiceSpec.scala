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

import no.uio.musit.models.ObjectTypes.CollectionObject
import models.storage.event.EventTypeRegistry.TopLevelEvents.MoveObjectType
import models.storage.event.move.{MoveNode, MoveObject}
import models.storage.Move.MoveNodesCmd
import models.storage.Interval
import models.storage.event.EventType
import models.storage.nodes.StorageUnit
import no.uio.musit.MusitResults.{MusitSuccess, MusitValidationError}
import no.uio.musit.models._
import no.uio.musit.security.{AuthenticatedUser, SessionUUID, UserInfo, UserSession}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues
import org.joda.time.DateTime
import utils.testhelpers.NodeGenerators

class StorageNodeServiceSpec
    extends MusitSpecWithAppPerSuite
    with NodeGenerators
    with MusitResultValues {

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
    val baseIds    = bootstrapBaseStructure()
    val rootId     = baseIds._1
    val orgId      = baseIds._2
    val buildingId = baseIds._3

    "successfully create a new room node with environment requirements" in {
      // Setup new room data, without the partOf relation, which is not
      // interesting in this particular test.
      val room = createRoom(partOf = Some(buildingId))
      val ins  = service.addRoom(defaultMuseumId, room).futureValue
      ins.successValue.value.updatedBy.value mustBe defaultUserId
      ins.successValue.value.updatedDate.value
        .year()
        .get() mustBe DateTime.now().year().get()

      val inserted = ins.successValue.value
      inserted.id must not be None
      inserted.environmentRequirement.value mustBe defaultEnvironmentRequirement

      val res = service.getRoomById(defaultMuseumId, inserted.id.get).futureValue
      res.successValue.value.id must not be None
      res.successValue.value.environmentRequirement.value mustBe defaultEnvironmentRequirement
    }

    "successfully update a building with new environment requirements" in {
      val building = createBuilding(partOf = Some(orgId))
      val ins      = service.addBuilding(defaultMuseumId, building).futureValue
      ins.successValue must not be None

      val inserted = ins.successValue.value
      inserted.id must not be None
      inserted.environmentRequirement.value mustBe defaultEnvironmentRequirement
      inserted.updatedBy.value mustBe defaultUserId
      inserted.updatedDate.value.year().get() mustBe DateTime.now().year().get()

      val someEnvReq = Some(
        initEnvironmentRequirement(
          hypoxic = Some(Interval[Double](44.4, Some(55)))
        )
      )
      val ub = inserted.copy(environmentRequirement = someEnvReq)

      val res = service.updateBuilding(defaultMuseumId, inserted.id.get, ub).futureValue

      val updated = res.successValue.value
      updated.id mustBe inserted.id
      updated.environmentRequirement mustBe someEnvReq
      updated.updatedBy.value mustBe defaultUserId
      updated.updatedDate.value.year().get() mustBe DateTime.now().year().get()
    }

    "successfully update a storage unit and fetch as StorageNode" in {
      val su  = createStorageUnit(partOf = Some(buildingId))
      val ins = service.addStorageUnit(defaultMuseumId, su).futureValue

      val inserted = ins.successValue.value
      inserted.id must not be None
      inserted.updatedBy.value mustBe defaultUserId
      inserted.updatedDate.value.year().get() mustBe DateTime.now().year().get()

      val res = storageUnitDao.getById(defaultMuseumId, inserted.id.get).futureValue

      res.successValue.value.storageType mustBe su.storageType
      res.successValue.value.name mustBe su.name

      val upd = res.successValue.value.copy(name = "UggaBugga", areaTo = Some(4.0))

      val updRes = service
        .updateStorageUnit(defaultMuseumId, res.successValue.value.id.value, upd)
        .futureValue // scalastyle:ignore
      updRes mustBe a[MusitSuccess[_]]
      updRes.successValue.value.name mustBe "UggaBugga"
      updRes.successValue.value.areaTo mustBe Some(4.0)

      val again = service.getNodeById(defaultMuseumId, inserted.id.get).futureValue
      again.successValue.value.name mustBe "UggaBugga"
      again.successValue.value.areaTo mustBe Some(4.0)
      again.successValue.value.updatedBy.value mustBe defaultUserId
      again.successValue.value.updatedDate.value
        .year()
        .get() mustBe DateTime.now().year().get()
    }

    "successfully mark a node as deleted" in {
      val su  = createStorageUnit(partOf = Some(buildingId))
      val ins = service.addStorageUnit(defaultMuseumId, su).futureValue

      val inserted = ins.successValue.value
      inserted.id must not be None

      val deleted =
        service.deleteNode(defaultMuseumId, inserted.id.value).futureValue.successValue

      val notAvailable =
        service.getNodeById(defaultMuseumId, inserted.id.get).futureValue
      notAvailable.successValue mustBe None
    }

    "not remove a node that has children" in {
      val su1  = createStorageUnit(partOf = Some(buildingId))
      val ins1 = service.addStorageUnit(defaultMuseumId, su1).futureValue

      val inserted1 = ins1.successValue.value
      inserted1.id must not be None

      val su2  = createStorageUnit(partOf = inserted1.id)
      val ins2 = service.addStorageUnit(defaultMuseumId, su2).futureValue

      val inserted2 = ins2.successValue.value
      inserted2.id must not be None

      val notDeleted = service.deleteNode(defaultMuseumId, inserted1.id.get).futureValue
      notDeleted.successValue.value mustBe -1
    }

    "successfully move a node and all its children" in {
      // Setup a few nodes...
      val b1        = createBuilding(name = "Building1", partOf = Some(orgId))
      val br1       = service.addBuilding(defaultMuseumId, b1).futureValue
      val building1 = br1.successValue.value
      building1.id must not be None

      val b2        = createBuilding(name = "Building2", partOf = Some(orgId))
      val br2       = service.addBuilding(defaultMuseumId, b2).futureValue
      val building2 = br2.successValue.value
      building2.id must not be None

      val su1   = createStorageUnit(name = "Unit1", partOf = building1.id)
      val u1    = service.addStorageUnit(defaultMuseumId, su1).futureValue
      val unit1 = u1.successValue.value
      unit1.id must not be None

      val su2   = createStorageUnit(name = "Unit2", partOf = unit1.id)
      val u2    = service.addStorageUnit(defaultMuseumId, su2).futureValue
      val unit2 = u2.successValue.value
      unit2.id must not be None

      val su3   = createStorageUnit(name = "Unit3", partOf = unit1.id)
      val u3    = service.addStorageUnit(defaultMuseumId, su3).futureValue
      val unit3 = u3.successValue.value
      unit3.id must not be None

      val su4   = createStorageUnit(name = "Unit4", partOf = unit3.id)
      val u4    = service.addStorageUnit(defaultMuseumId, su4).futureValue
      val unit4 = u4.successValue.value
      unit4.id must not be None

      // Get children of storage unit 1
      val pr       = service.getChildren(defaultMuseumId, unit1.id.get, 1, 10).futureValue
      val children = pr.successValue.matches
      val grandChildren = children.flatMap { c =>
        service
          .getChildren(defaultMuseumId, c.id.value, 1, 10)
          .futureValue
          .successValue
          .matches
      }
      val mostChildren = children ++ grandChildren

      val move = MoveNodesCmd(
        destination = building2.id.value,
        items = Seq(unit1.id.value)
      )

      val event = MoveNode.fromCommand(defaultUserId, move).head

      service
        .moveNodes(defaultMuseumId, building2.id.value, Seq(event))
        .futureValue
        .successValue

      mostChildren.map { c =>
        service.getNodeById(defaultMuseumId, c.id.value).futureValue.map { n =>
          n.value.path must not be None
          n.value.path.path must startWith(building2.path.path)
        }
      }
    }

    "successfully move an object with a previous location" in {
      val oid  = ObjectId(8)
      val dest = StorageNodeDatabaseId(23)

      val loc1 = service.currentObjectLocation(defaultMuseumId, oid).futureValue
      loc1.successValue.value.id mustBe Some(StorageNodeDatabaseId(6))

      val event = MoveObject(
        id = None,
        doneBy = Some(defaultUserId),
        doneDate = DateTime.now,
        affectedThing = Some(oid),
        registeredBy = Some(defaultUserId),
        registeredDate = Some(DateTime.now),
        eventType = EventType.fromEventTypeId(MoveObjectType.id),
        objectType = CollectionObject,
        from = Some(StorageNodeDatabaseId(6)),
        to = dest
      )

      val res =
        service.moveObjects(defaultMuseumId, dest, Seq(event)).futureValue.successValue

      val loc2 = service.currentObjectLocation(defaultMuseumId, oid).futureValue
      loc2.successValue.value.id mustBe Some(StorageNodeDatabaseId(23))
      loc2.successValue.value.pathNames must not be empty
    }

    "not register a move when current location and destination are the same" in {
      val oid  = ObjectId(8)
      val dest = StorageNodeDatabaseId(23)

      val loc1 = service.currentObjectLocation(defaultMuseumId, oid).futureValue
      loc1.successValue.value.id mustBe Some(StorageNodeDatabaseId(23))

      val event = MoveObject(
        id = None,
        doneBy = Some(defaultUserId),
        doneDate = DateTime.now,
        affectedThing = Some(oid),
        registeredBy = Some(defaultUserId),
        registeredDate = Some(DateTime.now),
        eventType = EventType.fromEventTypeId(MoveObjectType.id),
        objectType = CollectionObject,
        from = Some(dest),
        to = dest
      )

      val res = service.moveObjects(defaultMuseumId, dest, Seq(event)).futureValue
      res.isFailure mustBe true

      val loc2 = service.currentObjectLocation(defaultMuseumId, oid).futureValue
      loc2.successValue.value mustBe loc1.successValue.value
    }

    "successfully move an object with no previous location" in {
      val oid  = ObjectId(22)
      val dest = StorageNodeDatabaseId(23)
      val event = MoveObject(
        id = None,
        doneBy = Some(defaultUserId),
        doneDate = DateTime.now,
        affectedThing = Some(oid),
        registeredBy = Some(defaultUserId),
        registeredDate = Some(DateTime.now),
        eventType = EventType.fromEventTypeId(MoveObjectType.id),
        objectType = CollectionObject,
        from = None,
        to = dest
      )
      val res =
        service.moveObjects(defaultMuseumId, dest, Seq(event)).futureValue.successValue

      val loc = service.currentObjectLocation(defaultMuseumId, oid).futureValue
      loc.successValue.value.id mustBe Some(StorageNodeDatabaseId(23))
    }

    "not mark a node as deleted when wrong museumId is used" in {
      val su       = createStorageUnit(partOf = Some(buildingId))
      val ins      = service.addStorageUnit(defaultMuseumId, su).futureValue
      val inserted = ins.successValue.value
      val wrongMid = MuseumId(4)
      service.deleteNode(wrongMid, inserted.id.value).futureValue.successValue

      val stillAv = service.getNodeById(defaultMuseumId, inserted.id.get).futureValue
      stillAv.successValue.value.id mustBe inserted.id
      stillAv.successValue.value.updatedBy.value mustBe defaultUserId
    }

    "not update a storage unit when using the wrong museumId" in {
      val su       = createStorageUnit(partOf = Some(buildingId))
      val ins      = service.addStorageUnit(defaultMuseumId, su).futureValue
      val inserted = ins.successValue.value
      val res      = service.getNodeById(defaultMuseumId, inserted.id.value).futureValue

      val storageUnit = res.successValue.value.asInstanceOf[StorageUnit]
      storageUnit.storageType mustBe su.storageType
      storageUnit.name mustBe su.name
      storageUnit.name must include("FooUnit")
      storageUnit.areaTo mustBe Some(2.0)

      val upd = storageUnit.copy(name = "UggaBugga", areaTo = Some(4.0))

      val wrongMid = MuseumId(4)
      val updRes = service
        .updateStorageUnit(wrongMid, storageUnit.id.value, upd)
        .futureValue // scalastyle:ignore
      updRes.successValue mustBe None

      val again    = service.getNodeById(defaultMuseumId, inserted.id.value).futureValue
      val getAgain = again.successValue.value
      getAgain.name must include("FooUnit")
      getAgain.areaTo mustBe Some(2.0)
      getAgain.updatedBy mustBe Some(defaultUserId)
    }

    "not update a building or environment requirements when using wrong museumID" in {
      val building = createBuilding(partOf = Some(orgId))
      val ins      = service.addBuilding(defaultMuseumId, building).futureValue
      val inserted = ins.successValue.value
      inserted.id must not be None
      inserted.environmentRequirement.value mustBe defaultEnvironmentRequirement
      inserted.address.value must include("Foo")

      val someEnvReq = Some(
        initEnvironmentRequirement(
          hypoxic = Some(Interval[Double](44.4, Some(55)))
        )
      )
      val ub = building.copy(
        environmentRequirement = someEnvReq,
        address = Some("BortIStaurOgVeggAddress")
      )
      val wrongMid = MuseumId(4)
      val res      = service.updateBuilding(wrongMid, inserted.id.get, ub).futureValue
      res.successValue mustBe None

      val orig = service.getBuildingById(defaultMuseumId, inserted.id.get).futureValue
      orig.successValue.value.address.value must include("Foo")
      orig.successValue.value.updatedBy mustBe Some(defaultUserId)
    }

    "not update a room when using wrong museumId" in {
      val room = createRoom(partOf = Some(buildingId))
      val ins  = service.addRoom(defaultMuseumId, room).futureValue

      val inserted = ins.successValue.value
      inserted.id must not be None
      inserted.environmentAssessment.lightingCondition.value mustBe true
      inserted.securityAssessment.waterDamage.value mustBe false
      val secAss   = inserted.securityAssessment.copy(waterDamage = Some(true))
      val uptRoom  = room.copy(securityAssessment = secAss)
      val wrongMid = MuseumId(4)
      val res      = service.updateRoom(wrongMid, inserted.id.value, uptRoom).futureValue
      res.successValue mustBe None

      val orig = service.getRoomById(defaultMuseumId, inserted.id.value).futureValue
      orig.successValue.value.securityAssessment.waterDamage mustBe Some(false)
      orig.successValue.value.updatedBy mustBe Some(defaultUserId)
    }

    "get current location for an object" in {
      val oid = ObjectId(2)
      val aid = ActorId.generate()
      val currLoc =
        service.currentObjectLocation(defaultMuseumId, ObjectId(2)).futureValue
      currLoc.successValue.value.id.value.underlying mustBe 5
      val currIdStr = currLoc.successValue.value.id.value.underlying.toString
      currLoc.successValue.value.path.toString must include(currIdStr)
    }

    "find the relevant rooms when searching with a valid MuseumId" in {
      val searchRoom =
        service.searchByName(defaultMuseumId, "FooRoom", 1, 25).futureValue
      searchRoom.successValue.head.name mustBe "FooRoom"
      searchRoom.successValue.size mustBe 5
    }

    "not find any rooms when searching with the wrong MuseumId" in {
      val theMid    = MuseumId(4)
      val wrongRoom = service.searchByName(theMid, "FooRoom", 1, 25).futureValue
      wrongRoom.successValue.size mustBe 0
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
    val baseIds    = bootstrapBaseStructure()
    val rootId     = baseIds._1
    val orgId      = baseIds._2
    val buildingId = baseIds._3
    // Bootstrap some test strucutures
    val r1 =
      service.addRoom(defaultMuseumId, createRoom(partOf = Some(buildingId))).futureValue
    val room1 = r1.successValue.value

    val r2 =
      service.addRoom(defaultMuseumId, createRoom(partOf = Some(buildingId))).futureValue
    val room2 = r2.successValue.value

    val r3 =
      service.addRoom(defaultMuseumId, createRoom(partOf = Some(buildingId))).futureValue
    val room3 = r3.successValue.value

    val u1 = service
      .addStorageUnit(defaultMuseumId, createStorageUnit(partOf = room1.id))
      .futureValue
    val unit1 = u1.successValue.value

    val u2 = service
      .addStorageUnit(defaultMuseumId, createStorageUnit(partOf = room1.id))
      .futureValue
    val unit2 = u2.successValue.value

    "not be valid when the destination is a child of the current node" in {
      val result =
        service.validatePosition(defaultMuseumId, room1, unit2.path).futureValue
      result mustBe MusitValidationError("Illegal destination")
    }

    "not be valid when the destination is an empty node" in {
      val result =
        service.validatePosition(defaultMuseumId, room1, NodePath.empty).futureValue
      result mustBe MusitValidationError("Illegal move")
    }

    "be valid when the destination is not a child of the current node" in {
      val result =
        service.validatePosition(defaultMuseumId, unit1, room3.path).futureValue
      result mustBe MusitSuccess(())
    }

  }

}
