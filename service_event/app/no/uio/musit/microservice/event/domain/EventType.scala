package no.uio.musit.microservice.event.domain

import no.uio.musit.microservice.event.service._

case class EventType(id: Int, name: String, eventFactory: EventService)

object EventType {

  private val eventTypes = Seq(
    EventType(1, Move.getClass.getSimpleName, MoveService),
    EventType(2, Control.getClass.getSimpleName, ControlService),
    EventType(3, Observation.getClass.getSimpleName, ObservationService),
    EventType(4, ControlTemperature.getClass.getSimpleName, ControlTemperatureService),
    EventType(4, EnvRequirement.getClass.getSimpleName, EnvRequirementService)
  //EventType(5, ControlAir.getClass.getSimpleName, ControlService)
  // Add new event type here....
  )

  private val eventTypeById: Map[Int, EventType] = eventTypes.map(evt => evt.id -> evt).toMap
  private val eventTypeByName: Map[String, EventType] = eventTypes.map(evt => evt.name.toLowerCase -> evt).toMap

  def getByName(name: String) = eventTypeByName.get(name.toLowerCase).get

  def getById(id: Int) = eventTypeById.get(id).get
}
