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
        val oid = OrgId(359)
        val expected = Organisation(
          id = Some(oid),
          fullName = "Beta Analytic Limited",
          tel = Some("442076177459"),
          web = Some("http://www.radiocarbon.com/"),
          synonyms = None,
          serviceTags = Some(Seq("analysis")),
          contact = None,
          email = Some("lab@radiocarbon.com")
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

      "return list if the serviceTags is analysis" in {
        val res = orgDao.getByNameAndTags("Anders Lindahl", "analysis").futureValue
        res.size must be > 0
        res.size mustBe 1
      }

      "return list if the serviceTags is empty " in {
        val res = orgDao.getByName("Arkeologisk").futureValue
        res.size must be > 0
        res.size mustBe 2
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
          serviceTags = Some(Seq("storage_facility")),
          contact = Some("Ellen"),
          email = Some("ellen@hurra.no")
        )
        val res = orgDao.insert(org).futureValue
        res.fullName mustBe "Testmuseet i Bergen"
        res.id mustBe Some(OrgId(395))
      }

      "succeed when updating organization" in {
        val org1 = Organisation(
          id = None,
          fullName = "Museet i Foobar",
          tel = Some("12344321"),
          web = Some("www.foob.no"),
          synonyms = Some(Seq("Foo")),
          serviceTags = Some(Seq("storage_facility")),
          contact = Some("Foo"),
          email = Some("foo@hurra.no")
        )
        val res1 = orgDao.insert(org1).futureValue
        res1.fullName mustBe "Museet i Foobar"
        res1.id mustBe Some(OrgId(396))

        val orgUpd = Organisation(
          id = Some(OrgId(396)),
          fullName = "Museet i Bar",
          tel = Some("99344321"),
          web = Some("www.bar.no"),
          synonyms = Some(Seq("MusBar")),
          serviceTags = Some(Seq("storage_facility")),
          contact = Some("Bar"),
          email = Some("bar@hurra.no")
        )

        val resInt = orgDao.update(orgUpd).futureValue
        val res    = orgDao.getById(OrgId(396)).futureValue
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
          serviceTags = Some(Seq("storage_facility")),
          contact = Some("Nils"),
          email = Some("nils@hurra.no")
        )
        val res = orgDao.update(orgUpd).futureValue
        res.isSuccess mustBe true
        res.get mustBe None
      }

      "succeed when deleting organization" in {
        orgDao.delete(OrgId(396)).futureValue mustBe 1
        orgDao.getById(OrgId(396)).futureValue mustBe None
      }

      "not be able to delete organization with invalid id" in {
        orgDao.delete(OrgId(3)).futureValue mustBe 0
      }
    }
  }
}
