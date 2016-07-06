package no.uio.musit.microservice.event.domain

import no.uio.musit.microservice.event.dao.EventDao.BaseEventDto
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.linking.domain.Link
import no.uio.musit.microservices.common.utils.ErrorHelper
import play.api.libs.json._
import slick.dbio.DBIO

import scala.concurrent.Future

object BaseEventProps {
  def fromBaseEventDto(eventDto: BaseEventDto) = BaseEventProps(eventDto.id, eventDto.links, eventDto.eventType, eventDto.note)

  implicit object baseEventPropsWrites extends Writes[BaseEventProps] {


    // TODO: Fix this, this currently writes "note": null if no note! 
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

case class RelatedEvents(relation: EventRelation, events: Seq[Event])

class Event(val baseEventProps: BaseEventProps) {
  val id: Option[Long] = baseEventProps.id
  val note: Option[String] = baseEventProps.note
  val links: Option[Seq[Link]] = baseEventProps.links
  val eventType = baseEventProps.eventType

  private var subEvents = Seq.empty[RelatedEvents]

  def getSubEvents = subEvents.flatMap(relatedEvents=> relatedEvents.events) // TODO: Make subEvents mutable or wrap it in an Atom

  def getRelatedSubEvents = subEvents // TODO: Make subEvents mutable or wrap it in an Atom
  protected var parent: Option[Event] = None //The part_of relation

  def hasSubEvents = subEvents.length > 0

  def addSubEvents(relation: EventRelation, subEvents: Seq[Event]) = {
    assert(subEvents.length>0)

    this.subEvents = this.subEvents :+ RelatedEvents(relation, subEvents)
    if(relation==EventRelations.relation_parts)
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
object Constants {

  val subEventsPrefix = "subEvents-"

}


//Example of a simple event....
class Move(baseProps: BaseEventProps) extends Event(baseProps)

object Move extends SingleTableSingleDto {

  def createEventInMemory(baseEventProps: BaseEventProps): Event = {
    new Move(baseEventProps)
  }
}
