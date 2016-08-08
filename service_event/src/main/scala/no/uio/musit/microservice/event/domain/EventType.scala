package no.uio.musit.microservice.event.domain

import no.uio.musit.microservice.event.service._
import no.uio.musit.microservices.common.extensions.OptionExtensions._
import play.api.libs.json.{ Json, Writes }
import no.uio.musit.microservices.common.domain.MusitError

object EventType {

  private def eventType(id: Int, name: String, eventImplementation: EventImplementation) = {
    new EventType(id, name, eventImplementation)
  }

  private val eventTypes = Seq(
    eventType(1, "Move", Move),
    eventType(2, "Control", ControlService),
    eventType(3, "Observation", ObservationService),
    eventType(4, "ControlTemperature", ControlTemperatureService),
    eventType(5, "ControlInertluft", ControlInertluftService),
    eventType(6, "EnvRequirement", EnvRequirementService),
    eventType(7, "ObservationTemperature", ObservationTemperatureService),
    eventType(8, "ObservationRelativeHumidity", ObservationRelativeHumidityService),
    eventType(9, "ObservationInertAir", ObservationInertAirService),
    eventType(10, "ObservationLys", ObservationLysService),
    eventType(11, "ObservationSkadedyr", ObservationSkadedyrService),
    eventType(12, "ObservationRenhold", ObservationRenholdService),
    eventType(13, "ObservationGass", ObservationGassService),
    eventType(14, "ObservationMugg", ObservationMuggService),
    eventType(15, "ObservationTyveriSikring", ObservationTyveriSikringService),
    eventType(16, "ObservationBrannSikring", ObservationBrannSikringService),
    eventType(17, "ObservationSkallSikring", ObservationSkallSikringService),
    eventType(18, "ObservationVannskadeRisiko", ObservationVannskadeRisikoService),
    eventType(19, "ObservationSprit", ObservationSpritService),
    eventType(20, "ControlRelativLuftfuktighet", ControlRelativLuftfuktighetService),
    eventType(21, "ControlLysforhold", ControlLysforholdService),
    eventType(22, "ControlRenhold", ControlRenholdService),
    eventType(23, "ControlGass", ControlGassService),
    eventType(24, "ControlMugg", ControlMuggService),
    eventType(25, "ControlSkadedyr", ControlSkadedyrService),
    eventType(26, "ControlSprit", ControlSpritService)

  // Add new event type here....
  )

  private val eventTypeById: Map[Int, EventType] = eventTypes.map(evt => evt.id -> evt).toMap
  private val eventTypeByName: Map[String, EventType] = eventTypes.map(evt => evt.name.toLowerCase -> evt).toMap

  def getByName(name: String) = eventTypeByName.get(name.toLowerCase)

  /** If not found, a 400 error (with text "Unable to find event type: name") gets returned */
  def getByNameAsMusitResult(name: String) = getByName(name).toMusitResult(MusitError(message = s"Unable to find event type : $name"))

  //#OLD def getByNameOrFail(name: String) = getByName(name).getOrFail(s"Unable to find event type : $name")

  def getById(id: Int) = eventTypeById.get(id).get

  implicit val evenTypeWrites = new Writes[EventType] {
    def writes(eventType: EventType) = Json.toJson(eventType.name)
  }
}

case class EventType(id: Int, name: String, eventImplementation: EventImplementation) {

  //Some helper methods, used by the implementation of the event system. Ought to be moved somewhere else.

  def maybeMultipleTables = {
    eventImplementation match {
      case s: MultipleTablesEventType => Some(s)
      case _ => None
    }
  }

  def maybeMultipleDtos = maybeMultipleTables //Earlier on this wasn't the same, I want to preserve the semantic difference
  // in case there' a revolt against the system for storing custom values in the base table! ;)

  def singleOrMultipleDtos: Either[SingleTableEventType, MultipleTablesEventType] = {
    eventImplementation match {
      case s: SingleTableEventType => Left(s)
      case s: MultipleTablesEventType => Right(s)
    }
  }
}
