package utils.testhelpers

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import models.storage.event.dto.{DtoConverters, EventRolePlace}
import models.storage.event.old.move.{
  MoveNode => OldMoveNode,
  MoveObject => OldMoveObject
}
import no.uio.musit.MusitResults.{MusitDbError, MusitSuccess, MusitValidationError}
import no.uio.musit.models.{ObjectId, StorageNodeDatabaseId}
import no.uio.musit.test.MusitSpecWithApp
import no.uio.musit.test.matchers.MusitResultValues
import repositories.storage.old_dao.event.EventDao
import repositories.storage.old_dao.{LocalObjectDao => OldLocObjDao}
import services.old.{
  ControlService => OldCtrlService,
  EnvironmentRequirementService => OldEnvReqService,
  ObservationService => OldObsService,
  StorageNodeService => OldNodeService
}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

// TODO: This can be removed when Migration has been performed.

trait MigrationTest extends MusitResultValues {
  self: MusitSpecWithApp with NodeGenerators with EventGenerators_Old =>

  implicit val as: ActorSystem   = ActorSystem()
  implicit val mat: Materializer = ActorMaterializer()

  val oldEventDao: EventDao           = fromInstanceCache[EventDao]
  val ctrlService: OldCtrlService     = fromInstanceCache[OldCtrlService]
  val obsService: OldObsService       = fromInstanceCache[OldObsService]
  val nodeService: OldNodeService     = fromInstanceCache[OldNodeService]
  val envReqService: OldEnvReqService = fromInstanceCache[OldEnvReqService]
  val oldLocDao: OldLocObjDao         = fromInstanceCache[OldLocObjDao]

  // scalastyle:off method.length line.size.limit
  def bootstrap(): Future[Int] = {
    val maybeNodeId0 = Option(StorageNodeDatabaseId(4))
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
    val ctrls = (1 to 50).map(_ => createControl(maybeNodeId1))
    val obs = (1 to 50).map(_ => createObservation(maybeNodeId1))
    val envRes = (1 to 50).map(i => createEnvRequirement(maybeNodeId2, Some(s"Note $i")))
    val mnds =
      (
        (11 to 15).map(i => createMoveNode(Some(StorageNodeDatabaseId(i.toLong)), maybeNodeId0, maybeNodeId2.get)),
        (11 to 15).map(i => createMoveNode(Some(StorageNodeDatabaseId(i.toLong)), maybeNodeId2, maybeNodeId3.get))
      )
    val mods =
      (1 to 50).map(i => createMoveObject(Option(ObjectId(i.toLong)), None, maybeNodeId1.get)) ++
        (1 to 50).map(i => createMoveObject(Option(ObjectId(i.toLong)), maybeNodeId1, maybeNodeId2.get))
    // format: on

    for {
      cs <- Future.sequence(
             ctrls.map(c => ctrlService.add(defaultMuseumId, c.affectedThing.get, c))
           )
      os <- Future.sequence(
             obs.map(o => obsService.add(defaultMuseumId, o.affectedThing.get, o))
           )
      er <- Future.sequence(
             envRes.map(e => envReqService.add(defaultMuseumId, e))
           )
      m1 <- Future.sequence(mnds._1.map {
             case mn: OldMoveNode =>
               nodeService.moveNodes(defaultMuseumId, mn.to, Seq(mn))
             case _ =>
               Future.successful(MusitValidationError("Not possible in this case"))
           })
      m2 <- Future.sequence(mods.map {
             case mo: OldMoveObject =>
               nodeService.moveObjects(defaultMuseumId, mo.to, Seq(mo))
             case _ =>
               Future.successful(MusitValidationError("Not possible in this case"))
           })
      m3 <- Future.sequence(mnds._2.map {
             case mn: OldMoveNode =>
               nodeService.moveNodes(defaultMuseumId, mn.to, Seq(mn))
             case _ =>
               Future.successful(MusitValidationError("Not possible in this case"))
           })

      m4 <- {
        val oid = ObjectId(10L)
        val mo  = createMoveObject(Some(oid), maybeNodeId2, maybeNodeId3.get)
        val dto = DtoConverters.MoveConverters.moveObjectToDto(mo)
        val fromPlace = Seq(
          EventRolePlace(
            eventId = None,
            roleId = 4,
            placeId = maybeNodeId2.get,
            eventTypeId = dto.eventTypeId
          )
        )
        val cp = dto.copy(
          valueLong = None,
          relatedPlaces = dto.relatedPlaces ++ fromPlace
        )
        oldEventDao.insertEvent(defaultMuseumId, cp).map(MusitSuccess.apply).recover {
          case NonFatal(ex) => MusitDbError(ex.getMessage, Option(ex))
        }
      }
    } yield {
      val attempted = cs.size + os.size + er.size + m1.size + m2.size + m3.size + 1

      val successfulCtrls = cs.count(_.isSuccess)
      val successfulObs   = os.count(_.isSuccess)
      val successfulEnvs  = er.count(_.isSuccess)
      val successfulMvn   = (m1 ++ m3).count(_.isSuccess)
      val successfulMvo   = m2.count(_.isSuccess)
      val successfulBadMv = if (m4.isSuccess) 1 else 0

      val successful = successfulCtrls + successfulObs + successfulEnvs + successfulMvn + successfulMvo + successfulBadMv

      val failed = attempted - successful

      // scalastyle:off
      println(
        s"""
           |There were $successful successful insertions and $failed failures
           |when bootstrapping $attempted old events.
           |
           |controls        = $successfulCtrls
           |observations    = $successfulObs
           |env requirement = $successfulEnvs
           |move node       = $successfulMvn
           |move object     = ${successfulMvo + successfulBadMv}
           |""".stripMargin
      )
      // scalastyle:on

      successful
    }
  }

}
