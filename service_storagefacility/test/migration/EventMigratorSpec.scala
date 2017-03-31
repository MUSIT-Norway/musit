package migration

import models.storage.event.old.move.{
  MoveNode => OldMoveNode,
  MoveObject => OldMoveObject
}
import no.uio.musit.models.{ObjectId, StorageNodeDatabaseId}
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues
import org.scalatest.BeforeAndAfterAll
import org.scalatest.exceptions.TestFailedException
import repositories.storage.dao.MigrationDao
import repositories.storage.dao.events.{ControlDao, EnvReqDao, MoveDao, ObservationDao}
import repositories.storage.old_dao.event.EventDao
import repositories.storage.old_dao.{LocalObjectDao => OldLocObjDao}
import services.old.{
  ControlService => OldCtrlService,
  EnvironmentRequirementService => OldEnvReqService,
  ObservationService => OldObsService,
  StorageNodeService => OldNodeService
}
import utils.testhelpers.{EventGenerators_Old, NodeGenerators}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EventMigratorSpec
    extends MusitSpecWithAppPerSuite
    with NodeGenerators
    with EventGenerators_Old
    with MusitResultValues
    with BeforeAndAfterAll {

  val oldEventDao   = fromInstanceCache[EventDao]
  val ctrlDao       = fromInstanceCache[ControlDao]
  val obsDao        = fromInstanceCache[ObservationDao]
  val envReqDao     = fromInstanceCache[EnvReqDao]
  val mvDao         = fromInstanceCache[MoveDao]
  val ctrlService   = fromInstanceCache[OldCtrlService]
  val obsService    = fromInstanceCache[OldObsService]
  val nodeService   = fromInstanceCache[OldNodeService]
  val envReqService = fromInstanceCache[OldEnvReqService]
  val migrationDao  = fromInstanceCache[MigrationDao]
  val oldLocDao     = fromInstanceCache[OldLocObjDao]

  // In the test configs, we do not enable the DataMigrationModule. Hence
  // we need to initialise it manually using the constructor.
  val migrator = new EventMigrator(
    oldEventDao,
    migrationDao,
    oldLocDao,
    ctrlDao,
    obsDao,
    envReqDao,
    mvDao
  )

  var numOldEvents: Int = 0

  // scalastyle:off method.length line.size.limit
  override def beforeAll() = {
    val maybeNodeId1 = Option(StorageNodeDatabaseId(17L))
    val maybeNodeId2 = Option(StorageNodeDatabaseId(10L))
    val maybeNodeId3 = Option(StorageNodeDatabaseId(9L))

    val movableNode =
      nodeService
        .getNodeById(defaultMuseumId, maybeNodeId1.get)
        .futureValue
        .successValue
        .value

    // format: off
    val ctrls = (1 until 50).map(_ => createControl(maybeNodeId1))
    val obs = (1 until 50).map(_ => createObservation(maybeNodeId1))
    val envRes = (1 to 50).map(i => createEnvRequirement(maybeNodeId2, Some(s"Note $i")))
    val mnds =
      (
        (11 to 16).map(_ => createMoveNode(maybeNodeId1, movableNode.isPartOf, maybeNodeId2.get)),
        (11 to 16).map(_ => createMoveNode(maybeNodeId1, maybeNodeId2, maybeNodeId3.get))
      )
    val mods =
      (1 to 25).map(i => createMoveObject(Option(ObjectId(i.toLong)), None, maybeNodeId1.get)) ++
        (1 to 25).map(i => createMoveObject(Option(ObjectId(i.toLong)), maybeNodeId1, maybeNodeId2.get))
    // format: on

    (for {
      cs <- Future.sequence(
             ctrls.map(c => ctrlService.add(defaultMuseumId, c.affectedThing.get, c))
           )
      os <- Future.sequence(
             obs.map(o => obsService.add(defaultMuseumId, o.affectedThing.get, o))
           )
      er <- Future.sequence(
             envRes.map(e => envReqService.add(defaultMuseumId, e))
           )
      m1 <- Future.sequence((mnds._1 ++ mods).map {
             case mn: OldMoveNode =>
               nodeService.moveNodes(defaultMuseumId, mn.to, Seq(mn))
             case mo: OldMoveObject =>
               nodeService.moveObjects(defaultMuseumId, mo.to, Seq(mo))
           })
      m2 <- Future.sequence(mnds._2.map {
             case mn: OldMoveNode =>
               nodeService.moveNodes(defaultMuseumId, mn.to, Seq(mn))
             case _ => throw new TestFailedException("Not possible in this case", 0)
           })
    } yield {
      val attemptedWrites = cs.size + os.size + er.size + m1.size + m2.size
      val successfulWrites =
        cs.count(_.isSuccess) +
          os.count(_.isSuccess) +
          er.count(_.isSuccess) +
          m1.count(_.isSuccess) +
          m2.count(_.isSuccess)
      val failedWrites = attemptedWrites - successfulWrites

      if (failedWrites > 0) {
        fail(s"There were $failedWrites failures when bootstrapping old events.")
      }

      numOldEvents = successfulWrites
    }).futureValue

    super.beforeAll()
  }

  // scalastyle:on method.length line.size.limit

  "The EventMigrator" should {

    s"migrate all old events and local objects to new versions" in {
      migrator.migrateAll().futureValue mustBe numOldEvents

      val res = migrator.verify().futureValue
      res mustBe MigrationVerification(49, 49, 50, 12, 50)
      res.total mustBe 210
    }

  }

}
