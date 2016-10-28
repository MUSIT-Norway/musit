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
import no.uio.musit.security.{AuthenticatedUser, BearerToken, FakeAuthenticator}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import org.scalatest.time.{Millis, Seconds, Span}

class ActorDaoSpec extends MusitSpecWithAppPerSuite {

  val actorDao: ActorDao = fromInstanceCache[ActorDao]

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  "Actor dao" when {

    "querying the person legacy methods" should {

      "return None when Id is very large" in {
        actorDao.getPersonLegacyById(6386363673636335366L).futureValue mustBe None
      }

      "return a Person if the Id is valid" in {
        val expected = Person(
          id = Some(1),
          fn = "And, Arne1",
          dataportenId = Some("12345678-adb2-4b49-bce3-320ddfe6c90f")
        )

        actorDao.getPersonLegacyById(1).futureValue mustBe Some(expected)
      }

      "return None if the Id is 0 (zero)" in {
        actorDao.getPersonLegacyById(0).futureValue mustBe None
      }

      "return empty list if the search string is not found" in {
        actorDao.getPersonLegacyByName("Andlkjlkj").futureValue.isEmpty mustBe true
      }
    }

    "querying the organization methods" should {

      "return None when Id is very large" in {
        actorDao.getOrganizationById(Long.MaxValue).futureValue mustBe None
      }

      "return a organization if the Id is valid" in {
        val expected = Organization(
          id = Some(1),
          fn = "Kulturhistorisk museum - Universitetet i Oslo",
          nickname = "KHM",
          tel = "22 85 19 00",
          web = "www.khm.uio.no"
        )
        val res = actorDao.getOrganizationById(1).futureValue
        expected.id mustBe res.get.id
        expected.fn mustBe res.get.fn
        expected.nickname mustBe res.get.nickname
        expected.tel mustBe res.get.tel
        expected.web mustBe res.get.web
      }

      "return None if the Id is 0 (zero)" in {
        actorDao.getOrganizationById(0).futureValue mustBe None
      }

      "return empty list if the search string is not found" in {
        actorDao.getOrganizationByName("Andlkjlkj").futureValue mustBe empty
      }
    }

    "modifying organization" should {

      "succeed when inserting organization" in {
        val org = Organization(
          id = None,
          fn = "Testmuseet i Bergen",
          nickname = "TM",
          tel = "99887766",
          "www.tmib.no"
        )
        val res = actorDao.insertOrganization(org).futureValue
        res.fn mustBe "Testmuseet i Bergen"
        res.id mustBe Some(2)
      }

      "succeed when updating organization" in {
        val org1 = Organization(
          id = None,
          fn = "Museet i Foobar",
          nickname = "FB",
          tel = "12344321",
          web = "www.foob.no"
        )
        val res1 = actorDao.insertOrganization(org1).futureValue
        res1.fn mustBe "Museet i Foobar"
        res1.id mustBe Some(3)

        val orgUpd = Organization(
          id = Some(3),
          fn = "Museet i Bar",
          nickname = "B",
          tel = "99344321",
          web = "www.bar.no"
        )

        val resInt = actorDao.updateOrganization(orgUpd).futureValue
        val res = actorDao.getOrganizationById(3).futureValue
        res.get.fn mustBe "Museet i Bar"
        res.get.nickname mustBe "B"
        res.get.tel mustBe "99344321"
        res.get.web mustBe "www.bar.no"
      }

      "not update organization with invalid id" in {
        val orgUpd = Organization(
          id = Some(999991),
          fn = "Museet i Bar99",
          nickname = "B",
          tel = "99344321",
          web = "www.bar.no"
        )
        val res = actorDao.updateOrganization(orgUpd).futureValue
        res.isSuccess mustBe true
        res.get mustBe None
      }

      "succeed when deleting organization" in {
        actorDao.deleteOrganization(3).futureValue mustBe 1
        actorDao.getOrganizationById(3).futureValue mustBe None
      }

      "not be able to delete organization with invalid id" in {
        actorDao.deleteOrganization(999999).futureValue mustBe 0
      }

    }

    "modifying organizationAddress" should {

      "succeed when inserting organizationAddress" in {
        val orgAddr = OrganizationAddress(
          id = None,
          Some(2),
          "WORK",
          "Adressen",
          "Oslo",
          "0123",
          "Norway",
          60.11, 11.60
        )
        val res = actorDao.insertOrganizationAddress(orgAddr).futureValue
        res.addressType mustBe "WORK"
        res.streetAddress mustBe "Adressen"
        res.postalCode mustBe "0123"
        res.id mustBe Some(2)
      }

      "succeed when updating organizationAddress" in {
        val orgAddr1 = OrganizationAddress(
          id = None,
          organizationId = Some(2),
          addressType = "WORK2",
          streetAddress = "Adressen2",
          locality = "Bergen",
          postalCode = "0122",
          countryName = "Norway2",
          latitude = 60.11,
          longitude = 11.60
        )
        val res1 = actorDao.insertOrganizationAddress(orgAddr1).futureValue
        res1.addressType mustBe "WORK2"
        res1.streetAddress mustBe "Adressen2"
        res1.postalCode mustBe "0122"
        res1.id mustBe Some(3)

        val orgUpd = Organization(
          id = Some(3),
          fn = "Museet i Bar",
          nickname = "B",
          tel = "99344321",
          web = "www.bar.no"
        )
        val orgAddrUpd = OrganizationAddress(
          id = Some(3),
          organizationId = Some(2),
          addressType = "WORK3",
          streetAddress = "Adressen3",
          locality = "Bergen3",
          postalCode = "0133",
          countryName = "Norway3",
          latitude = 60.11,
          longitude = 11.60
        )

        val resInt = actorDao.updateOrganizationAddress(orgAddrUpd).futureValue
        resInt.isSuccess mustBe true
        resInt.get mustBe Some(1)
        val res = actorDao.getOrganizationAddressById(3).futureValue
        res must not be None
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
        val orgAddrUpd = OrganizationAddress(
          id = Some(9999992),
          organizationId = Some(2),
          addressType = "WORK3",
          streetAddress = "Adressen3",
          locality = "Bergen3",
          postalCode = "0133",
          countryName = "Norway3",
          latitude = 60.11,
          longitude = 11.60
        )
        val res = actorDao.updateOrganizationAddress(orgAddrUpd).futureValue
        res.isSuccess mustBe true
        res.get mustBe None
      }

      "not update organization address with missing id" in {
        val orgAddrUpd = OrganizationAddress(
          id = None,
          organizationId = Some(2),
          addressType = "WORK3",
          streetAddress = "Adressen3",
          locality = "Bergen3",
          postalCode = "0133",
          countryName = "Norway3",
          latitude = 60.11,
          longitude = 11.60
        )
        val res = actorDao.updateOrganizationAddress(orgAddrUpd).futureValue
        res.isSuccess mustBe true
        res.get mustBe None
      }

      "not update organization address with invalid organization id" in {
        val orgAddrUpd = OrganizationAddress(
          id = Some(3),
          organizationId = Some(9999993),
          addressType = "WORK3",
          streetAddress = "Adressen3",
          locality = "Bergen3",
          postalCode = "0133",
          countryName = "Norway3",
          latitude = 60.11,
          longitude = 11.60
        )
        // FIXME: This test assumes exception...there's nothing exceptional about invalid ID's
        whenReady(actorDao.updateOrganizationAddress(orgAddrUpd).failed) { e =>
          e mustBe a[org.h2.jdbc.JdbcSQLException]
          e.getMessage must startWith("Referential integrity")
        }
      }

      "succeed when deleting organization address" in {
        actorDao.deleteOrganizationAddress(3).futureValue mustBe 1
        actorDao.getOrganizationAddressById(3).futureValue mustBe None
      }

      "not be able to delete organization address with invalid id" in {
        actorDao.deleteOrganizationAddress(999999).futureValue mustBe 0
      }
    }

    "querying person details" should {

      "get person details" in {
        val ids = Set(1L, 2L, 3L)
        val persons = actorDao.getPersonDetailsByIds(ids).futureValue
        persons.length mustBe 2
        persons.head.fn mustBe "And, Arne1"
        persons.tail.head.fn mustBe "Kanin, Kalle1"
      }
    }

    "queriyng finders" should {

      "get all organization addresses" in {
        val orgAddrs = actorDao.allAddressesForOrganization(1).futureValue
        orgAddrs.length mustBe 1
        orgAddrs.head.streetAddress mustBe "Fredriks gate 2"
      }
    }

    "dataporten integration" should {

      "return a Person if the dataportenId is valid" in {
        val uid = "a1a2a3a4-adb2-4b49-bce3-320ddfe6c90f"
        val newPerson = Person(Some(2), "Herr Larmerud", dataportenId = Some(uid))
        val personId = actorDao.insertPersonLegacy(newPerson).futureValue.id.get
        val res = actorDao.getPersonByDataportenId(uid).futureValue
        res.isDefined mustBe true
        val person = res.get
        person.fn mustBe "Herr Larmerud"
        person.dataportenId mustBe Some(uid)
        person.id mustBe Some(personId)
        // We don't have a way to delete legacyPersons, may want to delete the
        // newly inserted actor in the future: actorDao.deletePerson(personId)
      }

      "not find actor with unknown dataportenId" in {
        actorDao.getPersonByDataportenId("tullballId").futureValue.isDefined mustBe false
      }

      "not find actor with unknown dataportenId based on security connection etc" in {
        val authenticator = new FakeAuthenticator
        val fakeUsrId = "jarle"
        val fakeToken = BearerToken(FakeAuthenticator.fakeAccessTokenPrefix + fakeUsrId)
        val fakeAuthUsr = AuthenticatedUser(authenticator.userInfo(fakeToken).futureValue.get, Seq.empty)

        actorDao.getPersonByDataportenId(fakeAuthUsr.userInfo.id).futureValue.isDefined mustBe false

        val person = actorDao.insertAuthenticatedUser(fakeAuthUsr).futureValue
        person.dataportenId mustBe Some(fakeAuthUsr.userInfo.id)
        person.fn mustBe fakeAuthUsr.userInfo.name.get
        person.email mustBe fakeAuthUsr.userInfo.email

        val res2 = actorDao.getPersonByDataportenId(fakeAuthUsr.userInfo.id).futureValue
        res2.isDefined mustBe true
        res2.get.fn mustBe fakeAuthUsr.userInfo.name.get
      }
    }
  }
}
