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

import no.uio.musit.microservice.actor.domain.{Organization, OrganizationAddress, Person}
import no.uio.musit.microservices.common.PlayTestDefaults
import no.uio.musit.microservices.common.linking.LinkService
import no.uio.musit.security.FakeSecurity
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{ Millis, Seconds, Span }
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import org.scalatest.Matchers._
import org.scalatest.exceptions.TestFailedException

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

      "return empty list if the search string is not found" in {
        val res = actorDao.getPersonLegacyByName("Andlkjlkj").futureValue
        res.isEmpty mustBe true
      }
    }

    "querying the organization methods" should {

      "return None when Id is very large" in {
        val res = actorDao.getOrganizationById(6386363673636335366L).futureValue
        res mustBe None
      }

      "return a organization if the Id is valid" in {
        val expected = Organization(Some(1), "Kulturhistorisk museum - Universitetet i Oslo", "KHM", "22 85 19 00", "www.khm.uio.no", None)
        val res = actorDao.getOrganizationById(1).futureValue
        expected.id mustBe res.get.id
        expected.fn mustBe res.get.fn
        expected.nickname mustBe res.get.nickname
        expected.tel mustBe res.get.tel
        expected.web mustBe res.get.web
      }

      "return None if the Id is 0 (zero)" in {
        val res = actorDao.getOrganizationById(0).futureValue
        res mustBe None
      }

      "return empty list if the search string is not found" in {
        val res = actorDao.getOrganizationByName("Andlkjlkj").futureValue
        res.isEmpty mustBe true
      }
    }

    "modifying organization" should {

      "succeed when inserting organization" in {
        val org = Organization(None, "Testmuseet i Bergen", "TM", "99887766", "www.tmib.no", None)
        val res = actorDao.insertOrganization(org).futureValue
        res.fn mustBe "Testmuseet i Bergen"
        res.id mustBe Some(2)
      }

      "succeed when updating organization" in {
        val org1 = Organization(None, "Museet i Foobar", "FB", "12344321", "www.foob.no", None)
        val res1 = actorDao.insertOrganization(org1).futureValue
        res1.fn mustBe "Museet i Foobar"
        res1.id mustBe Some(3)

        val orgUpd = Organization(Some(3), "Museet i Bar", "B", "99344321", "www.bar.no", None)

        val resInt = actorDao.updateOrganization(orgUpd).futureValue
        val res = actorDao.getOrganizationById(3).futureValue
        res.get.fn mustBe "Museet i Bar"
        res.get.nickname mustBe "B"
        res.get.tel mustBe "99344321"
        res.get.web mustBe "www.bar.no"
      }

      "not update organization with invalid id" in {
        val orgUpd = Organization(Some(999991), "Museet i Bar99", "B", "99344321", "www.bar.no", None)
        actorDao.updateOrganization(orgUpd).futureValue mustBe 0
      }

      "succeed when deleting organization" in {
        val res1 = actorDao.deleteOrganization(3).futureValue
        res1 mustBe 1
        val res = actorDao.getOrganizationById(3).futureValue
        res mustBe None
      }

      "not be able to delete organization with invalid id" in {
        val res2 = actorDao.deleteOrganization(999999).futureValue
        res2 mustBe 0
      }

    }

    "modifying organizationAddress" should {

      "succeed when inserting organizationAddress" in {
        val orgAddr = OrganizationAddress(None, Some(2), "WORK", "Adressen", "Oslo", "0123", "Norway", 60.11, 11.60, None)
        val res = actorDao.insertOrganizationAddress(orgAddr).futureValue
        res.addressType mustBe "WORK"
        res.streetAddress mustBe "Adressen"
        res.postalCode mustBe "0123"
        res.id mustBe Some(2)
      }

      "succeed when updating organizationAddress" in {
        val orgAddr1 = OrganizationAddress(None, Some(2), "WORK2", "Adressen2", "Bergen", "0122", "Norway2", 60.11, 11.60, None)
        val res1 = actorDao.insertOrganizationAddress(orgAddr1).futureValue
        res1.addressType mustBe "WORK2"
        res1.streetAddress mustBe "Adressen2"
        res1.postalCode mustBe "0122"
        res1.id mustBe Some(3)

        val orgUpd = Organization(Some(3), "Museet i Bar", "B", "99344321", "www.bar.no", None)
        val orgAddrUpd = OrganizationAddress(Some(3), Some(2), "WORK3", "Adressen3", "Bergen3", "0133", "Norway3", 60.11, 11.60, None)

        val resInt = actorDao.updateOrganizationAddress(orgAddrUpd).futureValue
        resInt mustBe 1
        val res = actorDao.getOrganizationAddressById(3).futureValue
        res.get.id mustBe Some(3)
        res.get.organizationId mustBe Some(2)
        res.get.addressType mustBe "WORK3"
        res.get.streetAddress mustBe "Adressen3"
        res.get.locality mustBe "Bergen3"
        res.get.postalCode mustBe "0133"
        res.get.countryName mustBe "Norway3"
        res.get.latitude mustBe 60.11
        res.get.longitude mustBe 11.60
      }

      "not update organization address with invalid id" in {
        val orgAddrUpd = OrganizationAddress(Some(9999992), Some(2), "WORK3", "Adressen3", "Bergen3", "0133", "Norway3", 60.11, 11.60, None)
        actorDao.updateOrganizationAddress(orgAddrUpd).futureValue mustBe 0
      }

      "not update organization address with missing id" in {
        val orgAddrUpd = OrganizationAddress(None, Some(2), "WORK3", "Adressen3", "Bergen3", "0133", "Norway3", 60.11, 11.60, None)
        actorDao.updateOrganizationAddress(orgAddrUpd).futureValue mustBe 0
      }

      "not update organization address with invalid organization id" in {
        val orgAddrUpd = OrganizationAddress(Some(3), Some(9999993), "WORK3", "Adressen3", "Bergen3", "0133", "Norway3", 60.11, 11.60, None)
        whenReady(actorDao.updateOrganizationAddress(orgAddrUpd).failed) { e =>
          e shouldBe a[org.h2.jdbc.JdbcSQLException]
          e.getMessage should startWith("Referential integrity")
        }
      }

      "succeed when deleting organization address" in {
        val res1 = actorDao.deleteOrganizationAddress(3).futureValue
        res1 mustBe 1
        val res = actorDao.getOrganizationAddressById(3).futureValue
        res mustBe None
      }

      "not be able to delete organization address with invalid id" in {
        val res2 = actorDao.deleteOrganizationAddress(999999).futureValue
        res2 mustBe 0
      }
    }
    "querying person details" should {
      "get person details" in {
        val ids = Set(1L, 2L, 3L)
        val persons = actorDao.getPersonDetailsByIds(ids).futureValue
        persons.length mustBe 2
        val person1 = persons(0)
        person1.fn mustBe "And, Arne1"
        val person2 = persons(1)
        person2.fn mustBe "Kanin, Kalle1"

      }
    }
    "queriyng finders" should {
      "get all legacy persons" in {
        val persons = actorDao.allPersonsLegacy().futureValue
        persons.length mustBe 2
      }
      "get all organizations" in {
        val orgs = actorDao.allOrganizations().futureValue
        orgs.length mustBe 2
      }
      "get all organization addresses" in {
        val orgAddrs = actorDao.allAddressesForOrganization(1).futureValue
        orgAddrs.length mustBe 1
        orgAddrs(0).streetAddress mustBe "Fredriks gate 2"
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
