package migration

import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues
import repositories.storage.dao.MigrationDao
import repositories.storage.dao.events.{ControlDao, EnvReqDao, MoveDao, ObservationDao}
import utils.testhelpers.{EventGenerators_Old, MigrationTest, NodeGenerators}

// TODO: This can be removed when Migration has been performed.

class EventMigratorSpec
    extends MusitSpecWithAppPerSuite
    with NodeGenerators
    with EventGenerators_Old
    with MusitResultValues
    with MigrationTest {

  val ctrlDao: ControlDao    = fromInstanceCache[ControlDao]
  val obsDao: ObservationDao = fromInstanceCache[ObservationDao]
  val envReqDao: EnvReqDao   = fromInstanceCache[EnvReqDao]
  val mvDao: MoveDao         = fromInstanceCache[MoveDao]

  val migrationDao: MigrationDao = fromInstanceCache[MigrationDao]

  // In the test configs, we do not enable the DataMigrationModule. Hence
  // we need to initialise it manually using the constructor.
  val migrator = new EventMigrator(
    migrationDao,
    oldLocDao,
    ctrlDao,
    obsDao,
    envReqDao,
    mvDao,
    mat,
    as
  )

  "The EventMigrator" should {

    s"migrate all old events and local objects to new versions" in {
      val numInsOld = bootstrap().futureValue
      val oldEvents = migrationDao.countOld.futureValue

      migrator.migrateAll().futureValue mustBe oldEvents

      val res = migrator.verify().futureValue
      res mustBe MigrationVerification(50, 50, 50, 10, 101)
      res.total mustBe oldEvents
    }

  }

}
