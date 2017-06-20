package repositories.analysis.dao

import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues

class SampleTypeDaoSpec extends MusitSpecWithAppPerSuite with MusitResultValues {

  val dao: SampleTypeDao = fromInstanceCache[SampleTypeDao]

  "The SampleTypeDao " should {
    "return sampleType list" in {
      val list = dao.getSampleTypeList.futureValue.successValue
      list.size mustBe 51
      list.head.noSampleType mustBe "DNA-ekstrakt"
      list.last.enSampleSubType mustBe Some("Grain size distribution (GSD) samples")
    }
  }

}
