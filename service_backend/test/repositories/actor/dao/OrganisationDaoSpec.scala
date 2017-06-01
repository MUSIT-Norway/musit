package repositories.actor.dao

import models.actor.Organisation
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
          fullName = "Kulturhistorisk museum - Universitetet i Oslo",
          tel = Some("22 85 19 00"),
          web = Some("www.khm.uio.no"),
          synonyms = Some(Seq("KHM")),
          serviceTags = Some(Seq("storage_facility"))
        )
        val res = orgDao.getById(oid).futureValue
        expected.id mustBe res.get.id
        expected.fullName mustBe res.get.fullName
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
        val res = orgDao.getByNameAndTags("Kulturhis", "storage_facility").futureValue
        res.size must be > 0
        res.size mustBe 1
      }

      "return list if the serviceTags is empty " in {
        val res = orgDao.getByName("Arkeologisk").futureValue
        res.size must be > 0
        res.size mustBe 1
      }

      "return lab list for analysis" in {
        val res = orgDao.getAnalysisLabList.futureValue.get
        res.size must be > 7
      }
    }

    "modifying organization" should {

      "succeed when inserting organization" in {
        val org = Organisation(
          id = None,
          fullName = "Testmuseet i Bergen",
          tel = Some("99887766"),
          web = Some("www.tmib.no"),
          synonyms = Some(Seq("UM")),
          serviceTags = Some(Seq("storage_facility"))
        )
        val res = orgDao.insert(org).futureValue
        res.fullName mustBe "Testmuseet i Bergen"
        res.id mustBe Some(OrgId(363))
      }

      "succeed when updating organization" in {
        val org1 = Organisation(
          id = None,
          fullName = "Museet i Foobar",
          tel = Some("12344321"),
          web = Some("www.foob.no"),
          synonyms = Some(Seq("Foo")),
          serviceTags = Some(Seq("storage_facility"))
        )
        val res1 = orgDao.insert(org1).futureValue
        res1.fullName mustBe "Museet i Foobar"
        res1.id mustBe Some(OrgId(364))

        val orgUpd = Organisation(
          id = Some(OrgId(364)),
          fullName = "Museet i Bar",
          tel = Some("99344321"),
          web = Some("www.bar.no"),
          synonyms = Some(Seq("MusBar")),
          serviceTags = Some(Seq("storage_facility"))
        )

        val resInt = orgDao.update(orgUpd).futureValue
        val res    = orgDao.getById(OrgId(364)).futureValue
        res.get.fullName mustBe "Museet i Bar"
        res.get.tel mustBe Some("99344321")
        res.get.web mustBe Some("www.bar.no")
      }

      "not update organization with invalid id" in {
        val orgUpd = Organisation(
          id = Some(OrgId(999991)),
          fullName = "Museet i Bar99",
          tel = Some("99344321"),
          web = Some("www.bar.no"),
          synonyms = Some(Seq("MusBar99", "MusBar")),
          serviceTags = Some(Seq("storage_facility"))
        )
        val res = orgDao.update(orgUpd).futureValue
        res.isSuccess mustBe true
        res.get mustBe None
      }

      "succeed when deleting organization" in {
        orgDao.delete(OrgId(364)).futureValue mustBe 1
        orgDao.getById(OrgId(364)).futureValue mustBe None
      }

      "not be able to delete organization with invalid id" in {
        orgDao.delete(OrgId(3)).futureValue mustBe 0
      }
    }
  }
}
