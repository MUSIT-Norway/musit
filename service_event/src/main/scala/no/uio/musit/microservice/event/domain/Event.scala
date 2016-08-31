package no.uio.musit.microservice.event.domain

import java.sql.{ Date, Timestamp }

import no.uio.musit.microservice.event.service.{ CustomFieldsSpec, CustomValuesInEventTable }
import no.uio.musit.microservices.common.linking.domain.Link
import play.api.libs.json._
import slick.dbio.DBIO

trait Dto

/**Events related (via relation) to a given event. */
case class RelatedEvents(relation: EventRelation, events: Seq[Event])

class Event(val baseEventProps: BaseEventDto) {
  val id: Option[Long] = baseEventProps.id
  val eventDate: Option[Date] = baseEventProps.eventDate
  val registeredDate: Option[Timestamp] = baseEventProps.registeredDate
  val relatedActors = baseEventProps.relatedActors
  val relatedObjects = baseEventProps.relatedObjects
  val note: Option[String] = baseEventProps.note
  val links: Option[Seq[Link]] = baseEventProps.links
  val eventType = baseEventProps.eventType

  val relatedSubEvents = baseEventProps.relatedSubEvents

  def getCustomBool = CustomValuesInEventTable.getBool(this)
  def getCustomOptBool = CustomValuesInEventTable.getOptBool(this)
  def getCustomString = CustomValuesInEventTable.getString(this)
  def getCustomOptString = CustomValuesInEventTable.getOptString(this)
  def getCustomDouble = CustomValuesInEventTable.getDouble(this)
  def getCustomOptDouble = CustomValuesInEventTable.getOptDouble(this)

  def subEventsWithRelation(eventRelation: EventRelation) = relatedSubEvents.find(p => p.relation == eventRelation).map(_.events)

  def hasSubEvents = relatedSubEvents.length > 0 //We assume none of the event-lists are empty. This is perhaps a wrong assumption.

  //Maybe not needed, just for convenience
  def getAllSubEvents = relatedSubEvents.flatMap(relatedEvents => relatedEvents.events)

  def getAllSubEventsAs[T] = getAllSubEvents.map(subEvent => subEvent.asInstanceOf[T])

  def execute: Option[(Long) => DBIO[Unit]] = None
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

case class EnvRequirementDto(
  id: Option[Long],
  temperature: Option[Int],
  tempInterval: Option[Int],
  airHumidity: Option[Int],
  airHumInterval: Option[Int],
  hypoxicAir: Option[Int],
  hypoxicInterval: Option[Int],
  cleaning: Option[String],
  light: Option[String]
) extends Dto

object EnvRequirementDto {
  implicit val format = Json.format[EnvRequirementDto]
}

object ControlSpecificDtoSpec {
  val customFieldsSpec = CustomFieldsSpec().defineRequiredBoolean("ok")
}

case class ObservationFromToDto(
  id: Option[Long],
  from: Option[Double],
  to: Option[Double]
) extends Dto

object ObservationFromToDto {
  implicit val format = Json.format[ObservationFromToDto]
}

object ObservationLightingConditionCustomFieldsSpec {
  val customFieldsSpec = CustomFieldsSpec().defineOptString("lightingCondition")
}

object ObservationCleaningCustomFieldsSpec {
  val customFieldsSpec = CustomFieldsSpec().defineOptString("cleaning")
}

object ObservationGasCustomFieldsSpec {
  val customFieldsSpec = CustomFieldsSpec().defineOptString("gas")
}

object ObservationMoldCustomFieldsSpec {
  val customFieldsSpec = CustomFieldsSpec().defineOptString("mold")
}

object ObservationTheftProtectionCustomFieldsSpec {
  val customFieldsSpec = CustomFieldsSpec().defineOptString("theftProtection")
}

object ObservationFireProtectionCustomFieldsSpec {
  val customFieldsSpec = CustomFieldsSpec().defineOptString("fireProtection")
}

object ObservationPerimeterSecurityCustomFieldsSpec {
  val customFieldsSpec = CustomFieldsSpec().defineOptString("perimeterSecurity")
}

object ObservationWaterDamageAssessmentCustomFieldsSpec {
  val customFieldsSpec = CustomFieldsSpec().defineOptString("waterDamageAssessment")
}

// ---------------------------------------------------
// ObservationPest
// ---------------------------------------------------
object ObservationPestCustomFieldsSpec {
  val customFieldsSpec = CustomFieldsSpec().defineOptString("identification")
}

//Note: The eventId is only used during writing to the database, it is "None-ed out" after having been read from the database, to prevent it from showing up in json.
case class LifeCycleDto(eventId: Option[Long], stage: Option[String], number: Option[Int])

object LifeCycleDto {
  implicit val format = Json.format[LifeCycleDto]
}

case class ObservationPestDto(lifeCycles: Seq[LifeCycleDto]) extends Dto

object ObservationPestDto {
  implicit val format = Json.format[ObservationPestDto]
}

// ---------------------------------------------------
// ObservationAlcohol
// ---------------------------------------------------
object ObservationAlcoholCustomFieldsSpec {
  val customFieldsSpec = CustomFieldsSpec().defineOptString("condition").defineOptDouble("volume")
}
