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

import no.uio.musit.microservice.storagefacility.domain.storage.{ Root, StorageType }
import no.uio.musit.microservice.storagefacility.testhelpers.NodeGenerators
import no.uio.musit.test.MusitSpecWithAppPerSuite
import org.scalatest.time.{ Millis, Seconds, Span }

class StorageUnitDaoSpec extends MusitSpecWithAppPerSuite with NodeGenerators {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  "StorageUnitDao" should {

    "succeed when inserting several root nodes" in {
      val ins1 = storageUnitDao.insertRoot(Root()).futureValue
      val ins2 = storageUnitDao.insertRoot(Root()).futureValue
      val ins3 = storageUnitDao.insertRoot(Root()).futureValue

      ins1.id.isEmpty must not be true
      ins1.storageType mustBe StorageType.RootType

      ins2.id.isEmpty must not be true
      ins2.storageType mustBe StorageType.RootType

      ins3.id.isEmpty must not be true
      ins3.storageType mustBe StorageType.RootType
    }

    "succeed when inserting a new storage unit" in {
      val inserted = storageUnitDao.insert(createStorageUnit()).futureValue
      inserted.id must not be None
    }

    "successfully fetch a storage unit" in {
      val su = createStorageUnit()
      val inserted = storageUnitDao.insert(su).futureValue
      inserted.id must not be None

      val res = storageUnitDao.getById(inserted.id.get).futureValue
      res must not be None

      res.get.storageType mustBe su.storageType
      res.get.name mustBe su.name
    }

    "successfully update a storage unit and fetch as StorageNode" in {
      val su = createStorageUnit()
      val inserted = storageUnitDao.insert(su).futureValue
      inserted.id must not be None

      val res = storageUnitDao.getById(inserted.id.get).futureValue
      res must not be None
      res.get.storageType mustBe su.storageType
      res.get.name mustBe su.name

      val upd = res.get.copy(name = "UggaBugga", areaTo = Some(4.0))

      val updRes = storageUnitDao.update(res.get.id.get, upd).futureValue
      updRes must not be None
      updRes.get.name mustBe "UggaBugga"
      updRes.get.areaTo mustBe Some(4.0)

      val again = storageUnitDao.getNodeById(inserted.id.get).futureValue
      again must not be None
      again.get.name mustBe "UggaBugga"
      again.get.areaTo mustBe Some(4.0)
    }

    "successfully list root nodes" in {
      val nodes = storageUnitDao.findRootNodes.futureValue
      nodes.size mustBe 3
      nodes.foreach(_.storageType mustBe StorageType.RootType)
    }
  }

