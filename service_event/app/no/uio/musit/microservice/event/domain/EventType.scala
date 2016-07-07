package no.uio.musit.microservice.event.domain

import no.uio.musit.microservice.event.service._
import no.uio.musit.microservices.common.extensions.OptionExtensions._
import play.api.libs.json.{ Json, Writes }

object EventType {

  private def eventType(id: Int, name: String, eventImplementation: EventImplementation) = {
    new EventType(id, name, eventImplementation)
  }

  private val eventTypes = Seq(
    eventType(1, "Move", Move),
    eventType(2, "Control", ControlService),
    eventType(3, "Observation", ObservationService),
    eventType(4, "ControlTemperature", ControlTemperatureService),
    eventType(5, "ControlAir", ControlAirService),
    eventType(6, "EnvRequirement", EnvRequirementService),
    eventType(7, "ObservationTemperature", ObservationTemperatureService),
    eventType(8, "ObservationRelativeHumidity", ObservationRelativeHumidityService),
    eventType(9, "ObservationInertAir", ObservationInertAirService),
    eventType(10, "ObservationLys", ObservationLysService)

  // Add new event type here....
  )

  private val eventTypeById: Map[Int, EventType] = eventTypes.map(evt => evt.id -> evt).toMap
  private val eventTypeByName: Map[String, EventType] = eventTypes.map(evt => evt.name.toLowerCase -> evt).toMap

  def getByName(name: String) = eventTypeByName.get(name.toLowerCase)

  def getByNameOrFail(name: String) = getByName(name).getOrFail(s"Unable to find event type : $name")

  def getById(id: Int) = eventTypeById.get(id).get

  implicit val evenTypeWrites = new Writes[EventType] {
    def writes(eventType: EventType) = Json.toJson(eventType.name)
  }
}

case class EventType(id: Int, name: String, eventImplementation: EventImplementation /*, maybeJsonHandler: Option[JsonHandler]*/ ) {
  //println(s"Event name: $name")

  def maybeMultipleTablesMultipleDtos = {
    eventImplementation match {
      case s: MultipleTablesNotUsingCustomFields => Some(s)
      case _ => None
    }
  }

  def maybeMultipleDtos = {
    eventImplementation match {
      case s: MultipleTablesEventType => Some(s)
      case _ => None
    }
  }

  def singleOrMultipleDtos: Either[SingleTableEventType, MultipleTablesEventType] = {
    eventImplementation match {
      case s: SingleTableEventType => Left(s)
      case s: MultipleTablesEventType => Right(s)
    }
  }

  def maybeSingleTableMultipleDtos = {
    eventImplementation match {
      case s: SingleTableUsingCustomFields => Some(s)
      case _ => None
    }
  }
}
