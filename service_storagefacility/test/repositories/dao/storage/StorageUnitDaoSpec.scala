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

import models.storage.StorageType._
import models.storage.{Root, StorageType}
import no.uio.musit.models.{MuseumId, NodePath, StorageNodeDatabaseId, StorageNodeId}
import no.uio.musit.service.MusitResults.MusitSuccess
import no.uio.musit.test.MusitSpecWithAppPerSuite
import org.joda.time.DateTime
import org.scalatest.time.{Millis, Seconds, Span}
import utils.testhelpers.NodeGenerators

class StorageUnitDaoSpec extends MusitSpecWithAppPerSuite with NodeGenerators {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  "StorageUnitDao" should {

    "succeed when inserting several root nodes" in {

      def createRoot(name: String): Root = Root(
        nodeId = StorageNodeId.generateAsOpt(),
        name = name,
        updatedBy = Some(defaultUserId),
        updatedDate = Some(DateTime.now())
      )

      for (i <- 7 to 9) {
        val r = createRoot(s"root$i")
        val insId = storageUnitDao.insertRoot(defaultMuseumId, r).futureValue
        insId mustBe a[StorageNodeDatabaseId]
        insId mustBe StorageNodeDatabaseId(i.toLong)
      }
      val anotherMid = MuseumId(4)
      for (i <- 10 to 12) {
        val r = createRoot(s"root$i")
        val insId = storageUnitDao.insertRoot(anotherMid, r).futureValue
        insId mustBe a[StorageNodeDatabaseId]
        insId mustBe StorageNodeDatabaseId(i.toLong)
      }
    }

    "succeed when inserting a new storage unit" in {
      val path = NodePath(",1,2,3,4,")
      val insId = storageUnitDao.insert(defaultMuseumId, createStorageUnit(path = path)).futureValue
      insId mustBe a[StorageNodeDatabaseId]
    }

    "successfully fetch a storage unit" in {
      val mid = MuseumId(5)
      val su = createStorageUnit()
      val insId = storageUnitDao.insert(mid, su).futureValue
      insId mustBe a[StorageNodeDatabaseId]

      val res = storageUnitDao.getById(mid, insId).futureValue
      res must not be None

      res.get.storageType mustBe su.storageType
      res.get.name mustBe su.name
    }

    "successfully update a storage unit and fetch as StorageNode" in {
      val mid = MuseumId(5)
      val su = createStorageUnit()
      val insId = storageUnitDao.insert(mid, su).futureValue
      insId mustBe a[StorageNodeDatabaseId]

      val res = storageUnitDao.getById(mid, insId).futureValue
      res must not be None
      res.get.storageType mustBe su.storageType
      res.get.name mustBe su.name

      val upd = res.get.copy(name = "UggaBugga", areaTo = Some(4.0))

      val updRes = storageUnitDao.update(mid, res.get.id.get, upd).futureValue
      updRes mustBe a[MusitSuccess[_]]
      updRes.get must not be None
      updRes.get.get mustBe 1

      val again = storageUnitDao.getById(mid, insId).futureValue
      again must not be None
      again.get.name mustBe "UggaBugga"
      again.get.areaTo mustBe Some(4.0)
    }

    "successfully list root nodes" in {
      val nodes = storageUnitDao.findRootNodes(defaultMuseumId).futureValue
      nodes.foreach(_.storageType mustBe StorageType.RootType)
    }

    "fail to list root nodes when museumId is wrong" in {
      val mid = MuseumId(5)
      val nodes = storageUnitDao.findRootNodes(mid).futureValue
      nodes.size mustBe 0
      nodes.foreach(_.storageType mustBe StorageType.RootType)
    }
    "fail to list root nodes with museumId that does not exists" in {
      val mid = MuseumId(55)
      val nodes = storageUnitDao.findRootNodes(mid).futureValue
      nodes.size mustBe 0
      nodes.foreach(_.storageType mustBe StorageType.RootType)
    }

    "successfully mark a node as deleted" in {
      val su = createStorageUnit()
      val insId = storageUnitDao.insert(defaultMuseumId, su).futureValue
      insId mustBe a[StorageNodeDatabaseId]

      val deleted = storageUnitDao.markAsDeleted(defaultUserId, defaultMuseumId, insId).futureValue // scalastyle:ignore
      deleted.isSuccess mustBe true
      deleted.get mustBe 1

      val res = storageUnitDao.getById(defaultMuseumId, insId).futureValue
      res mustBe None
    }

    "successfully fetch the named path elements for a storage node" in {
      val path1 = NodePath(",7,17,")
      val su1 = createStorageUnit(
        partOf = Some(StorageNodeDatabaseId(7)),
        path = path1
      ).copy(name = "node1")
      val insId1 = storageUnitDao.insert(defaultMuseumId, su1).futureValue
      insId1 mustBe a[StorageNodeDatabaseId]
      insId1 mustBe StorageNodeDatabaseId(17)

      val path2 = path1.appendChild(StorageNodeDatabaseId(18))
      val su2 = createStorageUnit(
        partOf = Some(insId1),
        path = path2
      ).copy(name = "node2")
      val insId2 = storageUnitDao.insert(defaultMuseumId, su2).futureValue
      insId2 mustBe a[StorageNodeDatabaseId]
      insId2 mustBe StorageNodeDatabaseId(18)

      val res = storageUnitDao.namesForPath(path2).futureValue
      res must not be empty
      res.size mustBe 3
      res.head.nodeId mustBe StorageNodeDatabaseId(7)
      res.head.name mustBe "root7"
      res.tail.head.nodeId mustBe StorageNodeDatabaseId(17)
      res.tail.head.name mustBe "node1"
      res.last.nodeId mustBe StorageNodeDatabaseId(18)
      res.last.name mustBe "node2"
    }

