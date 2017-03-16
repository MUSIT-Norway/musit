package repositories.analysis.dao

import models.analysis.events.EventCategories.Dating
import no.uio.musit.models.MuseumCollections.Entomology
import no.uio.musit.test.MusitSpecWithAppPerSuite
import org.scalatest.Inspectors.forAll

class AnalysisTypeDaoSpec extends MusitSpecWithAppPerSuite {

  val dao: AnalysisTypeDao = fromInstanceCache[AnalysisTypeDao]

  "The AnalysisTypeDao" should {
    "return all the stored analysis types" in {
      val res = dao.all.futureValue

      res.isSuccess mustBe true
      val ats = res.get

      ats.size mustBe 107
      ats.map(_.category.entryName).distinct.size mustBe 21
    }

    "return all analysis types in the Dating category" in {
      val res = dao.allForCategory(Dating).futureValue

      res.isSuccess mustBe true
      val ats = res.get

      ats.size mustBe 8

      forAll(ats) { a =>
        a.category mustBe Dating
        a.extraAttributes mustBe Some(Map("age" -> "String"))
      }
    }

    "return all analysis types for a specific collection" in {
      val entoUUID = Entomology.uuid // scalastyle:ignore

      val res = dao.allForCollection(entoUUID).futureValue

      res.isSuccess mustBe true
      val ats = res.get

      ats.size mustBe 28

      forAll(ats) { t =>
        (t.collections.contains(entoUUID) || t.collections.isEmpty) mustBe true
      }
    }
  }

}
