package migration

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.Source
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import com.google.inject.{Inject, Singleton}
import models.storage.event.EventTypeRegistry.TopLevelEvents
import models.storage.event.EventTypeRegistry.TopLevelEvents._
import models.storage.event.MusitEvent
import models.storage.event.control.Control
import models.storage.event.control.ControlAttributes._
import models.storage.event.dto.BaseEventDto
import models.storage.event.dto.DtoConverters._
import models.storage.event.envreq.EnvRequirement
import models.storage.event.move._
import models.storage.event.observation.Observation
import models.storage.event.observation.ObservationAttributes._
import models.storage.event.old.control.{Control => OldControl}
import models.storage.event.old.envreq.{EnvRequirement => OldEnvReq}
import models.storage.event.old.move.{
  MoveNode => OldMoveNode,
  MoveObject => OldMoveObject
}
import models.storage.event.old.observation.ObservationSubEvents.{
  ObservationAlcohol => OldObsAlcohol,
  ObservationCleaning => OldObsCleaning,
  ObservationFireProtection => OldObsFire,
  ObservationGas => OldObsGas,
  ObservationHypoxicAir => OldObsHypoxic,
  ObservationLightingCondition => OldObsLight,
  ObservationMold => OldObsMold,
  ObservationPerimeterSecurity => OldObsSec,
  ObservationPest => OldObsPest,
  ObservationRelativeHumidity => OldObsHum,
  ObservationTemperature => OldObsTemp,
  ObservationTheftProtection => OldObsTheft,
  ObservationWaterDamageAssessment => OldObsWater
}
import models.storage.event.old.observation.{Observation => OldObservation}
import no.uio.musit.MusitResults.MusitSuccess
import no.uio.musit.models.ObjectTypes.CollectionObject
import no.uio.musit.models._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.storage.dao.MigrationDao
import repositories.storage.dao.MigrationDao.AllEventsRow
import repositories.storage.dao.events.{ControlDao, EnvReqDao, MoveDao, ObservationDao}
import repositories.storage.old_dao.{LocalObjectDao => OldLocObjDao}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal

// TODO: This file can be removed when Migration has been performed.

private[migration] trait TypeMappers {

  val logger = Logger("migration.EventMigrator")

  type ConvertRes = (MuseumId, MusitEvent)

  val dummyMuseumId = MuseumId(666)

  val migrationDao: MigrationDao
  val nodeIdMap: Map[StorageNodeDatabaseId, (StorageNodeId, MuseumId)]
}

private[migration] trait SubObservationMappers {
  def convertOldObsAlcohol(mo: Option[OldObsAlcohol]): Option[ObservationAlcohol] = {
    mo.map(o => ObservationAlcohol(o.note, o.condition, o.volume))
  }

  def convertOldObsCleaning(mo: Option[OldObsCleaning]): Option[ObservationCleaning] = {
    mo.map(o => ObservationCleaning(o.note, o.cleaning))
  }

  def convertOldObsGas(mo: Option[OldObsGas]): Option[ObservationGas] = {
    mo.map(o => ObservationGas(o.note, o.gas))
  }

  def convertOldObsHypoxic(mo: Option[OldObsHypoxic]): Option[ObservationHypoxicAir] = {
    mo.map(o => ObservationHypoxicAir(o.note, o.range))
  }

  def convertOldObsLight(mo: Option[OldObsLight]): Option[ObservationLightingCondition] = { // scalastyle:ignore
    mo.map(o => ObservationLightingCondition(o.note, o.lightingCondition))
  }

  def convertOldObsMold(mo: Option[OldObsMold]): Option[ObservationMold] = {
    mo.map(o => ObservationMold(o.note, o.mold))
  }

  def convertOldObsPest(mo: Option[OldObsPest]): Option[ObservationPest] = {
    mo.map(o => ObservationPest(o.note, o.identification, o.lifecycles))
  }

  def convertOldObsRelHum(mo: Option[OldObsHum]): Option[ObservationRelativeHumidity] = {
    mo.map(o => ObservationRelativeHumidity(o.note, o.range))
  }

  def convertOldObsTemp(mo: Option[OldObsTemp]): Option[ObservationTemperature] = {
    mo.map(o => ObservationTemperature(o.note, o.range))
  }

  def convertOldObsTheft(mo: Option[OldObsTheft]): Option[ObservationTheftProtection] = {
    mo.map(o => ObservationTheftProtection(o.note, o.theftProtection))
  }

  def convertOldObsFire(mo: Option[OldObsFire]): Option[ObservationFireProtection] = {
    mo.map(o => ObservationFireProtection(o.note, o.fireProtection))
  }

  def convertOldObsSecurity(
      mo: Option[OldObsSec]
  ): Option[ObservationPerimeterSecurity] = {
    mo.map(o => ObservationPerimeterSecurity(o.note, o.perimeterSecurity))
  }

  def convertOldObsWater(
      mo: Option[OldObsWater]
  ): Option[ObservationWaterDamageAssessment] = {
    mo.map(o => ObservationWaterDamageAssessment(o.note, o.waterDamageAssessment))
  }
}

