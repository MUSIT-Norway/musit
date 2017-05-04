package repositories.analysis.dao

import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues

class TreatmentDaoSpec extends MusitSpecWithAppPerSuite with MusitResultValues {

  val dao: TreatmentDao = fromInstanceCache[TreatmentDao]

  "The TreatmentDao " should {
    "return treatment list" in {
      val list = dao.getTreatmentList.futureValue.successValue
      list.size mustBe 24
      list.head.noTreatment mustBe "CTAB"
    }
  }

}
