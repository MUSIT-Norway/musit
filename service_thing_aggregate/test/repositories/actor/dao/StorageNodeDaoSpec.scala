package repositories.actor.dao

import helpers.NodeTestData
import no.uio.musit.MusitResults.MusitSuccess
import no.uio.musit.models.{MuseumId, StorageNodeId}
import no.uio.musit.test.MusitSpecWithAppPerSuite

class StorageNodeDaoSpec extends MusitSpecWithAppPerSuite with NodeTestData {

  val dao: StorageNodeDao = fromInstanceCache[StorageNodeDao]

  "Interacting with the StorageNodeDao" when {

    "checking if a node exists" should {

      "return false if the node doesn't exists" in {
        dao.nodeExists(MuseumId(99), StorageNodeId.generate()).futureValue match {
          case MusitSuccess(false) =>
          case _                   => fail("it should not exist")
        }
      }

      "return true if the node exists" in {
        dao.nodeExists(MuseumId(99), nodeId4).futureValue match {
          case MusitSuccess(true) =>
          case _                  => fail("it should exist")
        }
      }

      "return false if the node doesn't exist for the museum" in {
        dao.nodeExists(MuseumId(55), nodeId4).futureValue match {
          case MusitSuccess(false) =>
          case _                   => fail("it should not exist for museum 55")
        }
      }
    }
  }
}
