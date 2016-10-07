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
import no.uio.musit.microservice.storagefacility.domain.storage.{Root, StorageType}
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
      val mid = MuseumId(2)
      for (i <- 1 to 3) {
        val ins = storageUnitDao.insertRoot(mid, Root()).futureValue
        ins.id.isEmpty must not be true
        ins.storageType mustBe StorageType.RootType
        ins.path mustBe Some(NodePath.empty)
      }
      val anotherMid = MuseumId(4)
      for (i <- 1 to 3) {
        val ins = storageUnitDao.insertRoot(anotherMid, Root()).futureValue
        ins.id.isEmpty must not be true
        ins.storageType mustBe StorageType.RootType
        ins.path mustBe Some(NodePath.empty)
      }
    }

    "succeed when inserting a new storage unit" in {
      val mid = MuseumId(2)
      val path = Some(NodePath(",1,2,3,4,"))
      val inserted = storageUnitDao.insert(mid, createStorageUnit(path = path)).futureValue
      inserted.id must not be None
      inserted.path mustBe path
    }

    "successfully fetch a storage unit" in {
      val mid = MuseumId(5)
      val su = createStorageUnit()
      val inserted = storageUnitDao.insert(mid, su).futureValue
      inserted.id must not be None

      val res = storageUnitDao.getById(mid, inserted.id.get).futureValue
      res must not be None

      res.get.storageType mustBe su.storageType
      res.get.name mustBe su.name
    }

    "successfully update a storage unit and fetch as StorageNode" in {
      val mid = MuseumId(5)
      val su = createStorageUnit()
      val inserted = storageUnitDao.insert(mid, su).futureValue
      inserted.id must not be None

      val res = storageUnitDao.getById(mid, inserted.id.get).futureValue
      res must not be None
      res.get.storageType mustBe su.storageType
      res.get.name mustBe su.name

      val upd = res.get.copy(name = "UggaBugga", areaTo = Some(4.0))

      val updRes = storageUnitDao.update(mid, res.get.id.get, upd).futureValue
      updRes must not be None
      updRes.get.name mustBe "UggaBugga"
      updRes.get.areaTo mustBe Some(4.0)

      val again = storageUnitDao.getById(mid, inserted.id.get).futureValue
      again must not be None
      again.get.name mustBe "UggaBugga"
      again.get.areaTo mustBe Some(4.0)
    }

    "successfully list root nodes" in {
      val mid = MuseumId(2)
      val nodes = storageUnitDao.findRootNodes(mid).futureValue
      nodes.size mustBe 3
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
      val mid = MuseumId(2)
      val su = createStorageUnit()
      val inserted = storageUnitDao.insert(mid, su).futureValue
      inserted.id must not be None

      val deleted = storageUnitDao.markAsDeleted(mid, inserted.id.get).futureValue
      deleted.isSuccess mustBe true
      deleted.get mustBe 1

      val res = storageUnitDao.getById(mid, inserted.id.get).futureValue
      res mustBe None
    }
    "UnSuccessfully fetch a storage unit with wrong museumId" in {
      val mid = MuseumId(5)
      val su = createStorageUnit()
      val inserted = storageUnitDao.insert(mid, su).futureValue
      inserted.id must not be None

      val wrongMid = MuseumId(4)
      val res = storageUnitDao.getById(mid, inserted.id.get).futureValue
      res must not be None

      res.get.storageType mustBe su.storageType
      res.get.name mustBe su.name
    }

    "UnSuccessfully update a storage unit with wrong museumId and fetch as StorageNode" in {
      val mid = MuseumId(5)
      val su = createStorageUnit()
      val inserted = storageUnitDao.insert(mid, su).futureValue
      inserted.id must not be None

      val res = storageUnitDao.getById(mid, inserted.id.get).futureValue
      res must not be None
      res.get.storageType mustBe su.storageType
      res.get.name must include("FooUnit")
      res.get.name mustBe su.name
      res.get.areaTo mustBe Some(2.0)

      val upd = res.get.copy(name = "UggaBugga", areaTo = Some(4.0))

      val anotherMid = MuseumId(4)
      val updRes = storageUnitDao.update(anotherMid, res.get.id.get, upd).futureValue
      updRes mustBe None

      val again = storageUnitDao.getById(mid, inserted.id.get).futureValue
      again must not be None
      again.get.name mustBe "FooUnit"
      again.get.areaTo mustBe Some(2.0)
    }

    "Unsuccessfully mark a node as deleted with wrong museumId" in {
      val mid = MuseumId(2)
      val su = createStorageUnit()
      val inserted = storageUnitDao.insert(mid, su).futureValue
      inserted.id must not be None

      val anotherMid = MuseumId(4)
      val deleted = storageUnitDao.markAsDeleted(anotherMid, inserted.id.get).futureValue
      deleted.isFailure mustBe true

      val res = storageUnitDao.getById(mid, inserted.id.get).futureValue
      res.get.id mustBe Some(inserted.id.get)
    }
  }

}
