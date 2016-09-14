import models.MuseumIdentifier
import org.scalatestplus.play.PlaySpec

class MuseumIdentifierSpec extends PlaySpec {
  "Interacting with MuseumIdentifier" when {
    "when only musemNO" should {
      "should get MuseumIdentifier with only museumNo and slash" in {
        val sqlString = "CH3456/"
        val ident = MuseumIdentifier.fromSqlString(sqlString)
        ident.museumNo mustBe "CH3456"
        ident.subNo mustBe None
      }
      "should get MuseumIdentifier with only museumNo and no slash" in {
        val sqlString = "CH3456"
        val ident = MuseumIdentifier.fromSqlString(sqlString)
        ident.museumNo mustBe "CH3456"
        ident.subNo mustBe None
      }
      "should get MuseumIdentifier with museumNo and subNo" in {
        val sqlString = "CH3456/AB34"
        val ident = MuseumIdentifier.fromSqlString(sqlString)
        ident.museumNo mustBe "CH3456"
        ident.subNo mustBe Some("AB34")
      }
    }
  }
}
