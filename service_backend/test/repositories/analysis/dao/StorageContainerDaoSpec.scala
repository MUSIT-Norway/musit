package repositories.analysis.dao

import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues

class StorageContainerDaoSpec extends MusitSpecWithAppPerSuite with MusitResultValues {

  val dao: StorageContainerDao = fromInstanceCache[StorageContainerDao]

  "The StorageContainerDao " should {
    "return storage container list" in {
      val list = dao.getStorageContainerList.futureValue.successValue
      list.size mustBe 30
      list.head.noStorageContainer mustBe "Eppendorfrør"
    }
  }

}
