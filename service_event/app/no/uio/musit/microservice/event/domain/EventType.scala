package no.uio.musit.microservice.event.domain

import no.uio.musit.microservice.event.service.{ ControlFactory, EventFactory, MoveFactory, ObservationFactory }

case class EventType(id: Int, name: String, eventFactory: EventFactory)

object EventType {

  private val eventTypes = Seq(
    EventType(1, Move.getClass.getSimpleName, MoveFactory),
    EventType(2, Control.getClass.getSimpleName, ControlFactory),
    EventType(3, Observation.getClass.getSimpleName, ObservationFactory)
  // Add new event type here....
  )

  private val eventTypeById: Map[Int, EventType] = eventTypes.map(evt => evt.id -> evt).toMap
  private val eventTypeByName: Map[String, EventType] = eventTypes.map(evt => evt.name.toLowerCase -> evt).toMap

  def getByName(name: String) = eventTypeByName.get(name.toLowerCase).get

  def getById(id: Int) = eventTypeById.get(id).get
}
