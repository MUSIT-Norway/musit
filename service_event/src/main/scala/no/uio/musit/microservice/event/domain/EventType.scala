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
    eventType(2, "EnvRequirement", EnvRequirementService),
    eventType(3, "Control", ControlService),
    eventType(4, "Observation", ObservationService),

    eventType(5, "ControlAlcohol", ControlAlcoholService),
    eventType(6, "ControlCleaning", ControlCleaningService),
    eventType(7, "ControlGas", ControlGasService),
    eventType(8, "ControlHypoxicAir", ControlHypoxicAirService),
    eventType(9, "ControlLightingCondition", ControlLightingConditionService),
    eventType(10, "ControlMold", ControlMoldService),
    eventType(11, "ControlPest", ControlPestService),
    eventType(12, "ControlRelativeHumidity", ControlRelativeHumidityService),
    eventType(13, "ControlTemperature", ControlTemperatureService),

    eventType(14, "ObservationAlcohol", ObservationAlcoholService),
    eventType(15, "ObservationCleaning", ObservationCleaningService),
    eventType(16, "ObservationFireProtection", ObservationFireProtectionService),
    eventType(17, "ObservationGas", ObservationGasService),
    eventType(18, "ObservationHypoxicAir", ObservationHypoxicAirService),
    eventType(19, "ObservationLightingCondition", ObservationLightingConditionService),
    eventType(20, "ObservationMold", ObservationMoldService),
    eventType(21, "ObservationPerimeterSecurity", ObservationPerimeterSecurityService),
    eventType(22, "ObservationRelativeHumidity", ObservationRelativeHumidityService),
    eventType(23, "ObservationPest", ObservationPestService),
    eventType(24, "ObservationTemperature", ObservationTemperatureService),
    eventType(25, "ObservationTheftProtection", ObservationTheftProtectionService),
    eventType(26, "ObservationWaterDamageAssessment", ObservationWaterDamageAssessmentService)

  // Add new event type here....
  )

  private val eventTypeById: Map[Int, EventType] = eventTypes.map(evt => evt.id -> evt).toMap
  private val eventTypeByName: Map[String, EventType] = eventTypes.map(evt => evt.name.toLowerCase -> evt).toMap

  def getByName(name: String) = eventTypeByName.get(name.toLowerCase)

  /** If not found, a 400 error (with text "Unable to find event type: name") gets returned */
  def getByNameAsMusitResult(name: String) = getByName(name).toMusitResult(MusitError(message = s"Unable to find event type : $name"))

  def getByNameOrFail(name: String) = getByName(name).getOrFail(s"Unable to find event type : $name")

  def getById(id: Int) = eventTypeById.get(id).get

  implicit val evenTypeWrites = new Writes[EventType] {
    def writes(eventType: EventType) = Json.toJson(eventType.name)
  }
}

case class EventType(id: Int, name: String, eventImplementation: EventImplementation) {

  def hasName(someName: String) = name.equalsIgnoreCase(someName)

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
