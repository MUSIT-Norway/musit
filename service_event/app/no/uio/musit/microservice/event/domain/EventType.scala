package no.uio.musit.microservice.event.domain

import no.uio.musit.microservice.event.service._
import no.uio.musit.microservices.common.extensions.OptionExtensions._
case class EventType(id: Int, name: String, eventFactory: EventService)
{println(s"Event name: $name")}

object EventType {

  private val eventTypes = Seq(
    EventType(1, "Move", MoveService),
    EventType(2, "Control", ControlService),
    EventType(3, "Observation", ObservationService),
    EventType(4, "ControlTemperature", ControlTemperatureService),
    EventType(5, "EnvRequirement", EnvRequirementService)
  //EventType(5, ControlAir.getClass.getSimpleName, ControlService)
  // Add new event type here....
  )

  private val eventTypeById: Map[Int, EventType] = eventTypes.map(evt => evt.id -> evt).toMap
  private val eventTypeByName: Map[String, EventType] = eventTypes.map(evt => evt.name.toLowerCase -> evt).toMap

  def getByName(name: String) = eventTypeByName.get(name.toLowerCase).getOrFail(s"Unable to find event type : $name")
/*
  def getByNameIgnoreDollar(name: String) = {
    assert(name.endsWith("$"))
    val nameWithout$:String = name.substring(0, name.length - 2)
    getByName(nameWithout$)

  }
*/
  def getById(id: Int) = eventTypeById.get(id).get
}
