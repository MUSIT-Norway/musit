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

import no.uio.musit.microservice.storagefacility.domain.{Museum, MuseumId, NodePath}
import no.uio.musit.microservice.storagefacility.domain.storage.{Root, StorageNodeId, StorageType}
import no.uio.musit.microservice.storagefacility.testhelpers.NodeGenerators
import no.uio.musit.service.MusitResults.MusitSuccess
import no.uio.musit.test.MusitSpecWithAppPerSuite
import org.scalatest.time.{Millis, Seconds, Span}

class StorageUnitDaoSpec extends MusitSpecWithAppPerSuite with NodeGenerators {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  "StorageUnitDao" should {

    "succeed when inserting several root nodes" in {
      for (i <- 7 to 9) {
        val insId = storageUnitDao.insertRoot(defaultMuseumId, Root()).futureValue
        insId mustBe a[StorageNodeId]
        insId mustBe StorageNodeId(i.toLong)
      }
      val anotherMid = MuseumId(4)
      for (i <- 10 to 12) {
        val insId = storageUnitDao.insertRoot(anotherMid, Root()).futureValue
        insId mustBe a[StorageNodeId]
        insId mustBe StorageNodeId(i.toLong)
      }
    }

    "succeed when inserting a new storage unit" in {
      val path = NodePath(",1,2,3,4,")
      val insId = storageUnitDao.insert(defaultMuseumId, createStorageUnit(path = path)).futureValue
      insId mustBe a[StorageNodeId]
    }

    "successfully fetch a storage unit" in {
      val mid = MuseumId(5)
      val su = createStorageUnit()
      val insId = storageUnitDao.insert(mid, su).futureValue
      insId mustBe a[StorageNodeId]

      val res = storageUnitDao.getById(mid, insId).futureValue
      res must not be None

      res.get.storageType mustBe su.storageType
      res.get.name mustBe su.name
    }

    "successfully update a storage unit and fetch as StorageNode" in {
      val mid = MuseumId(5)
      val su = createStorageUnit()
      val insId = storageUnitDao.insert(mid, su).futureValue
      insId mustBe a[StorageNodeId]

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

    "UnSuccessfully list root nodes with wrong museum" in {
      val mid = MuseumId(5)
      val nodes = storageUnitDao.findRootNodes(mid).futureValue
      nodes.size mustBe 0
      nodes.foreach(_.storageType mustBe StorageType.RootType)
    }
    "UnSuccessfully list root nodes with museum that do not exists" in {
      val mid = MuseumId(55)
      val nodes = storageUnitDao.findRootNodes(mid).futureValue
      nodes.size mustBe 0
      nodes.foreach(_.storageType mustBe StorageType.RootType)
    }

    "successfully mark a node as deleted" in {
      val su = createStorageUnit()
      val insId = storageUnitDao.insert(defaultMuseumId, su).futureValue
      insId mustBe a[StorageNodeId]

      val deleted = storageUnitDao.markAsDeleted(defaultMuseumId, insId).futureValue
      deleted.isSuccess mustBe true
      deleted.get mustBe 1

      val res = storageUnitDao.getById(defaultMuseumId, insId).futureValue
      res mustBe None
    }

    "successfully fetch the named path elements for a storage node" in {
      val path1 = NodePath(",7,17,")
      val su1 = createStorageUnit(
        partOf = Some(StorageNodeId(7)),
        path = path1
      ).copy(name = "node1")
      val insId1 = storageUnitDao.insert(defaultMuseumId, su1).futureValue
      insId1 mustBe a[StorageNodeId]
      insId1 mustBe StorageNodeId(17)

      val path2 = path1.appendChild(StorageNodeId(18))
      val su2 = createStorageUnit(
        partOf = Some(insId1),
        path = path2
      ).copy(name = "node2")
      val insId2 = storageUnitDao.insert(defaultMuseumId, su2).futureValue
      insId2 mustBe a[StorageNodeId]
      insId2 mustBe StorageNodeId(18)

      val res = storageUnitDao.namesForPath(path2).futureValue
      res must not be empty
      res.size mustBe 3
      res.head.nodeId mustBe StorageNodeId(7)
      res.head.name mustBe "root-node"
      res.tail.head.nodeId mustBe StorageNodeId(17)
      res.tail.head.name mustBe "node1"
      res.last.nodeId mustBe StorageNodeId(18)
      res.last.name mustBe "node2"
    }

    "UnSuccessfully fetch a storage unit with wrong museumId" in {
      val mid = MuseumId(5)
      val su = createStorageUnit()
      val insId = storageUnitDao.insert(mid, su).futureValue
      insId mustBe a[StorageNodeId]

      val wrongMid = MuseumId(4)
      val res = storageUnitDao.getById(mid, insId).futureValue
      res must not be None

      res.get.storageType mustBe su.storageType
      res.get.name mustBe su.name
    }

    "UnSuccessfully update a storage unit with wrong museumId and fetch as StorageNode" in {
      val mid = MuseumId(5)
      val su = createStorageUnit()
      val insId = storageUnitDao.insert(mid, su).futureValue
      insId mustBe a[StorageNodeId]

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

    "Unsuccessfully mark a node as deleted with wrong museumId" in {
      val su = createStorageUnit()
      val insId = storageUnitDao.insert(defaultMuseumId, su).futureValue
      insId mustBe a[StorageNodeId]

      val anotherMid = MuseumId(4)
      val deleted = storageUnitDao.markAsDeleted(anotherMid, insId).futureValue
      deleted.isFailure mustBe true

      val res = storageUnitDao.getById(defaultMuseumId, insId).futureValue
      res.get.id mustBe Some(insId)
    }
  }

}
