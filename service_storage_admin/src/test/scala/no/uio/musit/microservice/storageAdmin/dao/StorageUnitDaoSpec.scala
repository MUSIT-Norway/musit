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

import no.uio.musit.microservice.storageAdmin.domain.dto.{ StorageType, StorageUnitDTO }
import no.uio.musit.microservices.common.PlayTestDefaults
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{ Seconds, Span }
import org.scalatestplus.play.{ OneAppPerSuite, PlaySpec }
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder

class StorageUnitDaoSpec extends PlaySpec with OneAppPerSuite with ScalaFutures {

  implicit override lazy val app = new GuiceApplicationBuilder()
    .configure(PlayTestDefaults.inMemoryDatabaseConfig())
    .build()

  val storageDao: StorageUnitDao = {
    val instance = Application.instanceCache[StorageUnitDao]
    instance(app)
  }

  implicit val defaultPatience = PatienceConfig(timeout = Span(10, Seconds))

  "Interacting with the StorageUnitDao" when {

    "setting isPartOf for a StorageUnit" should {
      "succeed" in {
        storageDao.insert(StorageUnitDTO(None, "C2", None, None, None, None, None, None, None, None, isDeleted = false, StorageType.StorageUnit))
        storageDao.insert(StorageUnitDTO(None, "C2", None, None, None, None, None, None, None, None, isDeleted = false, StorageType.StorageUnit))
        val result = storageDao.all().futureValue
        result.size mustBe 2
        storageDao.setPartOf(1, 2).futureValue mustBe 1
        import scala.concurrent.ExecutionContext.Implicits.global
        storageDao.getStorageUnitOnlyById(1).map(_.get.isPartOf).futureValue mustBe Some(2)
      }
    }
  }
}
