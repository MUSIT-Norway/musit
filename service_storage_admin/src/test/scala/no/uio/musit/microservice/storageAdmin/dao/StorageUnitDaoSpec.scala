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

package no.uio.musit.microservice.storageAdmin.dao

import no.uio.musit.microservice.storageAdmin.domain._
import no.uio.musit.microservice.storageAdmin.domain.dto._
import no.uio.musit.microservices.common.PlayTestDefaults
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent.duration._
import org.scalatest.Matchers._
import _root_.no.uio.musit.microservice.storageAdmin.service.StatsService

class StorageUnitDaoSpec extends PlaySpec with OneAppPerSuite with ScalaFutures with StorageDtoConverter {

  implicit override lazy val app = new GuiceApplicationBuilder()
    .configure(PlayTestDefaults.inMemoryDatabaseConfig())
    .build()

  val storageUnitDao: StorageUnitDao = {
    val instance = Application.instanceCache[StorageUnitDao]
    instance(app)
  }

  val roomDao: RoomDao = {
    val instance = Application.instanceCache[RoomDao]
    instance(app)
  }

  val buildingDao: BuildingDao = {
    val instance = Application.instanceCache[BuildingDao]
    instance(app)
  }

  val storageDao: StorageDao = {
    val instance = Application.instanceCache[StorageDao]
    instance(app)
  }

  val statsService: StatsService = {
    val instance = Application.instanceCache[StatsService]
    instance(app)
  }

  val timeout = PatienceConfiguration.Timeout(5 seconds)

  "Interacting with the StorageUnitDao" when {

    "setting isPartOf for a StorageUnit" should {
      "succeed" in {

        val oldSize = storageUnitDao.all().futureValue.size
        val storageNode = storageUnitDao.insertStorageUnit(CompleteStorageUnitDto(StorageNodeDTO(None, "C2",
          None, None, None, None, None, parentPath = NodePath.empty, None, None, None, None, None, isDeleted = false, StorageType.StorageUnit), None)).futureValue(timeout)
        storageUnitDao.insertStorageUnit(CompleteStorageUnitDto(StorageNodeDTO(None, "C2",
          None, None, None, None, None, parentPath = NodePath.empty, None, None, None, None, None, isDeleted = false, StorageType.StorageUnit), None)).futureValue(timeout)
        val result = storageUnitDao.all().futureValue
        result.size mustBe (2 + oldSize)
        storageUnitDao.setPartOf(1, 2).futureValue mustBe 1
        import scala.concurrent.ExecutionContext.Implicits.global
        storageUnitDao.getStorageNodeOnlyById(1).map(_.get.isPartOf).futureValue mustBe Some(2)
      }
    }
  }

  def emptyRoom(name: String) =
    Room(
      id = None,
      name = name,
      area = None,
      areaTo = None,
      height = None,
      heightTo = None,
      isPartOf = None,
      groupRead = None,
      groupWrite = None,
      links = None,
      environmentRequirement = None,
      securityAssessment = SecurityAssessment.empty,
      environmentAssessment = EnvironmentAssessment.empty
    )

  def mkTestEnvReq = EnvironmentRequirement(
    temperature = Some(10),
    temperatureTolerance = Some(1),
    hypoxicAir = Some(5),
    hypoxicAirTolerance = Some(0.5),
    relativeHumidity = Some(10),
    relativeHumidityTolerance = Some(0.1),
    cleaning = Some("cleaning"),
    lightingCondition = Some("lighting condition"),
    comments = Some("comments")
  )

  def mkChangedEnvReq = EnvironmentRequirement(
    temperature = Some(11),
    temperatureTolerance = Some(12),
    hypoxicAir = Some(5),
    hypoxicAirTolerance = Some(0.7),
    relativeHumidity = Some(199),
    relativeHumidityTolerance = Some(0.9),
    cleaning = Some("cleaning - not good"),
    lightingCondition = Some("lighting condition - too dark"),
    comments = Some("comments - any comments")
  )

  def mkTestRoom = emptyRoom("test room").copy(
    environmentRequirement = Some(mkTestEnvReq),
    securityAssessment = SecurityAssessment.empty.copy(theftProtection = Some(true))
  )

  def mkTestRoomWithNoEnvReq = emptyRoom("test room").copy(
    securityAssessment = SecurityAssessment.empty.copy(theftProtection = Some(true))
  )


