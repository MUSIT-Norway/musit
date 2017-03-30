package repositories.storage.dao

import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues
import utils.testhelpers.BaseDummyData

class MigrationDaoSpec
    extends MusitSpecWithAppPerSuite
    with MusitResultValues
    with BaseDummyData {

  val migrationDao = fromInstanceCache[MigrationDao]

  "MigrationDao" should {
    "successfully fetch ObjectUUIDs and MuseumId for a given ObjectId" in {
      pending
    }
  }

}
