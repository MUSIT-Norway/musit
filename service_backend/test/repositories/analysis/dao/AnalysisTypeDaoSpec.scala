package repositories.analysis.dao

import models.analysis.events.EventCategories.Dating
import no.uio.musit.models.MuseumCollections.Entomology
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues
import org.scalatest.Inspectors.forAll

class AnalysisTypeDaoSpec extends MusitSpecWithAppPerSuite with MusitResultValues {

  val dao: AnalysisTypeDao = fromInstanceCache[AnalysisTypeDao]

  "The AnalysisTypeDao" should {
    "return all the stored analysis types" in {
      val res = dao.all.futureValue

      val ats = res.successValue
      ats.size mustBe 46
      ats.map(_.category.entryName).distinct.size mustBe 13
    }

    "return all analysis types in the Dating category" in {
      val res = dao.allForCategory(Dating).futureValue

      val ats = res.successValue
      ats.size mustBe 3

      val expResAttrs1 = Map(
        "ageEstimate"       -> "String",
        "standardDeviation" -> "String"
      )

      val expResAttrs2 = Map("age" -> "String")

      forAll(ats) { a =>
        a.category mustBe Dating
        a.extraResultAttributes must contain oneOf (expResAttrs1, expResAttrs2)
      }
    }

    "return all analysis types for a specific collection" ignore {
      val entoUUID = Entomology.uuid // scalastyle:ignore

      val res = dao.allForCollection(entoUUID).futureValue

      val ats = res.successValue
      ats.size mustBe 28

      forAll(ats) { t =>
        (t.collections.contains(entoUUID) || t.collections.isEmpty) mustBe true
      }
    }
  }

}