private[migration] trait ObservationMappers
    extends TypeMappers
    with SubObservationMappers {

  def convertObs(dto: BaseEventDto): Future[(MuseumId, Observation)] =
    migrationDao.enrichTopLevelEvent(dto).flatMap { base =>
      migrationDao.getObsDetails(base).map { old =>
        mapOldToNew(ObsConverters.observationFromDto(old))
      }
    }

  private def mapOldToNew(old: OldObservation): (MuseumId, Observation) = {
    logger.debug(s"Mapping old Observation events to new format")
    val idMid = nodeIdMap(old.affectedThing.get)
    val obs = Observation(
      id = None,
      doneBy = old.doneBy,
      doneDate = old.doneDate,
      affectedThing = Option(idMid._1),
      registeredBy = old.registeredBy,
      registeredDate = old.registeredDate,
      eventType = old.eventType,
      alcohol = convertOldObsAlcohol(old.alcohol),
      cleaning = convertOldObsCleaning(old.cleaning),
      gas = convertOldObsGas(old.gas),
      hypoxicAir = convertOldObsHypoxic(old.hypoxicAir),
      lightingCondition = convertOldObsLight(old.lightingCondition),
      mold = convertOldObsMold(old.mold),
      pest = convertOldObsPest(old.pest),
      relativeHumidity = convertOldObsRelHum(old.relativeHumidity),
      temperature = convertOldObsTemp(old.temperature),
      theftProtection = convertOldObsTheft(old.theftProtection),
      fireProtection = convertOldObsFire(old.fireProtection),
      perimeterSecurity = convertOldObsSecurity(old.perimeterSecurity),
      waterDamageAssessment = convertOldObsWater(old.waterDamageAssessment)
    )
    (idMid._2, obs)
  }
}

private[migration] trait ControlMappers extends TypeMappers with SubObservationMappers {

  def convertCtrl(dto: BaseEventDto): Future[(MuseumId, Control)] =
    migrationDao.enrichTopLevelEvent(dto).flatMap { base =>
      migrationDao.getCtrlDetails(base).map { old =>
        mapOldToNew(CtrlConverters.controlFromDto(old))
      }
    }

  private def mapOldToNew(old: OldControl): (MuseumId, Control) = {
    logger.debug(s"Mapping old Control events to new format")
    val idMid = nodeIdMap(old.affectedThing.get)
    val ctrl = Control(
      id = None,
      doneBy = old.doneBy,
      doneDate = old.doneDate,
      affectedThing = Option(idMid._1),
      registeredBy = old.registeredBy,
      registeredDate = old.registeredDate,
      eventType = old.eventType,
      alcohol = old.alcohol.map { c =>
        ControlAlcohol(c.ok, convertOldObsAlcohol(c.observation))
      },
      cleaning = old.cleaning.map { c =>
        ControlCleaning(c.ok, convertOldObsCleaning(c.observation))
      },
      gas = old.gas.map { c =>
        ControlGas(c.ok, convertOldObsGas(c.observation))
      },
      hypoxicAir = old.hypoxicAir.map { c =>
        ControlHypoxicAir(c.ok, convertOldObsHypoxic(c.observation))
      },
      lightingCondition = old.lightingCondition.map { c =>
        ControlLightingCondition(c.ok, convertOldObsLight(c.observation))
      },
      mold = old.mold.map { c =>
        ControlMold(c.ok, convertOldObsMold(c.observation))
      },
      pest = old.pest.map { c =>
        ControlPest(c.ok, convertOldObsPest(c.observation))
      },
      relativeHumidity = old.relativeHumidity.map { c =>
        ControlRelativeHumidity(c.ok, convertOldObsRelHum(c.observation))
      },
      temperature = old.temperature.map { c =>
        ControlTemperature(c.ok, convertOldObsTemp(c.observation))
      }
    )
    (idMid._2, ctrl)
  }
}

private[migration] trait EnvReqMappers extends TypeMappers {

  def convertEnvReq(dto: BaseEventDto): Future[(MuseumId, EnvRequirement)] = {
    migrationDao.enrichTopLevelEvent(dto).flatMap { base =>
      migrationDao.getEnvReqDetails(base).map { ex =>
        val old = EnvReqConverters.envReqFromDto(ex)
        mapOldToNew(old)
      }
    }
  }

  private def mapOldToNew(old: OldEnvReq): (MuseumId, EnvRequirement) = {
    logger.debug(s"Mapping old EnvRequirement events to new format")
    val idMid = nodeIdMap(old.affectedThing.get)
    val er = EnvRequirement(
      id = None,
      doneBy = old.doneBy,
      doneDate = old.doneDate,
      affectedThing = Option(idMid._1),
      registeredBy = old.registeredBy,
      registeredDate = old.registeredDate,
      note = old.note,
      eventType = old.eventType,
      temperature = old.temperature,
      airHumidity = old.airHumidity,
      hypoxicAir = old.hypoxicAir,
      cleaning = old.cleaning,
      light = old.light
    )
    (idMid._2, er)
  }
}

