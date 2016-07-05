package no.uio.musit.microservice.event.domain

import no.uio.musit.microservice.event.dao.EnvRequirementDAO.EnvRequirementDto
import no.uio.musit.microservice.event.dao.EventDao.BaseEventDto
import no.uio.musit.microservices.common.extensions.EitherExtensions._
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.extensions.OptionExtensions._
import no.uio.musit.microservices.common.linking.domain.Link
import no.uio.musit.microservices.common.utils.Misc._
import no.uio.musit.microservices.common.utils.{ErrorHelper, ResourceHelper}
import play.api.libs.json._
import slick.dbio.DBIO

import scala.collection.parallel.mutable
import scala.concurrent.Future

object BaseEventProps {
  def fromBaseEventDto(eventDto: BaseEventDto) = BaseEventProps(eventDto.id, eventDto.links, eventDto.eventType, eventDto.note)

  implicit object baseEventPropsWrites extends Writes[BaseEventProps] {

    def writes(a: BaseEventProps): JsValue = {
      Json.obj(
        "id" -> a.id,
        "links" -> a.links,
        "eventType" -> a.eventType,
        "note" -> a.note
      )
    }
  }

}

case class BaseEventProps(id: Option[Long], links: Option[Seq[Link]], eventType: EventType, note: Option[String]) {
  /** Copies all data except custom event data over to the baseEventDto object */
  def toBaseEventDto(parentId: Option[Long]) = BaseEventDto(this.id, this.links, this.eventType, this.note, parentId)

  def toJson: JsObject = Json.toJson(this).asInstanceOf[JsObject]
}

class Event(val baseEventProps: BaseEventProps) {
  val id: Option[Long] = baseEventProps.id
  val note: Option[String] = baseEventProps.note
  val links: Option[Seq[Link]] = baseEventProps.links
  val eventType = baseEventProps.eventType

  private var subEvents = Seq.empty[Event]


  def getTempSubEvents = subEvents // TODO: Make subEvents mutable or wrap it in an Atom 

  protected var parent: Option[Event] = None //The part of relation

  def hasSubEvents = subEvents.length > 0

  def addSubEvents(subEvents: Seq[Event]) = {
    this.subEvents = this.subEvents ++ subEvents
    subEvents.foreach(subEvent => subEvent.parent = Some(this))
  }
}

/**
  * We split event implementations into three kinds:
  * 1) Those which store all their data in the base event table and doesn't use the custom generic fields (valueAsInteger etc). This means single table and single dto.
  * 2) Those which store all their data in the base event table, but also use the custom generic fields. This means single table and baseProps and a custom dto.
  * 3) Those which needs a separate table. They are not allowed to use the custom generic fields. This means single table and baseProps and a custom dto.
  */

trait Dto

sealed trait EventImplementation

trait MultipleDtosEventType {
  def createEventInMemory(baseProps: BaseEventProps, customDto: Dto): Event

  //Json-stuff, consider moving this to a separate trait.
  def validateCustomDto(jsObject: JsObject): JsResult[Dto]

  def customDtoToJson(event: Event): JsObject
}

/**
  * For event types which don't need to store extra properties than what is in the base event table and doesn't use the custom generic fields.
  */
trait SingleDtoEventType {
  def createEventInMemory(baseEventProps: BaseEventProps): Event
}

/**
  * For event types which don't need to store extra properties than what is in the base event table and doesn't use the custom generic fields.
  */

trait SingleTableSingleDto extends EventImplementation with SingleDtoEventType {
}

/**
  * For event types which don't need to store extra properties than what is in the base event table, but does use custom generic fields in the base event table.
  *
  * Implement this event type if you need to store anything in valueInteger or valueString.
  *
  * Remember to call super if you implement further subtypes of this event implementation type
  */
trait SingleTableMultipleDtos extends EventImplementation with MultipleDtosEventType {

  /**
    * Interprets/reads the custom fields it needs (and copies them into the Dto).
    */
  def baseTableToCustomDto(baseEventDto: BaseEventDto): Dto

  /**
    * Stores the custom values into a BaseEventDto instance.
    * Use this if you need to store anything in valueInteger or valueString, override this method to provide this data. Gets called before the data is written to the database
    */
  def customDtoToBaseTable(event: Event, baseEventDto: BaseEventDto): BaseEventDto

}

/**
  * For event types which has their own extra properties table. Does *not* use any of the custom generic fields in the base event table.
  */
trait MultipleTablesMultipleDtos extends EventImplementation with MultipleDtosEventType {
  /** creates an action which inserts the extended/specific properties into the database */
  def createInsertCustomDtoAction(id: Long, event: Event): DBIO[Int]

  /** reads the extended/specific properties from the database. Won't typically need the baseEventDto parameter, remove this? */
  def getCustomDtoFromDatabase(id: Long, baseEventDto: BaseEventDto): Future[Option[Dto]] //? MusitFuture[Dto]

