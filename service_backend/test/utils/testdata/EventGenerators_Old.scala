package utils.testdata

import models.storage.event.EventType
import models.storage.event.EventTypeRegistry.TopLevelEvents._
import models.storage.event.dto.DtoConverters
import models.storage.event.old.control.Control
import models.storage.event.old.control.ControlSubEvents._
import models.storage.event.old.envreq.EnvRequirement
import models.storage.event.old.move.{MoveNode, MoveObject}
import models.storage.event.old.observation.Observation
import models.storage.event.old.observation.ObservationSubEvents._
import models.storage.{FromToDouble, Interval, LifeCycle}
import no.uio.musit.models.ObjectTypes.CollectionObject
import no.uio.musit.models.{MuseumId, ObjectId, StorageNodeDatabaseId}
import no.uio.musit.test.MusitSpecWithApp
import org.joda.time.DateTime
import repositories.storage.old_dao.event.EventDao

trait EventGenerators_Old extends EventTypeInitializers_Old with BaseDummyData {
  self: MusitSpecWithApp =>

  def eventDao: EventDao = fromInstanceCache[EventDao]

  def addControl(mid: MuseumId, ctrl: Control) = {
    val ctrlAsDto = DtoConverters.CtrlConverters.controlToDto(ctrl)
    eventDao.insertEvent(mid, ctrlAsDto)
  }

  def addObservation(mid: MuseumId, obs: Observation) = {
    val obsAsDto = DtoConverters.ObsConverters.observationToDto(obs)
    eventDao.insertEvent(mid, obsAsDto)
  }

  def addEnvRequirement(mid: MuseumId, envReq: EnvRequirement) = {
    val erAsDto = DtoConverters.EnvReqConverters.envReqToDto(envReq)
    eventDao.insertEvent(mid, erAsDto)
  }
}

trait EventTypeInitializers_Old { self: BaseDummyData =>

  def createControl(storageNodeId: Option[StorageNodeDatabaseId] = None) = {
    Control(
      id = None,
      doneDate = DateTime.now.minusDays(1),
      registeredBy = Some(defaultActorId),
      registeredDate = Some(DateTime.now),
      doneBy = Some(defaultActorId),
      affectedThing = storageNodeId,
      eventType = EventType.fromEventTypeId(ControlEventType.id),
      temperature = Some(createTemperatureControl()),
      alcohol = Some(createAlcoholControl()),
      cleaning = Some(createCleaningControl(ok = true)),
      pest = Some(createPestControl())
    )
  }

  def createObservation(storageNodeId: Option[StorageNodeDatabaseId] = None) = {
    Observation(
      id = None,
      doneDate = DateTime.now.minusDays(1),
      registeredBy = Some(defaultActorId),
      registeredDate = Some(DateTime.now),
      doneBy = Some(defaultActorId),
      affectedThing = storageNodeId,
      eventType = EventType.fromEventTypeId(ObservationEventType.id),
      alcohol = Some(createAlcoholObservation),
      cleaning = Some(createCleaningObservation),
      gas = Some(createGasObservation),
      hypoxicAir = Some(createHypoxicObservation),
      lightingCondition = Some(createLightingObservation),
      mold = Some(createMoldObservation),
      pest = Some(createPestObservation),
      relativeHumidity = Some(createHumidityObservation),
      temperature = Some(createTemperatureObservation),
      theftProtection = Some(createTheftObservation),
      fireProtection = Some(createFireObservation),
      perimeterSecurity = Some(createPerimeterObservation),
      waterDamageAssessment = Some(createWaterDmgObservation)
    )
  }

  def createEnvRequirement(
      storageNodeId: Option[StorageNodeDatabaseId] = None,
      note: Option[String] = Some("This is an envreq note")
  ) = {
    EnvRequirement(
      id = None,
      doneDate = DateTime.now.minusDays(1),
      note = note,
      registeredBy = Some(defaultActorId),
      registeredDate = Some(DateTime.now),
      doneBy = Some(defaultActorId),
      affectedThing = storageNodeId,
      eventType = EventType.fromEventTypeId(EnvRequirementEventType.id),
      temperature = Some(Interval(20, Some(5))),
      airHumidity = Some(Interval(60.0, Some(10))),
      hypoxicAir = Some(Interval(0, Some(15))),
      cleaning = Some("keep it clean, dude"),
      light = Some("dim")
    )
  }

  def createTemperatureControl(ok: Boolean = false): ControlTemperature = {
    ControlTemperature(ok, if (ok) None else Some(createTemperatureObservation))
  }