    "fail to fetch a storage unit with wrong museumId" in {
      val mid = MuseumId(5)
      val su = createStorageUnit()
      val insId = storageUnitDao.insert(mid, su).futureValue
      insId mustBe a[StorageNodeDatabaseId]

      val wrongMid = MuseumId(4)
      val res = storageUnitDao.getById(mid, insId).futureValue
      res must not be None

      res.get.storageType mustBe su.storageType
      res.get.name mustBe su.name
    }

    "fail to update a storage unit when museumId is wrong" in {
      val mid = MuseumId(5)
      val su = createStorageUnit()
      val insId = storageUnitDao.insert(mid, su).futureValue
      insId mustBe a[StorageNodeDatabaseId]

      val res = storageUnitDao.getById(mid, insId).futureValue
      res must not be None
      res.get.storageType mustBe su.storageType
      res.get.name must include("FooUnit")
      res.get.name mustBe su.name
      res.get.areaTo mustBe Some(2.0)

      val upd = res.get.copy(name = "UggaBugga", areaTo = Some(4.0))

      val anotherMid = MuseumId(4)
      val updRes = storageUnitDao.update(anotherMid, res.get.id.get, upd).futureValue
      updRes.isSuccess mustBe true
      updRes.get mustBe None

      val again = storageUnitDao.getById(mid, insId).futureValue
      again must not be None
      again.get.name mustBe "FooUnit"
      again.get.areaTo mustBe Some(2.0)
    }

    "fail to mark a node as deleted when museumId is wrong" in {
      val su = createStorageUnit()
      val insId = storageUnitDao.insert(defaultMuseumId, su).futureValue
      insId mustBe a[StorageNodeDatabaseId]

      val anotherMid = MuseumId(4)
      val deleted = storageUnitDao.markAsDeleted(defaultUserId, anotherMid, insId).futureValue // scalastyle:ignore
      deleted.isFailure mustBe true

      val res = storageUnitDao.getById(defaultMuseumId, insId).futureValue
      res.get.id mustBe Some(insId)
    }

    "fetch tuples of StorageNodeId and StorageType for a NodePath" in {
      val orgPath = NodePath(",1,22,")
      val org = createOrganisation(
        partOf = Some(StorageNodeDatabaseId(1)),
        path = orgPath
      ).copy(name = "node-x")
      val organisationId = organisationDao.insert(defaultMuseumId, org).futureValue

      val buildingPath = NodePath(",1,22,23,")
      val building = createBuilding(
        partOf = Some(organisationId),
        path = buildingPath
      )
      val buildingId = buildingDao.insert(defaultMuseumId, building).futureValue

      val roomPath = NodePath(",1,22,23,24,")
      val room = createRoom(
        partOf = Some(buildingId),
        path = roomPath
      )
      val roomId = roomDao.insert(defaultMuseumId, room).futureValue

      val su1Path = NodePath(",1,22,23,24,25,")
      val su1 = createStorageUnit(
        partOf = Some(roomId),
        path = su1Path
      )
      val suId = storageUnitDao.insert(defaultMuseumId, su1).futureValue

      val expected = Seq(
        StorageNodeDatabaseId(1) -> RootType,
        organisationId -> OrganisationType,
        buildingId -> BuildingType,
        roomId -> RoomType,
        suId -> StorageUnitType
      )

      val tuples = storageUnitDao.getStorageTypesInPath(defaultMuseumId, su1Path).futureValue

      tuples must contain theSameElementsInOrderAs expected
    }
    "successfully get a node when searching for name and not if it's wrong museumId" in {
      val mid = MuseumId(5)
      val getNodeName = storageUnitDao.getStorageNodeByName(mid, "Foo", 1, 25).futureValue
      getNodeName.size mustBe 3
      getNodeName.head.name must include("Foo")
      getNodeName.lift(2).get.name must include("Foo")

      val anotherMid = MuseumId(4)
      val notGetNodeName = storageUnitDao.getStorageNodeByName(anotherMid, "Foo", 1, 25).futureValue
      notGetNodeName.size mustBe 0
    }
    "fail when searching for name without any search criteria, or too few, or another museumId" in {
      val mid = MuseumId(5)
      val getNodeName = storageUnitDao.getStorageNodeByName(mid, "", 1, 25).futureValue
      getNodeName.size mustBe 0

      val tooFewLettersInSearchStr = storageUnitDao.getStorageNodeByName(mid, "", 1, 25).futureValue
      tooFewLettersInSearchStr.size mustBe 0

      val anotherMid = MuseumId(4)
      val noNodeName = storageUnitDao.getStorageNodeByName(anotherMid, "Foo", 1, 25).futureValue
      noNodeName.size mustBe 0
    }

  }

}
