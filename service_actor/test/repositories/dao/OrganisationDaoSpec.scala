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

import models.Organisation
import no.uio.musit.test.MusitSpecWithAppPerSuite
import org.scalatest.time.{Millis, Seconds, Span}

class OrganisationDaoSpec extends MusitSpecWithAppPerSuite {

  val orgDao: OrganisationDao = fromInstanceCache[OrganisationDao]

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  "OrganisationDao" when {

    "querying the organization methods" should {

      "return None when Id is very large" in {
        orgDao.getById(Long.MaxValue).futureValue mustBe None
      }

      "return a organization if the Id is valid" in {
        val expected = Organisation(
          id = Some(1),
          fn = "Kulturhistorisk museum - Universitetet i Oslo",
          nickname = "KHM",
          tel = "22 85 19 00",
          web = "www.khm.uio.no"
        )
        val res = orgDao.getById(1).futureValue
        expected.id mustBe res.get.id
        expected.fn mustBe res.get.fn
        expected.nickname mustBe res.get.nickname
        expected.tel mustBe res.get.tel
        expected.web mustBe res.get.web
      }

      "return None if the Id is 0 (zero)" in {
        orgDao.getById(0).futureValue mustBe None
      }

      "return empty list if the search string is not found" in {
        orgDao.getByName("Andlkjlkj").futureValue mustBe empty
      }
    }

    "modifying organization" should {

      "succeed when inserting organization" in {
        val org = Organisation(
          id = None,
          fn = "Testmuseet i Bergen",
          nickname = "TM",
          tel = "99887766",
          "www.tmib.no"
        )
        val res = orgDao.insert(org).futureValue
        res.fn mustBe "Testmuseet i Bergen"
        res.id mustBe Some(2)
      }

      "succeed when updating organization" in {
        val org1 = Organisation(
          id = None,
          fn = "Museet i Foobar",
          nickname = "FB",
          tel = "12344321",
          web = "www.foob.no"
        )
        val res1 = orgDao.insert(org1).futureValue
        res1.fn mustBe "Museet i Foobar"
        res1.id mustBe Some(3)

        val orgUpd = Organisation(
          id = Some(3),
          fn = "Museet i Bar",
          nickname = "B",
          tel = "99344321",
          web = "www.bar.no"
        )

        val resInt = orgDao.update(orgUpd).futureValue
        val res = orgDao.getById(3).futureValue
        res.get.fn mustBe "Museet i Bar"
        res.get.nickname mustBe "B"
        res.get.tel mustBe "99344321"
        res.get.web mustBe "www.bar.no"
      }

      "not update organization with invalid id" in {
        val orgUpd = Organisation(
          id = Some(999991),
          fn = "Museet i Bar99",
          nickname = "B",
          tel = "99344321",
          web = "www.bar.no"
        )
        val res = orgDao.update(orgUpd).futureValue
        res.isSuccess mustBe true
        res.get mustBe None
      }

      "succeed when deleting organization" in {
        orgDao.delete(3).futureValue mustBe 1
        orgDao.getById(3).futureValue mustBe None
      }

      "not be able to delete organization with invalid id" in {
        orgDao.delete(999999).futureValue mustBe 0
      }
    }
  }
}
