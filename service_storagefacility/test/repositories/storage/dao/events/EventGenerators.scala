package repositories.storage.dao.events

import models.storage.{FromToDouble, LifeCycle}
import models.storage.event.EventType
import models.storage.event.EventTypeRegistry.TopLevelEvents.{
  ControlEventType,
  MoveNodeType,
  MoveObjectType,
  ObservationEventType
}
import models.storage.event.control.Control
import models.storage.event.control.ControlAttributes._
import models.storage.event.move.{MoveNode, MoveObject}
import models.storage.event.observation.Observation
import models.storage.event.observation.ObservationAttributes._
import no.uio.musit.models.{ActorId, ObjectTypes, ObjectUUID, StorageNodeId}
import org.joda.time.DateTime

trait EventGenerators {

  val defaultActorId    = ActorId.generate()
  val defaultObjectUUID = ObjectUUID.generate()
  val defaultNodeId     = StorageNodeId.generate()
  val firstNodeId       = StorageNodeId.generate()
  val secondNodeId      = StorageNodeId.generate()

  def createMoveObject(
      objectId: Option[ObjectUUID] = Some(defaultObjectUUID),
      from: Option[StorageNodeId],
      to: StorageNodeId
  ): MoveObject = {
    MoveObject(
      id = None,
      doneDate = DateTime.now.minusDays(1),
      registeredBy = Some(defaultActorId),
      registeredDate = Some(DateTime.now),
      doneBy = Some(defaultActorId),
      affectedThing = objectId,
      eventType = EventType.fromEventTypeId(MoveObjectType.id),
      objectType = ObjectTypes.CollectionObject,
      from = from,
      to = to
    )
  }

  def createMoveNode(
      nodeId: Option[StorageNodeId] = Some(defaultNodeId),
      from: Option[StorageNodeId],
      to: StorageNodeId
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

  def createControl(storageNodeId: Option[StorageNodeId] = None) = {
    Control(
      id = None,
      doneDate = DateTime.now.minusDays(1),
      registeredBy = Some(defaultActorId),
      registeredDate = Some(DateTime.now),
      doneBy = Some(defaultActorId),
      affectedThing = Some(defaultNodeId),
      eventType = EventType.fromEventTypeId(ControlEventType.id),
      temperature = Some(createTemperatureControl()),
      alcohol = Some(createAlcoholControl()),
      cleaning = Some(createCleaningControl(ok = true)),
      pest = Some(createPestControl())
    )
  }

  def createObservation(storageNodeId: Option[StorageNodeId] = None) = {
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
}
