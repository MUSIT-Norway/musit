package no.uio.musit.microservice.event.domain

import no.uio.musit.microservices.common.linking.domain.Link
import play.api.libs.json._

trait Dto



case class RelatedEvents(relation: EventRelation, events: Seq[Event])

class Event(val baseEventProps: BaseEventDto) {
  val id: Option[Long] = baseEventProps.id
  val note: Option[String] = baseEventProps.note
  val links: Option[Seq[Link]] = baseEventProps.links
  val eventType = baseEventProps.eventType

  val relatedSubEvents = baseEventProps.relatedSubEvents

  def getCustomBool = CustomValuesInEventTable.getBool(this)
  def getCustomOptBool = CustomValuesInEventTable.getOptBool(this)
  def getCustomString = CustomValuesInEventTable.getString(this)
  def getCustomOptString = CustomValuesInEventTable.getOptString(this)


  def subEventsWithRelation(eventRelation: EventRelation) = relatedSubEvents.find(p => p.relation == eventRelation).map(_.events)

  def hasSubEvents = relatedSubEvents.length > 0 //We assume none of the event-lists are empty. This is perhaps a wrong assumption.

  //Maybe not needed, just for convenience
  def getAllSubEvents = relatedSubEvents.flatMap(relatedEvents => relatedEvents.events)

  def getAllSubEventsAs[T] = getAllSubEvents.map(subEvent => subEvent.asInstanceOf[T])

  /*#OLD ideas

  //protected var partOf: Option[Event] = None //The part_of relation. ("Semantic")
  // protected var jsonParent: Option[Event] = None //The parent container element in the json structure. ("Non-semantic")

  def addSubEvents(relation: EventRelation, subEvents: Seq[Event]) = {
    assert(subEvents.length > 0)

    this.subEvents = this.subEvents :+ RelatedEvents(relation, subEvents)
    //subEvents.foreach(subEvent => subEvent.jsonParent = Some(this))

    /*
    if(relation==EventRelations.relation_parts)
      subEvents.foreach(subEvent => subEvent.partOf = Some(this)) */
  }*/
}

object Constants {
  val subEventsPrefix = "subEvents-"
}


case class EnvRequirementDto(id: Option[Long],
                             temperature: Option[Int],
                             tempInterval: Option[Int],
                             airHumidity: Option[Int],
                             airHumInterval: Option[Int],
                             hypoxicAir: Option[Int],
                             hypoxicInterval: Option[Int],
                             cleaning: Option[String],
                             light: Option[String]) extends Dto

object EnvRequirementDto {
  implicit val format = Json.format[EnvRequirementDto]
}


object ControlSpecificDtoSpec {
  val customFieldsSpec = CustomEventFieldsSpec().defineRequiredBoolean("ok")
}


case class ObservationFromToDto(id: Option[Long],
                                from: Option[Double],
                                to: Option[Double]) extends Dto

object ObservationFromToDto {
  implicit val format = Json.format[ObservationFromToDto]
}


object ObservationLysDtoSpec {
  val customFieldsSpec = CustomEventFieldsSpec().defineOptString("lysforhold")
}



