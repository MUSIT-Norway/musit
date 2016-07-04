package no.uio.musit.microservice.event.domain

import no.uio.musit.microservice.event.service._
import no.uio.musit.microservices.common.extensions.OptionExtensions._
import play.api.libs.json.{JsPath, Json, Writes}
case class EventType(id: Int, name: String, simpleOrComplexEventType: Either[SimpleEventType, ComplexEventType], maybeJsonHandler: Option[JsonHandler])
{ //println(s"Event name: $name")


  def maybeComplexEventType: Option[ComplexEventType] = simpleOrComplexEventType.fold(_ => None, Some(_))
  def maybeSimpleEventType = simpleOrComplexEventType.fold(Some(_), _ => None)
}




object EventType {


  private def simpleEventType(id: Int, name: String, simpleEventType: SimpleEventType, maybeJsonHandler: Option[JsonHandler] = None) =
    new EventType(id, name, Left(simpleEventType), maybeJsonHandler)

  private def complexEventType(id: Int, name: String, complexEventType: ComplexEventType, jsonHandler: JsonHandler) = {
    new EventType(id, name, Right(complexEventType), Some(jsonHandler))
  }

  private val eventTypes = Seq(
    simpleEventType(1, "Move", Move),
      complexEventType(5, "EnvRequirement", EnvRequirementComplexEventType, EnvRequirementJson)

      /*
    EventType(2, "Control", ControlService),
    EventType(3, "Observation", ObservationService),
    EventType(4, "ControlTemperature", ControlTemperatureService),*/
  //EventType(5, ControlAir.getClass.getSimpleName, ControlService)
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
