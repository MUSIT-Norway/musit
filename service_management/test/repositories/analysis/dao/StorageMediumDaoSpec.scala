package repositories.analysis.dao

import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues

class StorageMediumDaoSpec extends MusitSpecWithAppPerSuite with MusitResultValues {

  val dao: StorageMediumDao = fromInstanceCache[StorageMediumDao]

  "The StorageMediumDao " should {
    "return storage medium list" in {
      val list = dao.getStorageMediumList.futureValue.successValue
      list.size mustBe 26
      //list.head.noTreatment mustBe "CTAB"
    }
  }

}
