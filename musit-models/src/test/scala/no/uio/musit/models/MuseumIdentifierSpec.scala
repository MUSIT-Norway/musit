package no.uio.musit.models

import org.scalatestplus.play.PlaySpec

class MuseumIdentifierSpec extends PlaySpec {
  "Interacting with MuseumIdentifier" when {
    "only MusemNO is set" should {
      "should get MuseumIdentifier with only museumNo and slash" in {
        val sqlString = "CH3456/"
        val ident     = MuseumIdentifier.fromSqlString(sqlString)
        ident.museumNo mustBe "CH3456"
        ident.subNo mustBe Some("")
      }
      "get MuseumIdentifier with only museumNo and no slash" in {
        val sqlString = "CH3456"
        val ident     = MuseumIdentifier.fromSqlString(sqlString)
        ident.museumNo mustBe "CH3456"
        ident.subNo mustBe None
      }
      "get MuseumIdentifier with museumNo with subNo and subSubNo" in {
        val sqlString = "CH3456/33/AB34"
        val ident     = MuseumIdentifier.fromSqlString(sqlString)
        ident.museumNo mustBe "CH3456"
        ident.subNo mustBe Some("33/AB34")
      }
      "get MuseumIdentifier with museumNo and subNo (with wrong subNo)" in {
        val sqlString = "CH3456//AB34"
        val ident     = MuseumIdentifier.fromSqlString(sqlString)
        ident.museumNo mustBe "CH3456"
        ident.subNo mustBe Some("/AB34")
      }
    }
  }
}
