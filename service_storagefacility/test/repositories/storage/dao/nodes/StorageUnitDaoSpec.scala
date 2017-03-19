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

package repositories.storage.dao.nodes

import models.storage.nodes.StorageType._
import models.storage.nodes.{Root, RootLoan, StorageType}
import no.uio.musit.MusitResults.MusitSuccess
import no.uio.musit.models._
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues
import org.joda.time.DateTime
import utils.testhelpers.NodeGenerators

class StorageUnitDaoSpec
    extends MusitSpecWithAppPerSuite
    with NodeGenerators
    with MusitResultValues {

  "StorageUnitDao" should {

    "succeed when inserting several root nodes" in {

      def createRoot(name: String): Root = Root(
        nodeId = StorageNodeId.generateAsOpt(),
        name = name,
        updatedBy = Some(defaultUserId),
        updatedDate = Some(DateTime.now())
      )

      def createRootLoan(name: String): RootLoan = RootLoan(
        nodeId = StorageNodeId.generateAsOpt(),
        name = name,
        updatedBy = Some(defaultUserId),
        updatedDate = Some(DateTime.now())
      )

      for (i <- 18 to 20) {
        val r     = createRoot(s"root$i")
        val insId = storageUnitDao.insertRoot(defaultMuseumId, r).futureValue

        insId mustBe MusitSuccess(StorageNodeDatabaseId(i.toLong))
      }
      val anotherMid = MuseumId(4)
      for (i <- 21 to 23) {
        val r     = createRootLoan(s"rootLoan$i")
        val insId = storageUnitDao.insertRoot(anotherMid, r).futureValue

        insId.successValue mustBe StorageNodeDatabaseId(i.toLong)
      }
    }

    "succeed when inserting a new storage unit" in {
      val path = NodePath(",1,2,3,4,")
      val insId = storageUnitDao
        .insert(defaultMuseumId, createStorageUnit(path = path))
        .futureValue // scalastyle:ignore
      insId.successValue mustBe a[StorageNodeDatabaseId]
    }

    "successfully fetch a storage unit" in {
      val mid   = MuseumId(5)
      val su    = createStorageUnit()
      val insId = storageUnitDao.insert(mid, su).futureValue
      insId.successValue mustBe a[StorageNodeDatabaseId]

      val res = storageUnitDao.getById(mid, insId.get).futureValue

      res.successValue.value.storageType mustBe su.storageType
      res.successValue.value.name mustBe su.name
    }

    "successfully update a storage unit and fetch as StorageNode" in {
      val mid   = MuseumId(5)
      val su    = createStorageUnit()
      val insId = storageUnitDao.insert(mid, su).futureValue
      insId.successValue mustBe a[StorageNodeDatabaseId]

      val res = storageUnitDao.getById(mid, insId.get).futureValue

      res.successValue.value.storageType mustBe su.storageType
      res.successValue.value.name mustBe su.name

      val upd = res.successValue.value.copy(name = "UggaBugga", areaTo = Some(4.0))

      val updRes =
        storageUnitDao.update(mid, res.successValue.value.id.get, upd).futureValue
      updRes.successValue.value mustBe 1

      val again = storageUnitDao.getById(mid, insId.get).futureValue
      again.successValue.value.name mustBe "UggaBugga"
      again.successValue.value.areaTo mustBe Some(4.0)
    }

    "successfully list root nodes" in {
      val nodes = storageUnitDao.findRootNodes(defaultMuseumId).futureValue

      nodes.successValue.foreach { n =>
        n.storageType.entryName must startWith("Root")
      }
    }

    "fail to list root nodes when museumId is wrong" in {
      val mid   = MuseumId(5)
      val nodes = storageUnitDao.findRootNodes(mid).futureValue

      nodes.successValue.size mustBe 0
      nodes.successValue.foreach(_.storageType mustBe StorageType.RootType)
    }

    "fail to list root nodes with museumId that does not exists" in {
      val mid   = MuseumId(55)
      val nodes = storageUnitDao.findRootNodes(mid).futureValue

      nodes.successValue.size mustBe 0
      nodes.successValue.foreach(_.storageType mustBe StorageType.RootType)
    }

    "successfully mark a node as deleted" in {
      val su    = createStorageUnit()
      val insId = storageUnitDao.insert(defaultMuseumId, su).futureValue
      insId.successValue mustBe a[StorageNodeDatabaseId]

      val deleted = storageUnitDao
        .markAsDeleted(defaultUserId, defaultMuseumId, insId.successValue)
        .futureValue // scalastyle:ignore

      deleted.successValue mustBe 1

      val res = storageUnitDao.getById(defaultMuseumId, insId.get).futureValue
      res.successValue mustBe None
    }

    "successfully fetch the named path elements for a storage node" in {
      val path1 = NodePath(",18,28,")
      val su1 = createStorageUnit(
        partOf = Some(StorageNodeDatabaseId(18)),
        path = path1
      ).copy(name = "node1")
      val insId1 = storageUnitDao.insert(defaultMuseumId, su1).futureValue
      insId1.successValue mustBe StorageNodeDatabaseId(28)

      val path2 = path1.appendChild(StorageNodeDatabaseId(29))
      val su2 = createStorageUnit(
        partOf = Some(insId1.get),
        path = path2
      ).copy(name = "node2")
      val insId2 = storageUnitDao.insert(defaultMuseumId, su2).futureValue
      insId2.successValue mustBe StorageNodeDatabaseId(29)

      val res = storageUnitDao.namesForPath(path2).futureValue
      res.successValue.size mustBe 3
      res.successValue.head.nodeId mustBe StorageNodeDatabaseId(18)
      res.successValue.head.name mustBe "root18"
      res.successValue.tail.head.nodeId mustBe StorageNodeDatabaseId(28)
      res.successValue.tail.head.name mustBe "node1"
      res.successValue.last.nodeId mustBe StorageNodeDatabaseId(29)
      res.successValue.last.name mustBe "node2"
    }

    "fail to fetch a storage unit with wrong museumId" in {
      val mid   = MuseumId(5)
      val su    = createStorageUnit()
      val insId = storageUnitDao.insert(mid, su).futureValue
      insId.successValue mustBe a[StorageNodeDatabaseId]

      val wrongMid = MuseumId(4)
      val res      = storageUnitDao.getById(mid, insId.get).futureValue

      res.successValue.value.storageType mustBe su.storageType
      res.successValue.value.name mustBe su.name
    }

    "fail to update a storage unit when museumId is wrong" in {
      val mid   = MuseumId(5)
      val su    = createStorageUnit()
      val insId = storageUnitDao.insert(mid, su).futureValue
      insId.successValue mustBe a[StorageNodeDatabaseId]

      val res = storageUnitDao.getById(mid, insId.get).futureValue

      res.successValue.value.storageType mustBe su.storageType
      res.successValue.value.name must include("FooUnit")
      res.successValue.value.name mustBe su.name
      res.successValue.value.areaTo mustBe Some(2.0)

      val upd = res.successValue.value.copy(name = "UggaBugga", areaTo = Some(4.0))

      val anotherMid = MuseumId(4)
      val updRes =
        storageUnitDao.update(anotherMid, res.successValue.value.id.get, upd).futureValue
      updRes.successValue mustBe None

      val again = storageUnitDao.getById(mid, insId.get).futureValue

      again.successValue.value.name mustBe "FooUnit"
      again.successValue.value.areaTo mustBe Some(2.0)
    }

    "fail to mark a node as deleted when museumId is wrong" in {
      val su    = createStorageUnit()
      val insId = storageUnitDao.insert(defaultMuseumId, su).futureValue
      insId.successValue mustBe a[StorageNodeDatabaseId]

      val anotherMid = MuseumId(4)
      val deleted = storageUnitDao
        .markAsDeleted(defaultUserId, anotherMid, insId.successValue)
        .futureValue // scalastyle:ignore
      deleted.isFailure mustBe true

      val res = storageUnitDao.getById(defaultMuseumId, insId.successValue).futureValue
      res.successValue.value.id mustBe Some(insId.get)
    }

    "fetch tuples of StorageNodeId and StorageType for a NodePath" in {
      val orgPath = NodePath(",1,33,")
      val org = createOrganisation(
        partOf = Some(StorageNodeDatabaseId(1)),
        path = orgPath
      ).copy(name = "node-x")
      val organisationId = organisationDao.insert(defaultMuseumId, org).futureValue

      val buildingPath = NodePath(",1,33,34,")
      val building = createBuilding(
        partOf = Some(organisationId.get),
        path = buildingPath
      )
      val buildingId = buildingDao.insert(defaultMuseumId, building).futureValue

      val roomPath = NodePath(",1,33,34,35,")
      val room = createRoom(
        partOf = Some(buildingId.get),
        path = roomPath
      )
      val roomId = roomDao.insert(defaultMuseumId, room).futureValue

      val su1Path = NodePath(",1,33,34,35,36,")
      val su1 = createStorageUnit(
        partOf = Some(roomId.get),
        path = su1Path
      )
      val suId = storageUnitDao.insert(defaultMuseumId, su1).futureValue

      val expected = Seq(
        StorageNodeDatabaseId(1)    -> RootType,
        organisationId.successValue -> OrganisationType,
        buildingId.successValue     -> BuildingType,
        roomId.successValue         -> RoomType,
        suId.successValue           -> StorageUnitType
      )

      val tuples = storageUnitDao
        .getStorageTypesInPath(defaultMuseumId, su1Path)
        .futureValue // scalastyle:ignore

      tuples.successValue must contain theSameElementsInOrderAs expected
    }

    "successfully get a node when searching for name and not if it's wrong museumId" in {
      val mid = MuseumId(5)
      val getNodeName =
        storageUnitDao.getStorageNodeByName(mid, "Foo", 1, 25).futureValue

      getNodeName.successValue.size mustBe 3
      getNodeName.successValue.head.name must include("Foo")
      getNodeName.successValue.lift(2).value.name must include("Foo")

      val anotherMid = MuseumId(4)
      val notGetNodeName = storageUnitDao
        .getStorageNodeByName(anotherMid, "Foo", 1, 25)
        .futureValue // scalastyle:ignore

      notGetNodeName mustBe a[MusitSuccess[_]]
      notGetNodeName.successValue.size mustBe 0
    }

    "fail when searching for name without invalid criteria" in {
      // scalastyle:ignore
      val mid         = MuseumId(5)
      val getNodeName = storageUnitDao.getStorageNodeByName(mid, "", 1, 25).futureValue

      getNodeName.successValue.size mustBe 0

      val tooFewLettersInSearchStr = storageUnitDao
        .getStorageNodeByName(mid, "", 1, 25)
        .futureValue // scalastyle:ignore
      tooFewLettersInSearchStr.successValue.size mustBe 0

      val anotherMid = MuseumId(4)
      val noNodeName = storageUnitDao
        .getStorageNodeByName(anotherMid, "Foo", 1, 25)
        .futureValue // scalastyle:ignore
      noNodeName.successValue.size mustBe 0
    }
  }

}