  //  "update storage unit with envReq " must {
  //    "without any change in envReq" in {
  //
  //      val testRoom = mkTestRoom
  //      val roomDto = roomToDto(testRoom)
  //      val insertedRoomDto = roomDao.insertRoom(roomDto).futureValue(timeout) //.asInstanceOf[Room]
  //      insertedRoomDto.storageNode.id.isDefined mustBe true
  //      insertedRoomDto.roomDto.theftProtection mustBe Some(true)
  //
  //      val id = insertedRoomDto.storageNode.id.get
  //
  //      val roomToUpdate = testRoom.copy(name = "Room12345")
  //
  //      val firstLatestEnvReqId = insertedRoomDto.storageNode.latestEnvReqId
  //
  //      val roomNodeInDatabase = storageUnitDao.getStorageNodeOnlyById(id).futureValue.get
  //      roomNodeInDatabase.latestEnvReqId mustBe firstLatestEnvReqId
  //
  //      val roomInDatabaseBeforeUpdate = storageDao.getById(id).futureValue.right.get.asInstanceOf[Room]
  //
  //      val res = roomDao.updateRoom(id, roomToUpdate).futureValue(timeout)
  //
  //      val roomInDatabase = storageDao.getById(id).futureValue.right.get.asInstanceOf[Room]
  //      roomInDatabase.environmentRequirement mustBe roomToUpdate.environmentRequirement
  //    }
  //    "with change in envReq, new envReq" in {
  //
  //      val testRoom = mkTestRoom
  //      val roomDto = roomToDto(testRoom)
  //      val insertedRoomDto = roomDao.insertRoom(roomDto).futureValue(timeout) //.asInstanceOf[Room]
  //      insertedRoomDto.storageNode.id.isDefined mustBe true
  //      insertedRoomDto.roomDto.theftProtection mustBe Some(true)
  //
  //      val id = insertedRoomDto.storageNode.id.get
  //
  //      val firstLatestEnvReqId = insertedRoomDto.storageNode.latestEnvReqId
  //
  //      val roomToUpdate = testRoom.copy(name = "Room666", environmentRequirement = Some(mkChangedEnvReq))
  //
  //      val roomNodeInDatabase = storageUnitDao.getStorageNodeOnlyById(id).futureValue.get
  //      roomNodeInDatabase.latestEnvReqId mustBe firstLatestEnvReqId
  //
  //      val roomInDatabaseBeforeUpdate = storageDao.getById(id).futureValue.right.get.asInstanceOf[Room]
  //
  //      val res = roomDao.updateRoom(id, roomToUpdate).futureValue(timeout)
  //
  //      val roomNodeInDatabase2 = storageUnitDao.getStorageNodeOnlyById(id).futureValue.get
  //      roomNodeInDatabase2.latestEnvReqId should not be firstLatestEnvReqId
  //
  //      val roomInDatabase = storageDao.getById(id).futureValue.right.get.asInstanceOf[Room]
  //      roomInDatabase.environmentRequirement mustBe roomToUpdate.environmentRequirement
  //    }
  //    "with new envReq, empty envReq in existing node" in {
  //
  //      val testRoom = mkTestRoomWithNoEnvReq
  //      val roomDto = roomToDto(testRoom)
  //      val insertedRoomDto = roomDao.insertRoom(roomDto).futureValue(timeout) //.asInstanceOf[Room]
  //      insertedRoomDto.storageNode.id.isDefined mustBe true
  //      insertedRoomDto.roomDto.theftProtection mustBe Some(true)
  //
  //      val id = insertedRoomDto.storageNode.id.get
  //
  //      val firstLatestEnvReqId = insertedRoomDto.storageNode.latestEnvReqId
  //
  //      val roomToUpdate = testRoom.copy(name = "Room777", environmentRequirement = Some(mkChangedEnvReq))
  //
  //      val roomNodeInDatabase = storageUnitDao.getStorageNodeOnlyById(id).futureValue.get
  //
  //      val roomInDatabaseBeforeUpdate = storageDao.getById(id).futureValue.right.get.asInstanceOf[Room]
  //
  //      val res = roomDao.updateRoom(id, roomToUpdate).futureValue(timeout)
  //
  //      val roomNodeInDatabase2 = storageUnitDao.getStorageNodeOnlyById(id).futureValue.get
  //      roomNodeInDatabase2.latestEnvReqId should not be firstLatestEnvReqId
  //
  //      val roomInDatabase = storageDao.getById(id).futureValue.right.get.asInstanceOf[Room]
  //      roomInDatabase.environmentRequirement mustBe roomToUpdate.environmentRequirement
  //    }
  //    "with new empty envReq, existing envReq in node" in {
  //
  //      val testRoom = mkTestRoom
  //      val roomDto = roomToDto(testRoom)
  //      val insertedRoomDto = roomDao.insertRoom(roomDto).futureValue(timeout) //.asInstanceOf[Room]
  //      insertedRoomDto.storageNode.id.isDefined mustBe true
  //      insertedRoomDto.roomDto.theftProtection mustBe Some(true)
  //
  //      val id = insertedRoomDto.storageNode.id.get
  //
  //      val firstLatestEnvReqId = insertedRoomDto.storageNode.latestEnvReqId
  //
  //      val roomToUpdate = testRoom.copy(name = "Room888", environmentRequirement = None)
  //
  //      val roomNodeInDatabase = storageUnitDao.getStorageNodeOnlyById(id).futureValue.get
  //
  //      val roomInDatabaseBeforeUpdate = storageDao.getById(id).futureValue.right.get.asInstanceOf[Room]
  //
  //      val res = roomDao.updateRoom(id, roomToUpdate).futureValue(timeout)
  //
  //      val roomNodeInDatabase2 = storageUnitDao.getStorageNodeOnlyById(id).futureValue.get
  //      roomNodeInDatabase2.latestEnvReqId should not be firstLatestEnvReqId
  //
  //      val roomInDatabase = storageDao.getById(id).futureValue.right.get.asInstanceOf[Room]
  //      roomInDatabase.environmentRequirement should not be roomToUpdate.environmentRequirement
  //
  //      roomInDatabase.environmentRequirement mustBe Some(EnvironmentRequirement.empty)
  //    }
  //    "with new empty envReq, and empty envReq in node" in {
  //
  //      val oldSize = storageUnitDao.all().futureValue.size
  //
  //      val testRoom = mkTestRoomWithNoEnvReq
  //      val roomDto = roomToDto(testRoom)
  //      val insertedRoomDto = roomDao.insertRoom(roomDto).futureValue(timeout) //.asInstanceOf[Room]
  //      insertedRoomDto.storageNode.id.isDefined mustBe true
  //      insertedRoomDto.roomDto.theftProtection mustBe Some(true)
  //
  //      val id = insertedRoomDto.storageNode.id.get
  //
  //      val firstLatestEnvReqId = insertedRoomDto.storageNode.latestEnvReqId
  //
  //      val roomToUpdate = testRoom.copy(name = "Room888", environmentRequirement = None)
  //
  //      val roomNodeInDatabase = storageUnitDao.getStorageNodeOnlyById(id).futureValue.get
  //
  //      val roomInDatabaseBeforeUpdate = storageDao.getById(id).futureValue.right.get.asInstanceOf[Room]
  //
  //      val res = roomDao.updateRoom(id, roomToUpdate).futureValue(timeout)
  //
  //      val roomNodeInDatabase2 = storageUnitDao.getStorageNodeOnlyById(id).futureValue.get
  //      roomNodeInDatabase2.latestEnvReqId mustBe firstLatestEnvReqId
  //
  //      val roomInDatabase = storageDao.getById(id).futureValue.right.get.asInstanceOf[Room]
  //      roomInDatabase.environmentRequirement mustBe roomToUpdate.environmentRequirement
  //
  //      firstLatestEnvReqId mustBe None
  //      roomNodeInDatabase2.latestEnvReqId mustBe None
  //
  //      roomInDatabase.environmentRequirement mustBe None
  //      roomToUpdate.environmentRequirement mustBe None
  //
  //      val newSize = storageUnitDao.all().futureValue.size
  //
  //      newSize mustBe oldSize + 1
  //
  //    }
  //
  //  }
}
