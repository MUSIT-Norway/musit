package no.uio.musit.microservice.event.domain

import no.uio.musit.microservice.event.dao.EnvRequirementDAO.EnvRequirementDto
import no.uio.musit.microservice.event.dao.EventDao.BaseEventDto
import no.uio.musit.microservice.event.service.SimpleService
import no.uio.musit.microservices.common.domain.MusitInternalErrorException
import no.uio.musit.microservices.common.extensions.FutureExtensions.{MusitFuture, MusitResult}
import no.uio.musit.microservices.common.linking.domain.Link
import no.uio.musit.microservices.common.utils.{ErrorHelper, ResourceHelper}
import play.api.libs.json._
import slick.dbio.DBIO
import no.uio.musit.microservices.common.extensions.EitherExtensions._
import no.uio.musit.microservices.common.extensions.OptionExtensions._
import no.uio.musit.microservices.common.utils.Misc._


object BaseEventProps {
  def fromEvent(evt: Event) = BaseEventProps(evt.id, evt.links, evt.eventType, evt.note)

  def fromBaseEventDto(eventDto: BaseEventDto) = BaseEventProps(eventDto.id, eventDto.links, eventDto.eventType, eventDto.note)

  implicit object baseEventPropsWrites extends Writes[BaseEventProps] {

    def writes(a: BaseEventProps): JsValue = {
      Json.obj(
        "id" -> a.id,
        "links" -> a.links,
        "type" -> a.eventType,
        "note" -> a.note
      )
    }
  }
}

case class BaseEventProps(id: Option[Long], links: Option[Seq[Link]], eventType: EventType, note: Option[String]) {
  def toBaseEventDto = BaseEventDto(this.id, this.links, this.eventType, this.note)
  def toJson: JsObject = Json.toJson(this).asInstanceOf[JsObject]
}


class Event(val baseEventProps: BaseEventProps) {
  val id: Option[Long] = baseEventProps.id
  val note: Option[String] = baseEventProps.note
  val links: Option[Seq[Link]] = baseEventProps.links
  val eventType = baseEventProps.eventType


  final def eventDtoToStoreInDatabase = this.specifyCustomData(baseEventProps.toBaseEventDto)


  //Extension points
  /** If you need to store anything in valueInteger or valueString, override this method to provide this data. Gets called before the data is written to the database
    **
    *NB: Remember to call super!
    *
    *@example
    **
    *override def specifyCustomData(baseEventDto: BaseEventDto) = {
    *val data = super.specifyCustomData(baseEventDto)
    **
    *data.copy(valueInteger = this.someProperty)
    *}
    */
  def specifyCustomData(baseEventDto: BaseEventDto) = baseEventDto


}


/**
  * We split events into two kinds:
  * 1) Those which store all their data in the base event table. We call these "Simple" event types.
  * 2) Those which have extended properties (ie need a separate table of properties), we call these "Complex" event types.

  */

/**
  * For event types which don't need to store extra properties than what is in the base event table.
  * */
trait SimpleEventType {
  /** creates an instance of the (simple) Event. May read custom stuff from valueInteger etc. Called after reading from the database. */
  def createEventInMemory(baseEventDto: BaseEventDto): MusitResult[Event]
}

/** For events which needs an extra table (or potentially even more) to store their data in. */

trait ComplexEventType {
  /** reads the extended/specific properties from the database and creates (in memory) the final event object. May read custom stuff from valueInteger etc */
  def fromDatabase(id: Long, baseEventDto: BaseEventDto): MusitFuture[Event]

  /** creates an action which inserts the extended/specific properties into the database */
  def createDatabaseInsertAction(id: Long, event: Event): DBIO[Int]
}

trait JsonHandler {
  /** creates an Event instance (of proper eventType) from jsObject. The base event data is already read into baseResult */
  def fromJson(eventType: EventType, baseResult: JsResult[BaseEventProps], jsObject: JsObject): JsResult[Event]

  /** Writes the extended/specific properties to a JsObject */
  def toJson(event: Event): JsValue
}


object EventHelpers {
  private def fromJsonToBaseEventProps(eventType: EventType, jsObject: JsObject): JsResult[BaseEventProps] = {
    for {
      id <- (jsObject \ "id").validateOpt[Long]
      links <- (jsObject \ "links").validateOpt[Seq[Link]]
      note <- (jsObject \ "note").validateOpt[String]
    } yield BaseEventProps(id, links, eventType, note)
  }

  def fromJsonToEventResult(eventType: EventType, jsObject: JsObject): JsResult[Event] = {
    val jsResBaseEventProps = fromJsonToBaseEventProps(eventType, jsObject)
    eventType.maybeJsonHandler match {
      case Some(jsonHandler) => jsonHandler.fromJson(eventType, jsResBaseEventProps, jsObject)
      case None =>
        jsResBaseEventProps.flatMap{
          baseEventProps =>
            eventType.simpleOrComplexEventType match {
              case Left(simpleEventType) =>
                val res = simpleEventType.createEventInMemory(baseEventProps.toBaseEventDto)  |> ResourceHelper.musitResultToJsResult
                res
              case Right(complexEventType) => EventType.complexEventTypeWithoutJsonHandlerInternalError(eventType.name)
            }
        }
    }
    /*#OLD
    eventType.eventFactory match {
      case Some(evtController) => evtController.fromJson(eventType, baseEventProps, jsObject)
      case None => baseEventProps.map(dto => new Event(eventType, dto))
    }
    */

  }

