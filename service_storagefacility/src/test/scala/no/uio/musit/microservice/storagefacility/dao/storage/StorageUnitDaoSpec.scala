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

import no.uio.musit.microservice.storagefacility.domain.NodePath
import no.uio.musit.microservice.storagefacility.domain.storage.{Root, StorageNodeId, StorageType}
import no.uio.musit.microservice.storagefacility.testhelpers.NodeGenerators
import no.uio.musit.test.MusitSpecWithAppPerSuite
import org.scalatest.time.{Millis, Seconds, Span}

class StorageUnitDaoSpec extends MusitSpecWithAppPerSuite with NodeGenerators {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  "StorageUnitDao" should {

    "succeed when inserting several root nodes" in {
      for (i <- 1 to 3) {
        val ins = storageUnitDao.insertRoot(Root()).futureValue
        ins.id.isEmpty must not be true
        ins.storageType mustBe StorageType.RootType
        ins.path mustBe Some(NodePath.empty)
      }
    }

    "succeed when inserting a new storage unit" in {
      val path = Some(NodePath(",1,2,3,4,"))
      val inserted = storageUnitDao.insert(createStorageUnit(path = path)).futureValue
      inserted.id must not be None
      inserted.path mustBe path
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

      val again = storageUnitDao.getById(inserted.id.get).futureValue
      again must not be None
      again.get.name mustBe "UggaBugga"
      again.get.areaTo mustBe Some(4.0)
    }

    "successfully list root nodes" in {
      val nodes = storageUnitDao.findRootNodes.futureValue
      nodes.size mustBe 3
      nodes.foreach(_.storageType mustBe StorageType.RootType)
    }

    "successfully mark a node as deleted" in {
      val su = createStorageUnit()
      val inserted = storageUnitDao.insert(su).futureValue
      inserted.id must not be None

      val deleted = storageUnitDao.markAsDeleted(inserted.id.get).futureValue
      deleted.isSuccess mustBe true
      deleted.get mustBe 1

      val res = storageUnitDao.getById(inserted.id.get).futureValue
      res mustBe None
    }

    "successfully fetch the named path elements for a storage node" in {
      val path1 = NodePath(",1,8,")
      val su1 = createStorageUnit(
        partOf = Some(StorageNodeId(1)),
        path = Some(path1)
      ).copy(name = "node1")
      val ins1 = storageUnitDao.insert(su1).futureValue
      ins1.id mustBe Some(StorageNodeId(8))
      ins1.path mustBe Some(path1)

      val path2 = path1.appendChild(StorageNodeId(9))
      val su2 = createStorageUnit(
        partOf = ins1.id,
        path = Some(path2)
      ).copy(name = "node2")
      val ins2 = storageUnitDao.insert(su2).futureValue
      ins2.id mustBe Some(StorageNodeId(9))
      ins2.path mustBe Some(path2)

      val res = storageUnitDao.namesForPath(path2).futureValue
      res must not be empty
      res.size mustBe 3
      res.head.nodeId mustBe StorageNodeId(1)
      res.head.name mustBe "root-node"
      res.tail.head.nodeId mustBe StorageNodeId(8)
      res.tail.head.name mustBe "node1"
      res.last.nodeId mustBe StorageNodeId(9)
      res.last.name mustBe "node2"

    }
  }

}
