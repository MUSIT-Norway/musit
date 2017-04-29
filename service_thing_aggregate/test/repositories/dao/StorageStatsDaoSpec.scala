package repositories.dao

import helpers.NodeTestData
import no.uio.musit.MusitResults.MusitSuccess
import no.uio.musit.models._
import no.uio.musit.test.MusitSpecWithAppPerSuite

/**
 * ¡¡¡This spec relies on objects being inserted in the evolution script under
 * {{{src/test/resources/evolutions/default/1.sql script.}}}.
 * This is achieved by using relaxed constraints on primary and foreign key
 * references in comparison to the proper schema!!!
 */
class StorageStatsDaoSpec extends MusitSpecWithAppPerSuite with NodeTestData {

  val statsDao = fromInstanceCache[StorageStatsDao]

  "StorageStatsDao" should {

    "return the number of direct child nodes" in {
      statsDao.numChildren(nodeId4).futureValue mustBe MusitSuccess(3)
    }

    "return the number of objects on a node" in {
      statsDao.numObjectsInNode(nodeId6).futureValue mustBe MusitSuccess(34)
    }

    "return the total number of objects i a node hierarchy" in {
      val path = NodePath(",1,")
      statsDao.numObjectsInPath(path).futureValue mustBe MusitSuccess(52)
    }

  }

}
