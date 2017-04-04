package migration

import com.google.inject.{Inject, Singleton}
import models.storage.event.EventTypeRegistry.TopLevelEvents._
import models.storage.event.MusitEvent
import models.storage.event.control.Control
import models.storage.event.control.ControlAttributes._
import models.storage.event.dto.{BaseEventDto, DtoConverters, ExtendedDto}
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
import no.uio.musit.models.ObjectTypes.CollectionObject
import no.uio.musit.models._
import play.api.Logger
import repositories.storage.dao.MigrationDao
import repositories.storage.dao.events.{ControlDao, EnvReqDao, MoveDao, ObservationDao}
import repositories.storage.old_dao.event.EventDao
import repositories.storage.old_dao.{LocalObjectDao => OldLocObjDao}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.util.control.NonFatal

private[migration] trait TypeMappers {

  type ConvertRes = Seq[(EventId, (MusitEvent, MuseumId))]

  val dummyMuseumId = MuseumId(666)
  val oldEventDao: EventDao
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

  def convertObs: Future[ConvertRes] =
    oldEventDao
      .getAllEvents(dummyMuseumId, ObservationEventType) { dto =>
        DtoConverters.ObsConverters.observationFromDto(dto.asInstanceOf[BaseEventDto])
      }
      .map(old => mapOldToNew(old))

  private def mapOldToNew(observations: Seq[OldObservation]): ConvertRes = {
    observations.map { old =>
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
      (old.id.get, (obs, idMid._2))
    }
  }
}

private[migration] trait ControlMappers extends TypeMappers with SubObservationMappers {

  def convertCtrl: Future[ConvertRes] =
    oldEventDao
      .getAllEvents(dummyMuseumId, ControlEventType) { dto =>
        DtoConverters.CtrlConverters.controlFromDto(dto.asInstanceOf[BaseEventDto])
      }
      .map(old => mapOldToNew(old))

  private def mapOldToNew(ctrls: Seq[OldControl]): ConvertRes = {
    ctrls.map { old =>
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
      (old.id.get, (ctrl, idMid._2))
    }
  }
}

private[migration] trait EnvReqMappers extends TypeMappers {

  def convertEnvReq: Future[ConvertRes] =
    oldEventDao
      .getAllEvents(dummyMuseumId, EnvRequirementEventType) { dto =>
        DtoConverters.EnvReqConverters.envReqFromDto(dto.asInstanceOf[ExtendedDto])
      }
      .map(old => mapOldToNew(old))

  private def mapOldToNew(envs: Seq[OldEnvReq]): ConvertRes = {
    envs.map { old =>
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

      (old.id.get, (er, idMid._2))
    }
  }
}

private[migration] trait MoveNodeMappers extends TypeMappers {

  val migrationDao: MigrationDao

  def convertMoveNode: Future[ConvertRes] =
    oldEventDao
      .getAllEvents(dummyMuseumId, MoveNodeType) { dto =>
        DtoConverters.MoveConverters.moveNodeFromDto(dto.asInstanceOf[BaseEventDto])
      }
      .map(old => mapOldToNew(old))

  private def mapOldToNew(moves: Seq[OldMoveNode]): ConvertRes = {
    moves.map { old =>
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

      (old.id.get, (mn, idMid._2))
    }
  }

}

private[migration] trait MoveObjectMappers extends TypeMappers {

  val migrationDao: MigrationDao

  def convertMoveObject: Future[ConvertRes] =
    for {
      old <- oldEventDao.getAllEvents(dummyMuseumId, MoveObjectType) { dto =>
              DtoConverters.MoveConverters.moveObjectFromDto(
                dto.asInstanceOf[BaseEventDto]
              )
            }
      affectedNodeIds <- Future.successful(old.flatMap(_.affectedThing))
      idMap           <- migrationDao.getObjectUUIDsForObjectIds(affectedNodeIds)
    } yield {
      mapOldToNew(old, idMap)
    }

  private def mapOldToNew(
      moves: Seq[OldMoveObject],
      idMap: Map[ObjectId, (ObjectUUID, MuseumId)]
  ): ConvertRes = {
    moves.map { old =>
      val idMid = idMap(old.affectedThing.get)
      val mn = MoveObject(
        id = None,
        doneBy = old.doneBy,
        doneDate = old.doneDate,
        affectedThing = Option(idMid._1),
        registeredBy = old.registeredBy,
        registeredDate = old.registeredDate,
        eventType = old.eventType,
        objectType = CollectionObject,
        from = old.from.map(o => nodeIdMap(o)._1),
        to = nodeIdMap(old.to)._1
      )

      (old.id.get, (mn, idMid._2))
    }
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
    val oldEventDao: EventDao,
    val migrationDao: MigrationDao,
    oldLocObjDao: OldLocObjDao,
    ctlDao: ControlDao,
    obsDao: ObservationDao,
    envDao: EnvReqDao,
    movDao: MoveDao
) extends ControlMappers
    with ObservationMappers
    with EnvReqMappers
    with MoveNodeMappers
    with MoveObjectMappers {

  val logger = Logger(classOf[EventMigrator])

  // Blocking operation to fetch the nodeId Map
  override val nodeIdMap =
    Await.result(migrationDao.getAllNodeIds, 5 minutes)

  logger.debug(s"Loaded ${nodeIdMap.size} nodes to internal lookup table...")

  /*
    TODO: Before running with this current implementation, count the number of
     existing events in the event table on e.g. the musit-test environment.
      If the amount of data is too big, we might get memory issues doing the
      migration
   */
  def migrateAll(): Future[Int] = {
    Future
      .sequence(
        Seq(convertCtrl, convertObs, convertEnvReq, convertMoveNode, convertMoveObject)
      )
      .map(_.flatten)
      .flatMap { newEvents =>
        logger.info(s"Going to insert ${newEvents.size} events into the new event table")
        Future.sequence {
          newEvents.sortBy(_._1.underlying).map { om =>
            val mid = om._2._2

            om._2._1 match {
              case c: Control        => ctlDao.insert(mid, c)
              case o: Observation    => obsDao.insert(mid, o)
              case e: EnvRequirement => envDao.insert(mid, e)
              case mn: MoveNode      => movDao.insert(mid, mn)
              case mo: MoveObject    => movDao.insert(mid, mo)
            }
          }
        }.map(
          _.foldLeft(0)((success, mres) => mres.map(_ => success + 1).getOrElse(success))
        )
      }
      .map { numSuccess =>
        logger.info(s"Successfully wrote $numSuccess events to the new event table")
        numSuccess
      }
      .recover {
        case NonFatal(ex) =>
          logger.error("This should not happen", ex)
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

}

// scalastyle:on