  def getEventFromDatabase(id: Long, baseEventDto: BaseEventDto) = {
    getCustomDtoFromDatabase(id, baseEventDto)
      .toMusitFuture(ErrorHelper.badRequest(s"Unable to find ${baseEventDto.eventType.name} with id: $id"))
      .musitFutureMap(customDto => createEventInMemory(baseEventDto.props, customDto))
  }
}

/*#OLD
trait JsonHandler {
  /** creates an Event instance (of proper eventType) from jsObject. The base event data is already read into baseResult */
  def fromJson(eventType: EventType, baseResult: JsResult[BaseEventProps], jsObject: JsObject): JsResult[Event]

  /** Writes the extended/specific properties to a JsObject */
  def toJson(event: Event): JsValue
}
*/

object EventHelpers {
  private def fromJsonToBaseEventProps(eventType: EventType, jsObject: JsObject): JsResult[BaseEventProps] = {
    for {
      id <- (jsObject \ "id").validateOpt[Long]
      links <- (jsObject \ "links").validateOpt[Seq[Link]]
      note <- (jsObject \ "note").validateOpt[String]
    } yield BaseEventProps(id, links, eventType, note)
  }

  def invokeJsonValidator(multipleDtos: MultipleDtosEventType, eventType: EventType, jsResBaseEventProps: JsResult[BaseEventProps], jsObject: JsObject) = {
    for {
      baseProps <- jsResBaseEventProps
      customDto <- multipleDtos.validateCustomDto(jsObject)
    } yield multipleDtos.createEventInMemory(baseProps, customDto)
  }

  def fromJsonToEventResult(eventType: EventType, jsObject: JsObject): JsResult[Event] = {
    val jsResBaseEventProps = fromJsonToBaseEventProps(eventType, jsObject)
    eventType.singleOrMultipleDtos match {
      case Left(singleDto) =>
        jsResBaseEventProps.map {
          baseEventProps =>
            singleDto.createEventInMemory(baseEventProps)
        }

      case Right(multipleDtos) => invokeJsonValidator(multipleDtos, eventType, jsResBaseEventProps, jsObject)

    }
  }

  def validateSingleEvent(jsObject: JsObject): MusitResult[Event] = {
    val evtTypeName = (jsObject \ "eventType").as[String]
    val maybeEventTypeResult = EventType.getByName(evtTypeName).toMusitResult(ErrorHelper.badRequest(s"Unknown eventType: $evtTypeName"))

    val maybeEventResult = maybeEventTypeResult.flatMap {
      eventType => fromJsonToEventResult(eventType, jsObject) |> ResourceHelper.jsResultToMusitResult
    }
    maybeEventResult
  }


  /** Handles recursion */
  def validateEvent(jsObject: JsObject): MusitResult[Event] = {
    val maybeEventResult = validateSingleEvent(jsObject)
    maybeEventResult.map {
      eventResult =>
        (jsObject \ "subEvents").toOption match {
          case Some(subEventsAsJson) =>
            println("hallo, har subEvents!")
            subEventsAsJson match {
              case jsArray: JsArray =>
                val subEvents = jsArray.value.map(jsValue => validateEvent(jsValue.asInstanceOf[JsObject]))
                val concatenatedMusitResults = concatenateMusitResults(subEvents)
                concatenatedMusitResults match {
                  case Left(error) => Left(error)
                  case Right(reallySubEvents) =>
                    println(s"really subEvents: $reallySubEvents")
                    eventResult.addSubEvents(reallySubEvents)
                    eventResult
                }

              case _ => ErrorHelper.badRequest("expected array of subEvents in subEvent property")
            }

            eventResult
          case None => eventResult
        }
    }
  }


  def eventFromJson[T <: Event](jsValue: JsValue): MusitResult[T] = {
    validateEvent(jsValue.asInstanceOf[JsObject]).map(res => res.asInstanceOf[T])
  }

  def toJson(event: Event) = {
    val baseJson = event.baseEventProps.toJson // Json.toJson(event.baseEventProps).asInstanceOf[JsObject]
    event.eventType.maybeMultipleDtos.fold(baseJson)(jsonWriter => baseJson ++ (jsonWriter.customDtoToJson(event).asInstanceOf[JsObject]))
  }

  def eventDtoToStoreInDatabase(event: Event, parentId: Option[Long]) = {
    event.eventType.maybeSingleTableMultipleDtos match {
      case Some(singleTableMultipleDtos) => singleTableMultipleDtos.customDtoToBaseTable(event, event.baseEventProps.toBaseEventDto(parentId))
      case None => event.baseEventProps.toBaseEventDto(parentId)
    }
  }
}

//Example of a simple event....
class Move(baseProps: BaseEventProps) extends Event(baseProps)

object Move extends SingleTableSingleDto {

  def createEventInMemory(baseEventProps: BaseEventProps): Event = {
    new Move(baseEventProps)
  }
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