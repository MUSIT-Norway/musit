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

import models.{Organisation, OrganisationAddress}
import no.uio.musit.models.{DatabaseId, OrgId}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import org.scalatest.BeforeAndAfterAll

class AddressDaoSpec extends MusitSpecWithAppPerSuite with BeforeAndAfterAll {

  val adrDao: AddressDao      = fromInstanceCache[AddressDao]
  val orgDao: OrganisationDao = fromInstanceCache[OrganisationDao]

  override def beforeAll(): Unit = {
    val org = Organisation(
      id = Some(OrgId(2)),
      fn = "Kulturhistorisk museum - Universitetet i Oslo",
      nickname = "KHM",
      tel = "22 85 19 00",
      web = "www.khm.uio.no"
    )
    orgDao.insert(org).futureValue must not be None
  }

  "AddressDao" when {

    "inserting organisation addresses" should {
      "succeed when inserting organizationAddress" in {
        val orgAddr = OrganisationAddress(
          id = None,
          organizationId = Some(OrgId(2)),
          addressType = "WORK",
          streetAddress = "Adressen",
          locality = "Oslo",
          postalCode = "0123",
          countryName = "Norway",
          latitude = 60.11,
          longitude = 11.60
        )
        val res = adrDao.insert(orgAddr).futureValue
        res.addressType mustBe "WORK"
        res.streetAddress mustBe "Adressen"
        res.postalCode mustBe "0123"
        res.id mustBe Some(DatabaseId(2))
      }
    }

    "modifying organizationAddress" should {

      "succeed when updating organizationAddress" in {
        val orgAddr1 = OrganisationAddress(
          id = None,
          organizationId = Some(OrgId(2)),
          addressType = "WORK2",
          streetAddress = "Adressen2",
          locality = "Bergen",
          postalCode = "0122",
          countryName = "Norway2",
          latitude = 60.11,
          longitude = 11.60
        )
        val res1 = adrDao.insert(orgAddr1).futureValue
        res1.addressType mustBe "WORK2"
        res1.streetAddress mustBe "Adressen2"
        res1.postalCode mustBe "0122"
        res1.id mustBe Some(DatabaseId(3))

        val orgUpd = Organisation(
          id = Some(OrgId(3)),
          fn = "Museet i Bar",
          nickname = "B",
          tel = "99344321",
          web = "www.bar.no"
        )
        val orgAddrUpd = OrganisationAddress(
          id = Some(DatabaseId(3)),
          organizationId = Some(OrgId(2)),
          addressType = "WORK3",
          streetAddress = "Adressen3",
          locality = "Bergen3",
          postalCode = "0133",
          countryName = "Norway3",
          latitude = 60.11,
          longitude = 11.60
        )

        val resInt = adrDao.update(orgAddrUpd).futureValue
        resInt.isSuccess mustBe true
        resInt.get mustBe Some(1)
        val res = adrDao.getById(DatabaseId(3)).futureValue
        res must not be None
        res.get.id mustBe Some(DatabaseId(3))
        res.get.organizationId mustBe Some(OrgId(2))
        res.get.addressType mustBe "WORK3"
        res.get.streetAddress mustBe "Adressen3"
        res.get.locality mustBe "Bergen3"
        res.get.postalCode mustBe "0133"
        res.get.countryName mustBe "Norway3"
        res.get.latitude mustBe 60.11
        res.get.longitude mustBe 11.60
      }

      "not update organisation address with invalid id" in {
        val orgAddrUpd = OrganisationAddress(
          id = Some(DatabaseId(9999992)),
          organizationId = Some(OrgId(2)),
          addressType = "WORK3",
          streetAddress = "Adressen3",
          locality = "Bergen3",
          postalCode = "0133",
          countryName = "Norway3",
          latitude = 60.11,
          longitude = 11.60
        )
        val res = adrDao.update(orgAddrUpd).futureValue
        res.isSuccess mustBe true
        res.get mustBe None
      }

      "not update organisation address with missing id" in {
        val orgAddrUpd = OrganisationAddress(
          id = None,
          organizationId = Some(OrgId(2)),
          addressType = "WORK3",
          streetAddress = "Adressen3",
          locality = "Bergen3",
          postalCode = "0133",
          countryName = "Norway3",
          latitude = 60.11,
          longitude = 11.60
        )
        val res = adrDao.update(orgAddrUpd).futureValue
        res.isSuccess mustBe true
        res.get mustBe None
      }

      "not update organisation address with invalid organisation id" in {
        val orgAddrUpd = OrganisationAddress(
          id = Some(DatabaseId(3)),
          organizationId = Some(OrgId(9999993)),
          addressType = "WORK3",
          streetAddress = "Adressen3",
          locality = "Bergen3",
          postalCode = "0133",
          countryName = "Norway3",
          latitude = 60.11,
          longitude = 11.60
        )
        // FIXME: This test assumes exception...there's nothing exceptional about invalid ID's
        whenReady(adrDao.update(orgAddrUpd).failed) { e =>
          e mustBe a[org.h2.jdbc.JdbcSQLException]
          e.getMessage must startWith("Referential integrity")
        }
      }

    }

    "deleting organisation addresses" should {
      "succeed when deleting organisation address" in {
        adrDao.delete(DatabaseId(3)).futureValue mustBe 1
        adrDao.getById(DatabaseId(3)).futureValue mustBe None
      }

      "not be able to delete organisation address with invalid id" in {
        adrDao.delete(DatabaseId(999999)).futureValue mustBe 0
      }
    }

    "retrieving addresses" should {

      "find all organisation addresses" in {
        val orgAddrs = adrDao.allFor(OrgId(1)).futureValue
        orgAddrs.length mustBe 1
        orgAddrs.head.streetAddress mustBe "Fredriks gate 2"
      }
    }

  }
}
