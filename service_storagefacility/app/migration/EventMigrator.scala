package migration

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.Source
import com.google.inject.{Inject, Singleton}
import models.storage.event.EventTypeRegistry.TopLevelEvents
import models.storage.event.EventTypeRegistry.TopLevelEvents._
import models.storage.event.MusitEvent
import models.storage.event.control.Control
import models.storage.event.dto.BaseEventDto
import models.storage.event.envreq.EnvRequirement
import models.storage.event.move._
import models.storage.event.observation.Observation
import no.uio.musit.MusitResults.MusitSuccess
import no.uio.musit.models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.storage.dao.MigrationDao
import repositories.storage.dao.events.{ControlDao, EnvReqDao, MoveDao, ObservationDao}
import repositories.storage.old_dao.{LocalObjectDao => OldLocObjDao}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal

// TODO: This file can be removed when Migration has been performed.

/**
 * This class deals with migration of events and "cached" "local objects"
 * for the StorageFacility service. The {{{migrateAll()}}} method reads _all_
 * events of type {{{TopLevelEvent}}} from the old database tables, using the
 * "old" DAO implementations. Then the events (and local objects) are mapped to
 * their new representation before being written to the new database tables.
 */
// scalastyle:off
@Singleton
final class EventMigrator @Inject()(
    val migrationDao: MigrationDao,
    oldLocObjDao: OldLocObjDao,
    ctlDao: ControlDao,
    obsDao: ObservationDao,
    envDao: EnvReqDao,
    movDao: MoveDao,
    materialiser: Materializer,
    actorSystem: ActorSystem
) extends ControlMappers
    with ObservationMappers
    with EnvReqMappers
    with MoveNodeMappers
    with MoveObjectMappers {

  implicit val sys = actorSystem
  implicit val mat = materialiser

  // Blocking operation to fetch a map with node DB id, uuid and museumId for
  // all nodes in the database.
  override val nodeIdMap = Await.result(migrationDao.getAllNodeIds, 5 minutes)

  private def lastWritten = Await.result(startingPoint, 1 minute)

  logger.debug(s"Loaded ${nodeIdMap.size} nodes to internal lookup table...")

  // Call the migrateAll function to trigger the migration code
  migrateAll()

  private case class MigrationResult(
      success: Int = 0,
      errors: Seq[EventId] = Seq.empty
  )

  private def startingPoint: Future[Option[EventId]] = {
    migrationDao.lastMigratedEvent
  }

  private def migrated(ids: Seq[EventId]) = migrationDao.addMigratedEvents(ids)

  private def transform(
      events: Seq[(BaseEventDto, Option[ObjectUUID], Option[MuseumId])]
  ): Future[Seq[(EventId, (MuseumId, MusitEvent))]] = Future.sequence {
    events.map {
      case (base, moid, mmid) =>
        val newEvent = TopLevelEvents.unsafeFromId(base.eventTypeId) match {
          case MoveObjectType          => convertMoveObject(base, moid, mmid)
          case MoveNodeType            => convertMoveNode(base)
          case EnvRequirementEventType => convertEnvReq(base)
          case ObservationEventType    => convertObs(base)
          case ControlEventType        => convertCtrl(base)
        }
        newEvent.map(ne => (base.id.get, ne))
    }
  }

  private def isMoveObject(e: MusitEvent): Boolean = {
    e.eventType.registeredEventId == MoveObjectType.id
  }

  private def batchSaveMoves(events: Seq[(EventId, (MuseumId, MusitEvent))]) = {
    if (events.isEmpty) Future.successful(MigrationResult())
    else {
      val oldIds = events.map(_._1)
      val mobs   = events.map(e => (e._2._1, e._2._2.asInstanceOf[MoveObject]))
      movDao.batchInsertMigratedObjects(mobs).map {
        case MusitSuccess(ids) => MigrationResult(ids.size, Seq.empty)
        case err               => MigrationResult(0, oldIds)
      }
    }
  }

  private def convertAndInsert(
      events: Seq[(BaseEventDto, Option[ObjectUUID], Option[MuseumId])]
  ): Future[MigrationResult] = {
    transform(events).flatMap { evts =>
      // isolate the MoveObject events so they can be inserted as one big batch.
      val moveObjEvents = evts.filter(e => isMoveObject(e._2._2))
      val theRestEvents = evts.filterNot(e => isMoveObject(e._2._2))
      for {
        mo <- batchSaveMoves(moveObjEvents).map { mr =>
               logger.info(s"Wrote ${mr.success} of ${moveObjEvents.size} events")
               moveObjEvents.map(_._1) -> mr
             }
        tr <- if (theRestEvents.isEmpty)
               Future.successful(Seq.empty -> MigrationResult())
             else
               Future.sequence {
                 theRestEvents.map {
                   case (oldId: EventId, (mid: MuseumId, e: MoveEvent)) =>
                     movDao.insert(mid, e).map((oldId, _))

                   case (oldId: EventId, (mid: MuseumId, e: EnvRequirement)) =>
                     envDao.insert(mid, e).map((oldId, _))

                   case (oldId: EventId, (mid: MuseumId, e: Observation)) =>
                     obsDao.insert(mid, e).map((oldId, _))

                   case (oldId: EventId, (mid: MuseumId, e: Control)) =>
                     ctlDao.insert(mid, e).map((oldId, _))
                 }
               }.map { results =>
                 val r = results.foldLeft((List.empty[EventId], MigrationResult())) {
                   case (acc, curr) =>
                     curr match {
                       case (oldId, MusitSuccess(eid)) =>
                         val oldIds = acc._1 ::: oldId :: Nil
                         oldIds -> acc._2.copy(success = acc._2.success + 1)

                       case (oldId, error) =>
                         logger.warn(s"Something went wrong migrating $oldId")
                         acc._1 -> acc._2.copy(errors = acc._2.errors :+ oldId)
                     }
                 }

                 logger.info(s"Wrote ${r._2.success} of ${theRestEvents.size} events")
                 r
               }
        _ <- migrated(mo._1 ++ tr._1)
      } yield {
        val s = mo._2.success + tr._2.success
        val e = mo._2.errors ++ tr._2.errors
        MigrationResult(s, e)
      }
    }
  }

  def migrateAll(): Future[Int] = {
    val startTime: FiniteDuration = System.currentTimeMillis() milliseconds

    logger.warn("Starting data migration of old events")

    val stream = lastWritten
      .map(l => migrationDao.streamAllEventsFrom(l))
      .getOrElse(migrationDao.streamAllEvents)

    Source
      .fromPublisher(stream)
      .via(new EventConcat)
      .map { rows =>
        if (rows.size > 1) {
          rows.reduceLeft { (acc, curr) =>
            logger.debug(s"merging acc (${acc._1.id}) with curr (${curr._1.id})")
            (migrationDao.enrichRelations(acc._1, curr), acc._2, acc._3)
          }
        } else {
          rows.head
        }
      }
      .grouped(1000)
      .runFoldAsync(MigrationResult()) { (acc, curr) =>
        convertAndInsert(curr).map { res =>
          acc.copy(
            success = acc.success + res.success,
            errors = acc.errors ++ res.errors
          )
        }
      }
      .map { res =>
        val endTime   = System.currentTimeMillis() milliseconds
        val totalTime = endTime - startTime
        logger.info(
          s"Migrated ${res.success} of ${res.success + res.errors.size} events " +
            s"completed in ${totalTime.toMinutes} minutes"
        )

        if (res.errors.nonEmpty) {
          logger.error(
            s"The following old eventIds were not migrated:\n" +
              s"${res.errors.mkString("[", ", ", "]")}"
          )
        }
        res.success
      }
      .recover {
        case NonFatal(ex) =>
          logger.error("Bad things during data migration", ex)
          throw ex
      }
  }

  def verify(): Future[MigrationVerification] = migrationDao.countAll()

}

case class MigrationVerification(
    numControl: Int = 0,
    numObservation: Int = 0,
    numEnvRequirement: Int = 0,
    numMoveNode: Int = 0,
    numMoveObject: Int = 0
) {

  val total = numControl + numObservation + numEnvRequirement + numMoveNode + numMoveObject

  override def toString = {
    s"""
       |MigrationVerification(
       |  control events: $numControl
       |  observation events: $numObservation
       |  env requirement events: $numEnvRequirement
       |  move node events: $numMoveNode
       |  move object events: $numMoveObject
       |  -------------------------------
       |  total events: $total
       |  ===============================
       |)
       |""".stripMargin
  }

}

// scalastyle:on
