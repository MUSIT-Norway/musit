package repositories.core.dao

import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues
import no.uio.musit.time
import org.scalatest.Inside

class IndexStatusDaoSpec
    extends MusitSpecWithAppPerSuite
    with MusitResultValues
    with Inside {

  val dao = fromInstanceCache[IndexStatusDao]

  "IndexStatusDao" should {
    val alias      = "musitalias"
    val indexTime  = time.dateTimeNow
    val updateTime = indexTime.plusMinutes(5)

    "return None when no entires exists" in {
      val res = dao.findLastIndexed(alias).futureValue.successValue

      res mustBe None
    }

    "return index time when indexed is registered" in {
      dao.indexed(alias, indexTime).futureValue.successValue

      val res = dao.findLastIndexed(alias).futureValue.successValue

      inside(res) {
        case Some(status) =>
          status.indexed mustBe indexTime
          status.updated mustBe None
      }
    }

    "return updated time when indexed is registered" in {
      dao.update(alias, updateTime).futureValue.successValue

      val res = dao.findLastIndexed(alias).futureValue.successValue

      inside(res) {
        case Some(status) =>
          status.indexed mustBe indexTime
          status.updated mustBe Some(updateTime)
      }
    }
    "clear update time when indexing is registered again" in {
      val newIndexTime = indexTime.plusMinutes(10)
      dao.indexed(alias, indexTime).futureValue.successValue
      dao.indexed(alias, newIndexTime).futureValue.successValue

      val res = dao.findLastIndexed(alias).futureValue.successValue

      inside(res) {
        case Some(status) =>
          status.indexed mustBe newIndexTime
          status.updated mustBe None
      }
    }

  }

}
