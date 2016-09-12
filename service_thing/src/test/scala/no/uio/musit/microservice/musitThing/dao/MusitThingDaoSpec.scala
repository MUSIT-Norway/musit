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

package no.uio.musit.microservice.musitThing.dao

import no.uio.musit.microservice.musitThing.domain.MusitThing
import no.uio.musit.microservices.common.PlayTestDefaults
import no.uio.musit.microservices.common.linking.LinkService
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.{ OneAppPerSuite, PlaySpec }
import play.api.Application
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import slick.backend.DatabaseConfig
import slick.profile.BasicProfile

class MusitThingDaoSpec extends PlaySpec with OneAppPerSuite with ScalaFutures {

  implicit override lazy val app = new GuiceApplicationBuilder()
    .configure(PlayTestDefaults.inMemoryDatabaseConfig())
    .build()

  val thingDao: MusitThingDao = {
    val instance = Application.instanceCache[MusitThingDao]
    instance(app)
  }

  "Interacting with the MusitThingDao" when {

    "inserting a MusitThing" should {
      "succeed when data is valid" in {
        thingDao.insert(MusitThing(Some(1), "C2", "spyd", None))
        thingDao.insert(MusitThing(Some(2), "C3", "øks", None))
        val result = thingDao.all.futureValue
        result.length mustBe 4
      }
    }

    "fetching the display name for a MusitThing" should {
      "return None when Id is a very large number" in {
        val result = thingDao.getDisplayName(6386363673636335366L).futureValue
        result mustBe None
      }

      "return the display name when providing a valid Id" in {
        val result = thingDao.getDisplayName(2).futureValue
        result mustBe Some("Kniv7")
      }

      "return None if the Id is 0 (zero)" in {
        val result = thingDao.getDisplayName(0).futureValue
        result mustBe None
      }
    }

    "fetching the display Id for a MusitThing" should {
      "return None when Id is a very large number" in {
        val result = thingDao.getDisplayId(6386363673636335366L).futureValue
        result mustBe None
      }

      "return the display Id when providing a valid Id" in {
        val result = thingDao.getDisplayId(2).futureValue
        result mustBe Some("C2")
      }

      "return None if the Id is 0 (zero)" in {
        val svar = thingDao.getDisplayId(0).futureValue
        svar mustBe None
      }
    }

    "fetching a MusitThing by Id" should {

      "return None if the Id is a very large number" in {
        val svar = thingDao.getById(6386363673636335366L).futureValue
        svar mustBe None
      }

      "return a MusitThing if the id is valid" in {
        val svar = thingDao.getById(1).futureValue
        svar.contains(MusitThing(Some(1), "C1", "Øks5", Some(Seq(LinkService.self("/v1/1")))))
      }

      "return None if the Id is 0 (zero)" in {
        val svar = thingDao.getById(0).futureValue
        svar mustBe None
      }
    }
  }
}