  def createTemperatureObservation: ObservationTemperature = {
    ObservationTemperature(
      note = Some("This is an observation temperature note"),
      range = FromToDouble(Some(12.32), Some(24.12))
    )
  }

  def createAlcoholControl(ok: Boolean = false): ControlAlcohol =
    ControlAlcohol(ok, if (ok) None else Some(createAlcoholObservation))

  def createAlcoholObservation: ObservationAlcohol =
    ObservationAlcohol(
      note = Some("This is an observation alcohol note"),
      condition = Some("pretty strong"),
      volume = Some(92.30)
    )

  def createCleaningControl(ok: Boolean = false): ControlCleaning =
    ControlCleaning(ok, if (ok) None else Some(createCleaningObservation))

  def createCleaningObservation: ObservationCleaning =
    ObservationCleaning(
      note = Some("This is an observation cleaning note"),
      cleaning = Some("Pretty dirty stuff")
    )

  def createGasObservation: ObservationGas =
    ObservationGas(
      note = Some("This is an observation gas note"),
      gas = Some("Smells like methane")
    )

  def createHypoxicObservation: ObservationHypoxicAir =
    ObservationHypoxicAir(
      note = Some("This is an observation hypoxic air note"),
      range = FromToDouble(Some(11.11), Some(12.12))
    )

  def createLightingObservation: ObservationLightingCondition =
    ObservationLightingCondition(
      note = Some("This is an observation lighting condition note"),
      lightingCondition = Some("Quite dim")
    )

  def createMoldObservation: ObservationMold =
    ObservationMold(
      note = Some("This is an observation mold note"),
      mold = Some("Mold is a fun guy")
    )

  def createHumidityObservation: ObservationRelativeHumidity =
    ObservationRelativeHumidity(
      note = Some("This is an observation humidity note"),
      range = FromToDouble(Some(70.0), Some(75.5))
    )

  def createTheftObservation: ObservationTheftProtection =
    ObservationTheftProtection(
      note = Some("This is an observation theft note"),
      theftProtection = Some("They stole all our stuff!!")
    )

  def createFireObservation: ObservationFireProtection =
    ObservationFireProtection(
      note = Some("This is an observation fire note"),
      fireProtection = Some("Fire extinguisher is almost empty")
    )

  def createPerimeterObservation: ObservationPerimeterSecurity =
    ObservationPerimeterSecurity(
      note = Some("This is an observation perimeter note"),
      perimeterSecurity = Some("Someone has cut a hole in the fence")
    )

  def createWaterDmgObservation: ObservationWaterDamageAssessment =
    ObservationWaterDamageAssessment(
      note = Some("This is an observation water damage note"),
      waterDamageAssessment = Some("The cellar is flooded")
    )

  def createPestControl(ok: Boolean = false): ControlPest =
    ControlPest(ok, if (ok) None else Some(createPestObservation))

  def createPestObservation: ObservationPest =
    ObservationPest(
      note = Some("This is an observation pest note"),
      identification = Some("termintes"),
      lifecycles = Seq(
        LifeCycle(
          stage = Some("mature colony"),
          quantity = Some(100)
        ),
        LifeCycle(
          stage = Some("new colony"),
          quantity = Some(4)
        )
      )
    )

  def createMoveObject(
      objectId: Option[ObjectId] = Some(ObjectId(1)),
      from: Option[StorageNodeDatabaseId],
      to: StorageNodeDatabaseId
  ): MoveObject = {
    MoveObject(
      id = None,
      doneDate = DateTime.now.minusDays(1),
      registeredBy = Some(defaultActorId),
      registeredDate = Some(DateTime.now),
      doneBy = Some(defaultActorId),
      affectedThing = objectId,
      eventType = EventType.fromEventTypeId(MoveObjectType.id),
      objectType = CollectionObject,
      from = from,
      to = to
    )
  }

  def createMoveNode(
      nodeId: Option[StorageNodeDatabaseId] = Some(StorageNodeDatabaseId(1)),
      from: Option[StorageNodeDatabaseId],
      to: StorageNodeDatabaseId
  ): MoveNode = {
    MoveNode(
      id = None,
      doneDate = DateTime.now.minusDays(1),
      registeredBy = Some(defaultActorId),
      registeredDate = Some(DateTime.now),
      doneBy = Some(defaultActorId),
      affectedThing = nodeId,
      eventType = EventType.fromEventTypeId(MoveNodeType.id),
      from = from,
      to = to
    )
  }
}
