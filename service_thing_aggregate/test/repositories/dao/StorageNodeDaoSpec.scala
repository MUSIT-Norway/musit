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

package repositories.dao

import no.uio.musit.models.{MuseumId, StorageNodeDatabaseId}
import no.uio.musit.service.MusitResults.MusitSuccess
import no.uio.musit.test.MusitSpecWithAppPerSuite
import org.scalatest.time.{Millis, Seconds, Span}

class StorageNodeDaoSpec extends MusitSpecWithAppPerSuite {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  val dao: StorageNodeDao = fromInstanceCache[StorageNodeDao]

  "Interacting with the StorageNodeDao" when {

    "getting objects for a nodeId that does not exist in a museum" should {
      "return false" in {
        dao.nodeExists(MuseumId(99), StorageNodeDatabaseId(9999)).futureValue match {
          case MusitSuccess(false) =>
          case _ => fail("it should not exist")
        }
      }
    }

    "getting objects for a nodeId that exists in a museum" should {
      "return true" in {
        dao.nodeExists(MuseumId(99), StorageNodeDatabaseId(4)).futureValue match {
          case MusitSuccess(true) =>
          case _ => fail("it should exist")
        }
      }
    }

    "getting objects using an invalid museum ID" should {
      "return true" in {
        dao.nodeExists(MuseumId(55), StorageNodeDatabaseId(4)).futureValue match {
          case MusitSuccess(false) =>
          case _ => fail("it should not exist")
        }
      }
    }
  }
}
