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
      fn = "Kulturhistorisk museum - Universitetet i Oslo",
      tel = "22 85 19 00",
      web = "www.khm.uio.no",
      synonyms = Some(Seq("KHM")),
      serviceTags = Some(Seq("storage_facility"))
    )
    orgDao.insert(org).futureValue must not be None
  }

  "AddressDao" when {

    "inserting organisation addresses" should {
      "succeed when inserting organizationAddress" in {
        val orgAddr = OrganisationAddress(
          id = None,
          organisationId = Some(OrgId(1)),
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
        res.id mustBe Some(DatabaseId(21))
      }
    }

    "modifying organizationAddress" should {

      "succeed when updating organizationAddress" in {
        val orgAddr1 = OrganisationAddress(
          id = None,
          organisationId = Some(OrgId(1)),
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
        res1.id mustBe Some(DatabaseId(22))

        val orgAddrUpd = OrganisationAddress(
          id = Some(DatabaseId(22)),
          organisationId = Some(OrgId(1)),
          addressType = "WORK3",
          streetAddress = "Adressen3",
          locality = "Bergen3",
          postalCode = "0133",
          countryName = "Norway3",
          latitude = 60.11,
          longitude = 11.60
        )

        val resInt = adrDao.update(orgAddrUpd).futureValue
        resInt.successValue mustBe Some(1)
        val res = adrDao.getById(OrgId(1), DatabaseId(22)).futureValue
        res.value.id mustBe Some(DatabaseId(22))
        res.value.organisationId mustBe Some(OrgId(1))
        res.value.addressType mustBe "WORK3"
        res.value.streetAddress mustBe "Adressen3"
        res.value.locality mustBe "Bergen3"
        res.value.postalCode mustBe "0133"
        res.value.countryName mustBe "Norway3"
        res.value.latitude mustBe 60.11
        res.value.longitude mustBe 11.60
      }

      "not update organisation address with invalid id" in {
        val orgAddrUpd = OrganisationAddress(
          id = Some(DatabaseId(9999992)),
          organisationId = Some(OrgId(1)),
          addressType = "WORK3",
          streetAddress = "Adressen3",
          locality = "Bergen3",
          postalCode = "0133",
          countryName = "Norway3",
          latitude = 60.11,
          longitude = 11.60
        )
        val res = adrDao.update(orgAddrUpd).futureValue
        res.successValue mustBe None
      }

      "not update organisation address with missing id" in {
        val orgAddrUpd = OrganisationAddress(
          id = None,
          organisationId = Some(OrgId(1)),
          addressType = "WORK3",
          streetAddress = "Adressen3",
          locality = "Bergen3",
          postalCode = "0133",
          countryName = "Norway3",
          latitude = 60.11,
          longitude = 11.60
        )
        val res = adrDao.update(orgAddrUpd).futureValue
        res.successValue mustBe None
      }

      "not update organisation address with invalid organisation id" in {
        val orgAddrUpd = OrganisationAddress(
          id = Some(DatabaseId(22)),
          organisationId = Some(OrgId(9999993)),
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
        orgAddrs.head.streetAddress mustBe "Fredriks gate 2"
        orgAddrs.head.postalCode mustBe "0255"
      }
    }

  }
}
