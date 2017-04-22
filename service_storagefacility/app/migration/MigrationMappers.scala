package migration

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
import no.uio.musit.models.ObjectTypes.CollectionObject
import no.uio.musit.models._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.storage.dao.MigrationDao
import repositories.storage.old_dao.{LocalObjectDao => OldLocObjDao}

import scala.concurrent.Future

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
