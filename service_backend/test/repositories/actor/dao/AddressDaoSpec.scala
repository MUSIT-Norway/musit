package repositories.actor.dao

import models.actor.{Organisation, OrganisationAddress}
import no.uio.musit.models.{DatabaseId, OrgId}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues
import org.scalatest.BeforeAndAfterAll

class AddressDaoSpec
    extends MusitSpecWithAppPerSuite
    with BeforeAndAfterAll
    with MusitResultValues {

  val adrDao: AddressDao      = fromInstanceCache[AddressDao]
  val orgDao: OrganisationDao = fromInstanceCache[OrganisationDao]

  override def beforeAll(): Unit = {
    val org = Organisation(
      id = Some(OrgId(1)),
      fullName = "Kulturhistorisk museum - Universitetet i Oslo",
      tel = Some("22 85 19 00"),
      web = Some("www.khm.uio.no"),
      synonyms = Some(Seq("KHM")),
      serviceTags = Some(Seq("storage_facility")),
      contact = Some("Knut"),
      email = Some("knut@hurra.no")
    )
    orgDao.insert(org).futureValue must not be None
  }

  "AddressDao" when {

    "inserting organisation addresses" should {
      "succeed when inserting organizationAddress" in {
        val orgAddr = OrganisationAddress(
          id = None,
          organisationId = Some(OrgId(1)),
          streetAddress = Some("Adressen"),
          streetAddress2 = Some("adresse2"),
          postalCodePlace = "0123",
          countryName = "Norway"
        )
        val res = adrDao.insert(orgAddr).futureValue
        res.streetAddress2 mustBe Some("adresse2")
        res.streetAddress mustBe Some("Adressen")
        res.postalCodePlace mustBe "0123"
        res.id mustBe Some(DatabaseId(41))
      }
    }

    "modifying organizationAddress" should {

      "succeed when updating organizationAddress" in {
        val orgAddr1 = OrganisationAddress(
          id = None,
          organisationId = Some(OrgId(1)),
          streetAddress = Some("Adressen2"),
          streetAddress2 = Some("postadressen"),
          postalCodePlace = "0122",
          countryName = "Norway2"
        )
        val res1 = adrDao.insert(orgAddr1).futureValue
        res1.streetAddress2 mustBe Some("postadressen")
        res1.streetAddress mustBe Some("Adressen2")
        res1.postalCodePlace mustBe "0122"
        res1.id mustBe Some(DatabaseId(42))

        val orgAddrUpd = OrganisationAddress(
          id = Some(DatabaseId(42)),
          organisationId = Some(OrgId(1)),
          streetAddress = Some("Adressen3"),
          streetAddress2 = Some("postboks"),
          postalCodePlace = "0133",
          countryName = "Norway3"
        )

        val resInt = adrDao.update(orgAddrUpd).futureValue
        resInt.successValue mustBe Some(1)
        val res = adrDao.getById(OrgId(1), DatabaseId(42)).futureValue
        res.value.id mustBe Some(DatabaseId(42))
        res.value.organisationId mustBe Some(OrgId(1))
        res.value.streetAddress mustBe Some("Adressen3")
        res.value.streetAddress2 mustBe Some("postboks")
        res.value.postalCodePlace mustBe "0133"
        res.value.countryName mustBe "Norway3"
      }

      "not update on organisation address with invalid id" in {
        val orgAddrUpd = OrganisationAddress(
          id = Some(DatabaseId(9999992)),
          organisationId = Some(OrgId(1)),
          streetAddress = Some("Adressen3"),
          streetAddress2 = Some("Bergen3"),
          postalCodePlace = "0133",
          countryName = "Norway3"
        )
        val res = adrDao.update(orgAddrUpd).futureValue
        res.successValue mustBe None
      }

      "not update on organisation address with missing id" in {
        val orgAddrUpd = OrganisationAddress(
          id = None,
          organisationId = Some(OrgId(1)),
          streetAddress = Some("Adressen3"),
          streetAddress2 = Some("Bergen3"),
          postalCodePlace = "0133",
          countryName = "Norway3"
        )
        val res = adrDao.update(orgAddrUpd).futureValue
        res.successValue mustBe None
      }

      "not update on organisation address with invalid organisation id" in {
        val orgAddrUpd = OrganisationAddress(
          id = Some(DatabaseId(22)),
          organisationId = Some(OrgId(9999993)),
          streetAddress = Some("Adressen3"),
          streetAddress2 = Some("Bergen3"),
          postalCodePlace = "0133",
          countryName = "Norway3"
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
        adrDao.delete(DatabaseId(22)).futureValue mustBe 1
        adrDao.getById(OrgId(1), DatabaseId(22)).futureValue mustBe None
      }

      "not be able to delete organisation address with invalid id" in {
        adrDao.delete(DatabaseId(999999)).futureValue mustBe 0
      }
    }

    "retrieving addresses" should {

      "find all organisation addresses" in {
        val orgAddrs = adrDao.allFor(OrgId(1)).futureValue
        orgAddrs.size mustBe 2
        orgAddrs.head.streetAddress mustBe Some("Adressen")
        orgAddrs.head.postalCodePlace mustBe "0123"
      }
    }

  }
}