private[migration] trait MoveNodeMappers extends TypeMappers {

  def convertMoveNode(dto: BaseEventDto): Future[(MuseumId, MoveNode)] =
    Future.successful {
      val old = MoveConverters.moveNodeFromDto(dto)
      mapOldToNew(old)
    }

  private def mapOldToNew(old: OldMoveNode): (MuseumId, MoveNode) = {
    logger.debug(s"Mapping old MoveNode events to new format")
    val idMid = nodeIdMap(old.affectedThing.get)
    val mn = MoveNode(
      id = None,
      doneBy = old.doneBy,
      doneDate = old.doneDate,
      affectedThing = Option(idMid._1),
      registeredBy = old.registeredBy,
      registeredDate = old.registeredDate,
      eventType = old.eventType,
      from = old.from.map(o => nodeIdMap(o)._1),
      to = nodeIdMap(old.to)._1
    )
    (idMid._2, mn)
  }
}

private[migration] trait MoveObjectMappers extends TypeMappers {

  val migrationDao: MigrationDao

  def convertMoveObject(
      dto: BaseEventDto,
      moid: Option[ObjectUUID],
      mmid: Option[MuseumId]
  ): Future[(MuseumId, MoveObject)] = Future.successful {
    val old = MoveConverters.moveObjectFromDto(dto)
    mapOldToNew(old, moid, mmid)
  }

  private def mapOldToNew(
      old: OldMoveObject,
      uuid: Option[ObjectUUID],
      mid: Option[MuseumId]
  ): (MuseumId, MoveObject) = {
    logger.debug(s"Mapping old MoveObject events to new format")
    val mo = MoveObject(
      id = None,
      doneBy = old.doneBy,
      doneDate = old.doneDate,
      affectedThing = uuid,
      registeredBy = old.registeredBy,
      registeredDate = old.registeredDate,
      eventType = old.eventType,
      objectType = CollectionObject,
      from = old.from.map(o => nodeIdMap(o)._1),
      to = nodeIdMap(old.to)._1
    )
    (mid.get, mo)
  }
}

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

  logger.debug(s"Loaded ${nodeIdMap.size} nodes to internal lookup table...")

  // Call the migrateAll function to trigger the migration code
  migrateAll()

  private case class MigrationResult(
      success: Int = 0,
      errors: Seq[EventId] = Seq.empty
  )

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
               mr
             }
        tr <- if (theRestEvents.isEmpty) Future.successful(MigrationResult())
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
                 val r = results.foldLeft(MigrationResult()) { (acc, curr) =>
                   curr match {
                     case (_, MusitSuccess(eid)) => acc.copy(success = acc.success + 1)
                     case (oldId, error) =>
                       logger.warn(s"Something went wrong migrating $oldId")
                       acc.copy(errors = acc.errors :+ oldId)
                   }
                 }
                 logger.info(s"Wrote ${r.success} of ${theRestEvents.size} events")
                 r
               }
      } yield {
        val s = mo.success + tr.success
        val e = mo.errors ++ tr.errors
        MigrationResult(s, e)
      }
    }
  }

  def migrateAll(): Future[Int] = {
    val startTime: FiniteDuration = System.currentTimeMillis() milliseconds

    logger.warn("Starting data migration of old events")

    val stream = migrationDao.streamAllEvents

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

  final class EventConcat
      extends GraphStage[FlowShape[AllEventsRow, Seq[AllEventsRow]]] {

    val in  = Inlet[AllEventsRow]("AllEventsConcat.in")
    val out = Outlet[Seq[AllEventsRow]]("AllEventsConcat.out")

    override def shape = FlowShape.of(in, out)

    override def createLogic(attributes: Attributes) = new GraphStageLogic(shape) {

      // format: off
      private var currentState: Option[AllEventsRow] = None
      private val buffer = Vector.newBuilder[AllEventsRow]
      // format: on

      setHandlers(
        in = in,
        out = out,
        handler = new InHandler with OutHandler {
          override def onPush(): Unit = {
            val nextElement = grab(in)
            val nextId      = nextElement._1.id

            if (currentState.isEmpty || currentState.exists(_._1.id == nextId)) {
              buffer += nextElement
              pull(in)
            } else {
              val result = buffer.result()
              buffer.clear()
              buffer += nextElement
              push(out, result)
            }
            currentState = Some(nextElement)
          }

          override def onPull(): Unit = pull(in)

          override def onUpstreamFinish(): Unit = {
            val result = buffer.result()
            if (result.nonEmpty) emit(out, result)
            completeStage()
          }
        }
      )

      override def postStop(): Unit = buffer.clear()
    }
  }

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
    s"""MigrationVerification(
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
