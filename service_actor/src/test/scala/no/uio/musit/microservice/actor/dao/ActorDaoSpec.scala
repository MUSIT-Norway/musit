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

package no.uio.musit.microservice.actor.dao

import no.uio.musit.microservice.actor.domain.Person
import no.uio.musit.microservices.common.PlayTestDefaults
import no.uio.musit.microservices.common.linking.LinkService
import no.uio.musit.security.FakeSecurity
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{ Millis, Seconds, Span }
import org.scalatestplus.play.{ OneAppPerSuite, PlaySpec }
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder

class ActorDaoSpec extends PlaySpec with OneAppPerSuite with ScalaFutures {

  implicit override lazy val app = new GuiceApplicationBuilder()
    .configure(PlayTestDefaults.inMemoryDatabaseConfig())
    .build()

  val actorDao: ActorDao = {
    val instance = Application.instanceCache[ActorDao]
    instance(app)
  }

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  "Actor dao" when {

    "querying the person legacy methods" should {

      "return None when Id is very large" in {
        val res = actorDao.getPersonLegacyById(6386363673636335366L).futureValue
        res mustBe None
      }

      "return a Person if the Id is valid" in {
        val expected = Person(Some(1), "And, Arne1", dataportenId = Some("12345678-adb2-4b49-bce3-320ddfe6c90f"), links = Some(Seq(LinkService.self("/v1/person/1"))))
        val res = actorDao.getPersonLegacyById(1).futureValue

        res mustBe Some(expected)

      }

      "return None if the Id is 0 (zero)" in {
        val res = actorDao.getPersonLegacyById(0).futureValue
        res mustBe None
      }
    }

    "dataporten integration" should {
      "return a Person if the dataportenId is valid" in {
        val uid = "a1a2a3a4-adb2-4b49-bce3-320ddfe6c90f"
        val newPerson = Person(Some(2), "Herr Larmerud", dataportenId = Some(uid),
          links = Some(Seq(LinkService.self("/v1/person/2"))))

        val personId = actorDao.insertPersonLegacy(newPerson).futureValue.id.get

        val res = actorDao.getPersonByDataportenId(uid).futureValue
        res.isDefined mustBe true
        val person = res.get
        person.fn mustBe "Herr Larmerud"
        person.dataportenId mustBe Some(uid)
        person.id mustBe Some(personId)
        //We don't have a way to delete legacyPersons, may want to delete the newly insterted actor in the future: actorDao.deletePerson(personId)
      }

      "Don't find actor with unknown dataportenId" in {
        val res = actorDao.getPersonByDataportenId("tullballId").futureValue
        res.isDefined mustBe false
      }

      "Don't find actor with unknown dataportenId based on security connection etc" in {
        val secConnection = FakeSecurity.createInMemoryFromFakeAccessToken("fake-token-zab-xy-jarle", false).futureValue
        val res = actorDao.getPersonByDataportenId(secConnection.userId).futureValue
        res.isDefined mustBe false

        val person = actorDao.insertActorWithDataportenUserInfo(secConnection).futureValue
        person.isRight mustBe true
        person.right.map {
          p =>
            p.dataportenId mustBe Some(secConnection.userId)
            p.fn mustBe secConnection.userName
            p.email mustBe secConnection.userEmail
        }

        val res2 = actorDao.getPersonByDataportenId(secConnection.userId).futureValue
        res2.isDefined mustBe true
        res2.get.fn mustBe secConnection.userName
      }
    }
  }
}
