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

import java.util.UUID

import models.Person
import no.uio.musit.models.{ActorId, DatabaseId}
import no.uio.musit.test.MusitSpecWithAppPerSuite

class ActorDaoSpec extends MusitSpecWithAppPerSuite {

  val actorDao: ActorDao = fromInstanceCache[ActorDao]

  val andersAndAppId = ActorId(UUID.fromString("41ede78c-a6f6-4744-adad-02c25fb1c97c"))
  val kalleKaninAppId = ActorId(UUID.fromString("5224f873-5fe1-44ec-9aaf-b9313db410c6"))

  "ActorDao" when {

    "querying the person legacy methods" should {

      "return None when Id doesn't exist" in {
        actorDao.getByDbId(6386363673636335366L).futureValue mustBe None
      }

      "return a Person if the actor Id is valid" in {
        val expected = Person(
          id = Some(DatabaseId(1L)),
          fn = "And, Arne1",
          dataportenId = None,
          dataportenUser = Some("andarn"),
          applicationId = Some(andersAndAppId)
        )

        actorDao.getByActorId(andersAndAppId).futureValue mustBe Some(expected)
      }

      "return None if the actor Id doesn't exist" in {
        actorDao.getByActorId(ActorId.generate()).futureValue mustBe None
      }

      "return empty list if the search string is not found" in {
        actorDao.getByName(99, "Andlkjlkj").futureValue.isEmpty mustBe true
      }

      "get person details" in {
        val ids = Set(andersAndAppId, kalleKaninAppId, ActorId.generate())
        val persons = actorDao.listBy(ids).futureValue
        persons.length mustBe 2
        persons.head.fn mustBe "And, Arne1"
        persons.tail.head.fn mustBe "Kanin, Kalle1"
      }

      "not find an actor if the Id from dataporten is unknown" in {
        actorDao.getByDataportenId(ActorId(UUID.randomUUID()))
          .futureValue.isDefined mustBe false
      }
    }
  }
}
