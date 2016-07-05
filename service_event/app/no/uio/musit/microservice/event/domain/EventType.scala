package no.uio.musit.microservice.event.domain

import no.uio.musit.microservice.event.service._
//import no.uio.musit.microservice.event.service.ControlTemperatureService
import no.uio.musit.microservices.common.extensions.OptionExtensions._
import play.api.libs.json.{ JsPath, Json, Writes }

case class EventType(id: Int, name: String, eventImplementation: EventImplementation /*, maybeJsonHandler: Option[JsonHandler]*/ ) {
  //println(s"Event name: $name")

  def maybeMultipleTablesMultipleDtos = {
    eventImplementation match {
      case s: MultipleTablesMultipleDtos => Some(s)
      case _ => None
    }
  }

  def maybeMultipleDtos = {
    eventImplementation match {
      case s: MultipleDtosEventType => Some(s)
      case _ => None
    }
  }

  def singleOrMultipleDtos: Either[SingleDtoEventType, MultipleDtosEventType] = {
    eventImplementation match {
      case s: SingleDtoEventType => Left(s)
      case s: MultipleDtosEventType => Right(s)
      //case _ => assert(false, "Internal error in EventType.singleOrMultipleDtos")
    }
  }

  def maybeSingleTableMultipleDtos = {
    eventImplementation match {
      case s: SingleTableMultipleDtos => Some(s)
      case _ => None
    }
  }
}

object EventType {

  /*#OLD
    private def simpleEventType(id: Int, name: String, simpleEventType: SimpleEventType, maybeJsonHandler: Option[JsonHandler] = None) =
      new EventType(id, name, Left(simpleEventType), maybeJsonHandler)

    private def complexEventType(id: Int, name: String, complexEventType: ComplexEventType, jsonHandler: JsonHandler) = {
      new EventType(id, name, Right(complexEventType), Some(jsonHandler))
    }
    */

  private def eventType(id: Int, name: String, eventImplementation: EventImplementation) = {
    new EventType(id, name, eventImplementation)
  }

  private val eventTypes = Seq(
    EventType(1, "Move", Move),
    EventType(2, "Control", ControlService),
    //EventType(4, "ControlTemperature", ControlTemperatureService),
    EventType(5, "EnvRequirement", EnvRequirementEventImplementation)

  /*
  EventType(3, "Observation", ObservationService),
    //EventType(5, ControlAir.getClass.getSimpleName, ControlService) */
  // Add new event type here....
  )

  private val eventTypeById: Map[Int, EventType] = eventTypes.map(evt => evt.id -> evt).toMap
  private val eventTypeByName: Map[String, EventType] = eventTypes.map(evt => evt.name.toLowerCase -> evt).toMap

  def getByName(name: String) = eventTypeByName.get(name.toLowerCase)

  def getByNameOrFail(name: String) = getByName(name).getOrFail(s"Unable to find event type : $name")

  def getById(id: Int) = eventTypeById.get(id).get

  def complexEventTypeWithoutJsonHandlerInternalError(eventName: String) = throw new Exception(s"Internal error, when the event type doesn't have a json handler, it needs to be a simple event type. Event type: $eventName")

  implicit val evenTypeWrites = new Writes[EventType] {
    def writes(eventType: EventType) = Json.toJson(eventType.name)
  }
}