  private def insertRoom(roomDto: CompleteRoomDto, parentNode: Option[Long] = None): CompleteRoomDto = {
    val room = roomDao.insertRoom(roomDto).futureValue(timeout)

    parentNode.foreach { parentNodeId =>
      storageUnitDao.setPartOf(room.id.get, parentNodeId).futureValue mustBe 1
    }
    room
  }

  //private def insertRoom(room: Room, parentNode: Option[Long] = None): CompleteRoomDto = insertRoom(roomToDto(room), parentNode)

  private def insertBuilding(buildingDto: CompleteBuildingDto, parentNode: Option[Long] = None): CompleteBuildingDto = {
    val building = buildingDao.insertBuilding(buildingDto).futureValue(timeout)

    parentNode.foreach { parentNodeId =>
      storageUnitDao.setPartOf(building.id.get, parentNodeId).futureValue mustBe 1
    }
    building
  }

  private def insertBuilding(building: Building): CompleteBuildingDto = insertBuilding(buildingToDto(building))

  "update storage unit with envReq " must {
    "without any change in envReq" in {

      val testRoom = mkTestRoom
      val roomDto = roomToDto(testRoom)
      val insertedRoomDto = insertRoom(roomDto) //.asInstanceOf[Room]
      insertedRoomDto.storageNode.id.isDefined mustBe true
      insertedRoomDto.roomDto.theftProtection mustBe Some(true)

      val id = insertedRoomDto.storageNode.id.get

      val roomToUpdate = testRoom.copy(name = "Room12345")

      val firstLatestEnvReqId = insertedRoomDto.storageNode.latestEnvReqId

      val roomNodeInDatabase = storageUnitDao.getStorageNodeOnlyById(id).futureValue.get
      roomNodeInDatabase.latestEnvReqId mustBe firstLatestEnvReqId

      val roomInDatabaseBeforeUpdate = storageDao.getById(id).futureValue.right.get.asInstanceOf[Room]

      val res = roomDao.updateRoom(id, roomToUpdate).futureValue(timeout)

      val roomInDatabase = storageDao.getById(id).futureValue.right.get.asInstanceOf[Room]
      roomInDatabase.environmentRequirement mustBe roomToUpdate.environmentRequirement
    }
    "with change in envReq, new envReq" in {

      val testRoom = mkTestRoom
      val roomDto = roomToDto(testRoom)
      val insertedRoomDto = insertRoom(roomDto) //.asInstanceOf[Room]
      insertedRoomDto.storageNode.id.isDefined mustBe true
      insertedRoomDto.roomDto.theftProtection mustBe Some(true)

      val id = insertedRoomDto.storageNode.id.get

      val firstLatestEnvReqId = insertedRoomDto.storageNode.latestEnvReqId

      val roomToUpdate = testRoom.copy(name = "Room666", environmentRequirement = Some(mkChangedEnvReq))

      val roomNodeInDatabase = storageUnitDao.getStorageNodeOnlyById(id).futureValue.get
      roomNodeInDatabase.latestEnvReqId mustBe firstLatestEnvReqId

      val roomInDatabaseBeforeUpdate = storageDao.getById(id).futureValue.right.get.asInstanceOf[Room]

      val res = roomDao.updateRoom(id, roomToUpdate).futureValue(timeout)

      val roomNodeInDatabase2 = storageUnitDao.getStorageNodeOnlyById(id).futureValue.get
      roomNodeInDatabase2.latestEnvReqId should not be firstLatestEnvReqId

      val roomInDatabase = storageDao.getById(id).futureValue.right.get.asInstanceOf[Room]
      roomInDatabase.environmentRequirement mustBe roomToUpdate.environmentRequirement
    }
    "with new envReq, empty envReq in existing node" in {

      val testRoom = mkTestRoomWithNoEnvReq
      val roomDto = roomToDto(testRoom)
      val insertedRoomDto = insertRoom(roomDto) //.asInstanceOf[Room]
      insertedRoomDto.storageNode.id.isDefined mustBe true
      insertedRoomDto.roomDto.theftProtection mustBe Some(true)

      val id = insertedRoomDto.storageNode.id.get

      val firstLatestEnvReqId = insertedRoomDto.storageNode.latestEnvReqId

      val roomToUpdate = testRoom.copy(name = "Room777", environmentRequirement = Some(mkChangedEnvReq))

      val roomNodeInDatabase = storageUnitDao.getStorageNodeOnlyById(id).futureValue.get

      val roomInDatabaseBeforeUpdate = storageDao.getById(id).futureValue.right.get.asInstanceOf[Room]

      val res = roomDao.updateRoom(id, roomToUpdate).futureValue(timeout)

      val roomNodeInDatabase2 = storageUnitDao.getStorageNodeOnlyById(id).futureValue.get
      roomNodeInDatabase2.latestEnvReqId should not be firstLatestEnvReqId

      val roomInDatabase = storageDao.getById(id).futureValue.right.get.asInstanceOf[Room]
      roomInDatabase.environmentRequirement mustBe roomToUpdate.environmentRequirement
    }
    "with new empty envReq, existing envReq in node" in {

      val testRoom = mkTestRoom
      val roomDto = roomToDto(testRoom)
      val insertedRoomDto = insertRoom(roomDto) //.asInstanceOf[Room]
      insertedRoomDto.storageNode.id.isDefined mustBe true
      insertedRoomDto.roomDto.theftProtection mustBe Some(true)

      val id = insertedRoomDto.storageNode.id.get

      val firstLatestEnvReqId = insertedRoomDto.storageNode.latestEnvReqId

      val roomToUpdate = testRoom.copy(name = "Room888", environmentRequirement = None)

      val roomNodeInDatabase = storageUnitDao.getStorageNodeOnlyById(id).futureValue.get

      val roomInDatabaseBeforeUpdate = storageDao.getById(id).futureValue.right.get.asInstanceOf[Room]

      val res = roomDao.updateRoom(id, roomToUpdate).futureValue(timeout)

      val roomNodeInDatabase2 = storageUnitDao.getStorageNodeOnlyById(id).futureValue.get
      roomNodeInDatabase2.latestEnvReqId should not be firstLatestEnvReqId

      val roomInDatabase = storageDao.getById(id).futureValue.right.get.asInstanceOf[Room]
      roomInDatabase.environmentRequirement should not be roomToUpdate.environmentRequirement

      roomInDatabase.environmentRequirement mustBe Some(EnvironmentRequirement.empty)
    }
    "with new empty envReq, and empty envReq in node" in {

      val oldSize = storageUnitDao.all().futureValue.size

      val testRoom = mkTestRoomWithNoEnvReq
      val roomDto = roomToDto(testRoom)
      val insertedRoomDto = insertRoom(roomDto) //.asInstanceOf[Room]
      insertedRoomDto.storageNode.id.isDefined mustBe true
      insertedRoomDto.roomDto.theftProtection mustBe Some(true)

      val id = insertedRoomDto.storageNode.id.get

      val firstLatestEnvReqId = insertedRoomDto.storageNode.latestEnvReqId

      val roomToUpdate = testRoom.copy(name = "Room888", environmentRequirement = None)

      val roomNodeInDatabase = storageUnitDao.getStorageNodeOnlyById(id).futureValue.get

      val roomInDatabaseBeforeUpdate = storageDao.getById(id).futureValue.right.get.asInstanceOf[Room]

      val res = roomDao.updateRoom(id, roomToUpdate).futureValue(timeout)

      val roomNodeInDatabase2 = storageUnitDao.getStorageNodeOnlyById(id).futureValue.get
      roomNodeInDatabase2.latestEnvReqId mustBe firstLatestEnvReqId

      val roomInDatabase = storageDao.getById(id).futureValue.right.get.asInstanceOf[Room]
      roomInDatabase.environmentRequirement mustBe roomToUpdate.environmentRequirement

      firstLatestEnvReqId mustBe None
      roomNodeInDatabase2.latestEnvReqId mustBe None

      roomInDatabase.environmentRequirement mustBe None
      roomToUpdate.environmentRequirement mustBe None

      val newSize = storageUnitDao.all().futureValue.size

      newSize mustBe oldSize + 1

    }

  }



  "Statistics test " must {
    "without any change in envReq" in {


      val room1 = insertRoom(roomToDto(emptyRoom("MittRom")))
      val room2 = insertRoom(roomToDto(emptyRoom("MittUnderRom")), room1.id)

      statsService.subNodeCount(room1.id.get).futureValue mustBe Right(1)

      statsService.subNodeCount(room2.id.get).futureValue mustBe Right(0)

      val unknownId = 9999999
      val unknownRes = statsService.subNodeCount(9999999).futureValue
      unknownRes mustBe Left(storageUnitDao.storageUnitNotFoundError(unknownId))

    }
  }
}
