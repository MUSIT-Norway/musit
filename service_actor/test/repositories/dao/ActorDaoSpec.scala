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

import models.Person
import no.uio.musit.security.FakeAuthenticator.fakeAccessTokenPrefix
import no.uio.musit.security.{AuthenticatedUser, BearerToken, FakeAuthenticator}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import org.scalatest.time.{Millis, Seconds, Span}

class ActorDaoSpec extends MusitSpecWithAppPerSuite {

  val actorDao: ActorDao = fromInstanceCache[ActorDao]

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  "ActorDao" when {

    "querying the person legacy methods" should {

      "return None when Id is very large" in {
        actorDao.getById(6386363673636335366L).futureValue mustBe None
      }

      "return a Person if the Id is valid" in {
        val expected = Person(
          id = Some(1),
          fn = "And, Arne1",
          dataportenId = Some("12345678-adb2-4b49-bce3-320ddfe6c90f")
        )

        actorDao.getById(1).futureValue mustBe Some(expected)
      }

      "return None if the Id is 0 (zero)" in {
        actorDao.getById(0).futureValue mustBe None
      }

      "return empty list if the search string is not found" in {
        actorDao.getByName("Andlkjlkj").futureValue.isEmpty mustBe true
      }

      "get person details" in {
        val ids = Set(1L, 2L, 3L)
        val persons = actorDao.listByIds(ids).futureValue
        persons.length mustBe 2
        persons.head.fn mustBe "And, Arne1"
        persons.tail.head.fn mustBe "Kanin, Kalle1"
      }

      "return a Person if the ID from dataporten is valid" in {
        val uid = "a1a2a3a4-adb2-4b49-bce3-320ddfe6c90f"
        val newPerson = Person(
          id = Some(2),
          fn = "Herr Larmerud",
          dataportenId = Some(uid)
        )
        val personId = actorDao.insert(newPerson).futureValue.id.get
        val res = actorDao.getByDataportenId(uid).futureValue
        res.isDefined mustBe true
        val person = res.get
        person.fn mustBe "Herr Larmerud"
        person.dataportenId mustBe Some(uid)
        person.id mustBe Some(personId)
      }

      "not find an actor if the Id from dataporten is unknown" in {
        actorDao.getByDataportenId("tull").futureValue.isDefined mustBe false
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
