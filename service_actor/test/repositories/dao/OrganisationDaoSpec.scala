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
import no.uio.musit.models.OrgId
import no.uio.musit.test.MusitSpecWithAppPerSuite

class OrganisationDaoSpec extends MusitSpecWithAppPerSuite {

  val orgDao: OrganisationDao = fromInstanceCache[OrganisationDao]

  "OrganisationDao" when {

    "querying the organization methods" should {

      "return None when Id is very large" in {
        orgDao.getById(Long.MaxValue).futureValue mustBe None
      }

      "return a organization if the Id is valid" in {
        val oid = OrgId(1)
        val expected = Organisation(
          id = Some(oid),
          fn = "Kulturhistorisk museum - Universitetet i Oslo",
          tel = "22 85 19 00",
          web = "www.khm.uio.no",
          synonyms = Some(Seq("KHM")),
          serviceTags = Some(Seq("storage_facility"))
        )
        val res = orgDao.getById(oid).futureValue
        expected.id mustBe res.get.id
        expected.fn mustBe res.get.fn
        expected.tel mustBe res.get.tel
        expected.web mustBe res.get.web
      }

      "return None if the Id is 0 (zero)" in {
        orgDao.getById(OrgId(0)).futureValue mustBe None
      }

      "return empty list if the search string is not found" in {
        orgDao.getByName("Andlkjlkj").futureValue mustBe empty
      }

      "return list if the serviceTags is storage_facility" in {
        val res = orgDao.getByNameAndTags("kulturhis", "storage_facility").futureValue
        res.size must be > 0
        res.size mustBe 8
      }
    }

    "modifying organization" should {

      "succeed when inserting organization" in {
        val org = Organisation(
          id = None,
          fn = "Testmuseet i Bergen",
          tel = "99887766",
          web = "www.tmib.no",
          synonyms = Some(Seq("UM")),
          serviceTags = Some(Seq("storage_facility"))
        )
        val res = orgDao.insert(org).futureValue
        res.fn mustBe "Testmuseet i Bergen"
        res.id mustBe Some(OrgId(2))
      }

      "succeed when updating organization" in {
        val org1 = Organisation(
          id = None,
          fn = "Museet i Foobar",
          tel = "12344321",
          web = "www.foob.no",
          synonyms = Some(Seq("Foo")),
          serviceTags = Some(Seq("storage_facility"))
        )
        val res1 = orgDao.insert(org1).futureValue
        res1.fn mustBe "Museet i Foobar"
        res1.id mustBe Some(OrgId(3))

        val orgUpd = Organisation(
          id = Some(OrgId(3)),
          fn = "Museet i Bar",
          tel = "99344321",
          web = "www.bar.no",
          synonyms = Some(Seq("MusBar")),
          serviceTags = Some(Seq("storage_facility"))
        )

        val resInt = orgDao.update(orgUpd).futureValue
        val res = orgDao.getById(OrgId(3)).futureValue
        res.get.fn mustBe "Museet i Bar"
        res.get.tel mustBe "99344321"
        res.get.web mustBe "www.bar.no"
      }

      "not update organization with invalid id" in {
        val orgUpd = Organisation(
          id = Some(OrgId(999991)),
          fn = "Museet i Bar99",
          tel = "99344321",
          web = "www.bar.no",
          synonyms = Some(Seq("MusBar99", "MusBar")),
          serviceTags = Some(Seq("storage_facility"))
        )
        val res = orgDao.update(orgUpd).futureValue
        res.isSuccess mustBe true
        res.get mustBe None
      }

      "succeed when deleting organization" in {
        orgDao.delete(OrgId(3)).futureValue mustBe 1
        orgDao.getById(OrgId(3)).futureValue mustBe None
      }

      "not be able to delete organization with invalid id" in {
        orgDao.delete(OrgId(3)).futureValue mustBe 0
      }
    }
  }
}
