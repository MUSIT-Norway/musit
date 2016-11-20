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
import no.uio.musit.security.fake.FakeAuthenticator
import no.uio.musit.security.fake.FakeAuthenticator.fakeAccessTokenPrefix
import no.uio.musit.security.{AuthenticatedUser, BearerToken}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import org.scalatest.time.{Millis, Seconds, Span}

class ActorDaoSpec extends MusitSpecWithAppPerSuite {

  val actorDao: ActorDao = fromInstanceCache[ActorDao]

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  val andersAndAuthId = ActorId(UUID.fromString("12345678-adb2-4b49-bce3-320ddfe6c90f"))
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
          dataportenId = Some(andersAndAuthId),
          dataportenUser = Some("andarn"),
          applicationId = Some(andersAndAppId)
        )

        actorDao.getByActorId(andersAndAppId).futureValue mustBe Some(expected)
      }

      "return None if the actor Id doesn't exist" in {
        actorDao.getByActorId(ActorId.generate()).futureValue mustBe None
      }

      "return empty list if the search string is not found" in {
        actorDao.getByName("Andlkjlkj").futureValue.isEmpty mustBe true
      }

      "get person details" in {
        val ids = Set(andersAndAuthId, kalleKaninAppId, ActorId.generate())
        val persons = actorDao.listBy(ids).futureValue
        persons.length mustBe 2
        persons.head.fn mustBe "And, Arne1"
        persons.tail.head.fn mustBe "Kanin, Kalle1"
      }

      "return a Person if the ID from dataporten is valid" in {
        val did = ActorId(UUID.randomUUID())
        val newPerson = Person(
          id = None,
          fn = "Herr Larmerud",
          dataportenId = Some(did),
          dataportenUser = Some("larmerud")
        )
        val personId = actorDao.insert(newPerson).futureValue.id.get
        val res = actorDao.getByDataportenId(did).futureValue
        res.isDefined mustBe true
        val person = res.get
        person.fn mustBe "Herr Larmerud"
        person.dataportenId mustBe Some(did)
        person.dataportenUser mustBe Some("larmerud")
        person.applicationId must not be None
        person.applicationId.get mustBe an[ActorId]
        person.id mustBe Some(DatabaseId(3L))
      }

      "not find an actor if the Id from dataporten is unknown" in {
        actorDao.getByDataportenId(ActorId(UUID.randomUUID()))
          .futureValue.isDefined mustBe false
      }

      "insert an authenticated user that doesn't exist in the actor table" in {
        val authenticator = new FakeAuthenticator
        val fakeUsrId = "guest"
        val fakeToken = BearerToken(fakeAccessTokenPrefix + fakeUsrId)
        val authUsr = AuthenticatedUser(
          userInfo = authenticator.userInfo(fakeToken).futureValue.get,
          groups = Seq.empty
        )

        actorDao.getByDataportenId(authUsr.userInfo.id)
          .futureValue.isDefined mustBe false

        val person = actorDao.insertAuthUser(authUsr).futureValue
        person.dataportenId mustBe Some(authUsr.userInfo.id)
        person.fn mustBe authUsr.userInfo.name.get
        person.email mustBe authUsr.userInfo.email

        val res2 = actorDao.getByDataportenId(authUsr.userInfo.id).futureValue
        res2.isDefined mustBe true
        res2.get.fn mustBe authUsr.userInfo.name.get
      }
    }
  }
}
