package repositories.dao

import no.uio.musit.models.{MuseumId, StorageNodeDatabaseId}
import no.uio.musit.MusitResults.MusitSuccess
import no.uio.musit.test.MusitSpecWithAppPerSuite

class StorageNodeDaoSpec extends MusitSpecWithAppPerSuite {

  val dao: StorageNodeDao = fromInstanceCache[StorageNodeDao]

  "Interacting with the StorageNodeDao" when {

    "getting objects for a nodeId that does not exist in a museum" should {
      "return false" in {
        dao.nodeExists(MuseumId(99), StorageNodeDatabaseId(9999)).futureValue match {
          case MusitSuccess(false) =>
          case _                   => fail("it should not exist")
        }
      }
    }

    "getting objects for a nodeId that exists in a museum" should {
      "return true" in {
        dao.nodeExists(MuseumId(99), StorageNodeDatabaseId(4)).futureValue match {
          case MusitSuccess(true) =>
          case _                  => fail("it should exist")
        }
      }
    }

    "getting objects using an invalid museum ID" should {
      "return true" in {
        dao.nodeExists(MuseumId(55), StorageNodeDatabaseId(4)).futureValue match {
          case MusitSuccess(false) =>
          case _                   => fail("it should not exist")
        }
      }
    }
  }
}