  def validateEvent(jsObject: JsObject): MusitResult[Event] = {
    val evtTypeName = (jsObject \ "type").as[String]
    val maybeEventTypeResult = EventType.getByName(evtTypeName).toMusitResult(ErrorHelper.badRequest(s"Unknown eventType: $evtTypeName"))

    val maybeEventResult = maybeEventTypeResult.flatMap {
      eventType => fromJsonToEventResult(eventType, jsObject) |> ResourceHelper.jsResultToMusitResult
    }
    maybeEventResult
  }

  def eventFromJson[T <: Event](jsValue: JsValue): MusitResult[T] = {
    validateEvent(jsValue.asInstanceOf[JsObject]).map(res => res.asInstanceOf[T])
  }

  /*#OLD
  def fromDatabaseToEvent(eventType: EventType, id: Long, baseEventDto: BaseEventProps): MusitFuture[Event] = {

    baseEventDto => baseEventDto.eventType.simpleOrComplexEventType match {
      case Left(simpleEventType) => simpleEventType.createEventInMemory(baseEventDto).toMusitFuture
      case Right(complexEventType) => complexEventType.fromDatabase(id, baseEventDto)
    }
    }
    */




  //#OLD def eventFactoryFor(event: Event) = event.eventType.eventFactory

  def toJson(event: Event) = {
    val baseJson = event.baseEventProps.toJson // Json.toJson(event.baseEventProps).asInstanceOf[JsObject]
    event.eventType.maybeJsonHandler.fold(baseJson)(jsonHandler => baseJson ++ (jsonHandler.toJson(event).asInstanceOf[JsObject]))
  }
}

//Example of a simple event....
class Move(/*eventType: EventType, */baseProps: BaseEventProps) extends Event(baseProps)


object Move extends SimpleEventType {

  def createEventInMemory(baseEventDto: BaseEventDto): MusitResult[Event] = {
    MusitResult(new Move(baseEventDto.props))
  }
}




class EnvRequirement(val baseProps: BaseEventProps, val envReqDto: EnvRequirementDto) extends Event(baseProps) {
  val temperature = envReqDto.temperature
  val airHumidity = envReqDto.airHumidity
  //todo....

}



/*
trait EventFields {
  val eventType: EventType
  val id: Option[Long]
  val links: Option[Seq[Link]]
  val note: Option[String]
}

trait EventWithSubEvents extends EventFields {
  val subEvents: Option[Seq[Event]]
}

sealed trait Event extends EventFields

object MoveO {
  sealed trait MoveOp extends EventFields {
    val eventType = EventType.getByName(this.getClass.getSimpleName)
  }
  case class Move(
    id: Option[Long],
    links: Option[Seq[Link]],
    note: Option[String],
    subEvents: Option[Seq[MoveOp]]
  ) extends MoveOp

  object MoveOp {
    implicit lazy val format: OFormat[MoveOp] = flat.oformat((__ \ "type").format[String])
  }
}

case class Move(
    id: Option[Long],
    links: Option[Seq[Link]],
    note: Option[String],
    subEvents: Option[Seq[MoveO.MoveOp]]
) extends Event with MoveO.MoveOp {
  //val eventType = EventType.getByName(this.getClass.getSimpleName)
}

object Move {
  implicit val format = Json.format[Move]
}

trait ControlSpesific extends EventFields {
  val ok: Boolean
  val observation: Option[ObservationSpesific] //only relevant for insert not select. Gets translated into motivatedBy-relation
}

case class Control(
    id: Option[Long],
    links: Option[Seq[Link]],
    note: Option[String],
    subEvents: Option[Seq[Event]]
) extends Event {
  val eventType = EventType.getByName(this.getClass.getSimpleName)
}

case class ControlTemperature(
    id: Option[Long],
    links: Option[Seq[Link]],
    note: Option[String],
    subEvents: Option[Seq[Event]],
    ok: Boolean,
    observation: Option[ObservationSpesific]
) extends Event with ControlSpesific {
  val eventType = EventType.getByName(this.getClass.getSimpleName)
}

sealed trait ObservationSpesific extends EventFields

case class ObservationTemperature(
    id: Option[Long],
    links: Option[Seq[Link]],
    note: Option[String]
) extends ObservationSpesific {
  val eventType = EventType.getByName(this.getClass.getSimpleName)
}

object ObservationSpesific {
  implicit lazy val format: OFormat[ObservationSpesific] = flat.oformat((__ \ "type").format[String])
}

case class Observation(
    id: Option[Long],
    links: Option[Seq[Link]],
    note: Option[String],
    subEvents: Option[Seq[Event]]
//temperature: Option[Double]
) extends Event {
  val eventType = EventType.getByName(this.getClass.getSimpleName)
}

/*object Observation {
  // FIXME should possibly reside in the service, since the service is the integrator between dto and public domain
  def apply(base: EventBaseDto, observation: ObservationDTO): Observation =
    Observation(
      base.id,
      base.links,
      base.note,
      None,
      observation.temperature
    )
}*/

case class EnvRequirement(
    id: Option[Long],
    links: Option[Seq[Link]],
    note: Option[String],
    temperature: Option[Int],
    temperatureInterval: Option[Int],
    airHumidity: Option[Int],
    airHumidityInterval: Option[Int],
    hypoxicAir: Option[Int],
    hypoxicAirInterval: Option[Int],
    cleaning: Option[String],
    light: Option[String]
) extends Event {
  val eventType = EventType.getByName(this.getClass.getSimpleName)
}

object Event {
  implicit lazy val format: OFormat[Event] = flat.oformat((__ \ "type").format[String])
}
*/