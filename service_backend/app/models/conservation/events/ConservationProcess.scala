package models.conservation.events

import no.uio.musit.formatters.WithDateTimeFormatters
import no.uio.musit.models._
import org.joda.time.DateTime
import play.api.libs.json._

sealed trait ConservationModuleEvent extends ModernMusitEvent {

  val eventTypeId: EventTypeId
  val partOf: Option[EventId]
  val note: Option[String]
  val caseNumber: Option[String]
  val actorsAndRoles: Option[Seq[ActorRoleDate]]
  val affectedThings: Option[Seq[ObjectUUID]]

  def withoutActorRoleAndDates: ConservationModuleEvent = withActorRoleAndDates(None)

  def withActorRoleAndDates(
      actorsAndRoles: Option[Seq[ActorRoleDate]]
  ): ConservationModuleEvent

}

trait TypedConservationEvent {
  protected val missingEventTypeMsg        = "No event type was specified"
  protected val discriminatorAttributeName = "eventTypeId"
}

case class EventIdWithEventTypeId(val eventId: EventId, eventTypeId: EventTypeId)

object ConservationModuleEvent extends TypedConservationEvent {

  /**
   * The implicit Reads for all events in the conservation module. It ensures that
   * the JSON message aligns with one of the types defined in
   * eventTypes in Conservation. If not the parsing will (and should) fail.
   */
  implicit val reads: Reads[ConservationModuleEvent] = Reads { jsv =>
    (jsv \ "eventTypeId").validateOpt[EventTypeId] match {
      case JsSuccess(maybeType, path) =>
        maybeType.map {
          case ConservationProcess.eventTypeId =>
            ConservationProcess.reads.reads(jsv)
          case _ =>
            ConservationEvent.reads.reads(jsv)

        }.getOrElse {
          JsError(path, missingEventTypeMsg)
        }

      case err: JsError =>
        err
    }
  }

  /**
   * Implicit Writes for analysis module events. Ensures that each type
   * is written with their specific type discriminator. This ensure that the
   * JSON message is readable on the other end.
   */
  implicit val writes: Writes[ConservationModuleEvent] = Writes {
    case cpe: ConservationProcess =>
      ConservationProcess.writes.writes(cpe).as[JsObject] ++ Json.obj(
        discriminatorAttributeName -> ConservationProcess.eventTypeId
      )

    case te: Treatment =>
      Treatment.writes.writes(te).as[JsObject] ++ Json.obj(
        discriminatorAttributeName -> Treatment.eventTypeId
      )

    case tde: TechnicalDescription =>
      TechnicalDescription.writes.writes(tde).as[JsObject] ++ Json.obj(
        discriminatorAttributeName -> TechnicalDescription.eventTypeId
      )

    case sahe: StorageAndHandling =>
      StorageAndHandling.writes.writes(sahe).as[JsObject] ++ Json.obj(
        discriminatorAttributeName -> StorageAndHandling.eventTypeId
      )

    case hsera: HseRiskAssessment =>
      HseRiskAssessment.writes.writes(hsera).as[JsObject] ++ Json.obj(
        discriminatorAttributeName -> HseRiskAssessment.eventTypeId
      )

    case ca: ConditionAssessment =>
      ConditionAssessment.writes.writes(ca).as[JsObject] ++ Json.obj(
        discriminatorAttributeName -> ConditionAssessment.eventTypeId
      )
  }
}

/**
 * Describes the least common denominator for conservation events. Different
 * implementations may contain more fields than this trait.
 */
sealed trait ConservationEvent extends ConservationModuleEvent {
  val updatedBy: Option[ActorId]
  val updatedDate: Option[DateTime]
  val completedBy: Option[ActorId]
  val completedDate: Option[DateTime]
  val actorsAndRoles: Option[Seq[ActorRoleDate]]
  val affectedThings: Option[Seq[ObjectUUID]]
  // todo val extraAttributes: Option[ExtraAttributes]

  //A new copy, appropriate when updating the event in the database.
  def withUpdatedInfo(
      updatedBy: Option[ActorId],
      updatedDate: Option[DateTime]
  ): ConservationEvent

  //A new copy, appropriate when adding/inserting the event in the database.
  def withRegisteredInfo(
      registeredBy: Option[ActorId],
      registeredDate: Option[DateTime]
  ): ConservationEvent

  def asPartOf(partOf: Option[EventId]): ConservationEvent

  def withAffectedThings(objects: Option[Seq[ObjectUUID]]): ConservationEvent

  override def withoutActorRoleAndDates: ConservationEvent = withActorRoleAndDates(None)

  def withActorRoleAndDates(actorsAndRoles: Option[Seq[ActorRoleDate]]): ConservationEvent
}

object ConservationEvent extends TypedConservationEvent with WithDateTimeFormatters {

  override protected val missingEventTypeMsg =
    "Type must be a subevent of Conservation"
  // "Type must be either Analysis or AnalysisCollection"

  val reads: Reads[ConservationEvent] = Reads { jsv =>
    implicit val _t   = Treatment.reads
    implicit val _td  = TechnicalDescription.reads
    implicit val _sah = StorageAndHandling.reads
    implicit val _hse = HseRiskAssessment.reads
    implicit val _ca  = ConditionAssessment.reads

    (jsv \ discriminatorAttributeName).validateOpt[EventTypeId] match {
      case JsSuccess(maybeType, path) =>
        maybeType.map { eventTypeId =>
          ConservationEventType(eventTypeId) match {
            case Some(eventType) =>
              eventType match {
                case Treatment =>
                  jsv.validate[Treatment]

                case TechnicalDescription =>
                  jsv.validate[TechnicalDescription]

                case StorageAndHandling =>
                  jsv.validate[StorageAndHandling]

                case HseRiskAssessment =>
                  jsv.validate[HseRiskAssessment]

                case ConditionAssessment =>
                  jsv.validate[ConditionAssessment]
              }
            case None =>
              JsError(path, s"$eventTypeId is not a valid type. $missingEventTypeMsg")
          }
        }.getOrElse {
          JsError(path, missingEventTypeMsg)
        }

      case err: JsError =>
        err
    }
  }

  val writes: Writes[ConservationEvent] = Writes {
    case pres: Treatment =>
      ConservationEvent.writes.writes(pres).as[JsObject] ++
        Json.obj(discriminatorAttributeName -> Treatment.eventTypeId)
    case prep: TechnicalDescription =>
      ConservationEvent.writes.writes(prep).as[JsObject] ++
        Json.obj(discriminatorAttributeName -> TechnicalDescription.eventTypeId)
    case sahe: StorageAndHandling =>
      ConservationEvent.writes.writes(sahe).as[JsObject] ++
        Json.obj(discriminatorAttributeName -> StorageAndHandling.eventTypeId)
    case hsera: HseRiskAssessment =>
      ConservationEvent.writes.writes(hsera).as[JsObject] ++
        Json.obj(discriminatorAttributeName -> HseRiskAssessment.eventTypeId)
    case ca: ConditionAssessment =>
      ConservationEvent.writes.writes(ca).as[JsObject] ++
        Json.obj(discriminatorAttributeName -> ConditionAssessment.eventTypeId)
  }
}

/**
 * Representation of typical events related to the analysis of museum objects.
 * The specific event types are encoded as {{{EventTypeId}}}.
 *
 *
 * Note: When you add a new subEvent type, you need to add a new ConservationSubEventType, to enable
 * the compiler to find the other places which needs modification when a new event type gets added
 */
case class ConservationProcess(
    id: Option[EventId],
    eventTypeId: EventTypeId,
    caseNumber: Option[String],
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    updatedBy: Option[ActorId],
    updatedDate: Option[DateTime],
    completedBy: Option[ActorId],
    completedDate: Option[DateTime],
    partOf: Option[EventId],
    note: Option[String],
    actorsAndRoles: Option[Seq[ActorRoleDate]],
    affectedThings: Option[Seq[ObjectUUID]],
    events: Option[Seq[ConservationEvent]]
) extends ConservationModuleEvent {
  // These fields are not relevant for the ConservationProcess type
  //override val affectedThing: Option[ObjectUUID] = None

  override def withId(id: Option[EventId]) = copy(id = id)

  def withoutEvents = copy(events = None)

  def withUpdatedInfo(
      updatedBy: Option[ActorId],
      updatedDate: Option[DateTime]
  ) = copy(updatedBy = updatedBy, updatedDate = updatedDate)

  def withRegisteredInfo(
      registeredBy: Option[ActorId],
      registeredDate: Option[DateTime]
  ) = copy(registeredBy = registeredBy, registeredDate = registeredDate)

  def withEvents(newEvents: Seq[ConservationEvent]) = copy(events = Some(newEvents))

  override def withoutActorRoleAndDates: ConservationProcess = withActorRoleAndDates(None)

  override def withActorRoleAndDates(actorsAndRoles: Option[Seq[ActorRoleDate]]) =
    copy(actorsAndRoles = actorsAndRoles)

  def withoutAfftectedThings = copy(affectedThings = None)

  def withAffectedThings(objects: Option[Seq[ObjectUUID]]): ConservationProcess =
    copy(affectedThings = objects)

}
object ConservationProcess extends WithDateTimeFormatters {
  val eventTypeId = EventTypeId(1)

  implicit val readsConsEvents: Reads[Seq[ConservationEvent]] =
    Reads.seq(ConservationEvent.reads)

  // The below formatters cannot be implicit due to undesirable implicit ambiguities
  val reads: Reads[ConservationProcess]   = Json.reads[ConservationProcess]
  val writes: Writes[ConservationProcess] = Json.writes[ConservationProcess]

}

sealed trait ConservationEventType {
  val eventTypeId: EventTypeId
}

object ConservationEventType {
  def apply(eventTypeId: EventTypeId): Option[ConservationEventType] = {
    eventTypeId match {
      case Treatment.eventTypeId            => Some(Treatment)
      case TechnicalDescription.eventTypeId => Some(TechnicalDescription)
      case StorageAndHandling.eventTypeId   => Some(StorageAndHandling)
      case HseRiskAssessment.eventTypeId    => Some(HseRiskAssessment)
      case ConditionAssessment.eventTypeId  => Some(ConditionAssessment)
      case _                                => None
    }
  }
  def mustFind(eventTypeId: EventTypeId): ConservationEventType = {
    apply(eventTypeId) match {
      case Some(t) => t
      case None =>
        throw new IllegalStateException(
          s"Unknown/unhandled eventTypeId: $eventTypeId in ConservationProcess.ConservationProcess.subEventTypeIdToConservationEventType"
        )
    }
  }
}

case class Treatment(
    id: Option[EventId],
    eventTypeId: EventTypeId,
    caseNumber: Option[String],
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    updatedBy: Option[ActorId],
    updatedDate: Option[DateTime],
    completedBy: Option[ActorId],
    completedDate: Option[DateTime],
    partOf: Option[EventId],
    note: Option[String],
    actorsAndRoles: Option[Seq[ActorRoleDate]],
    affectedThings: Option[Seq[ObjectUUID]],
    keywords: Option[Seq[Int]],
    materials: Option[Seq[Int]]
) extends ConservationEvent {

  override def withId(id: Option[EventId]) = copy(id = id)

  override def withUpdatedInfo(
      updatedBy: Option[ActorId],
      updatedDate: Option[DateTime]
  ) = copy(updatedBy = updatedBy, updatedDate = updatedDate)

  override def withRegisteredInfo(
      registeredBy: Option[ActorId],
      registeredDate: Option[DateTime]
  ) = copy(registeredBy = registeredBy, registeredDate = registeredDate)

  override def asPartOf(partOf: Option[EventId]) = copy(partOf = partOf)

  override def withAffectedThings(objects: Option[Seq[ObjectUUID]]): ConservationEvent =
    copy(affectedThings = objects)

  override def withActorRoleAndDates(actorsAndRoles: Option[Seq[ActorRoleDate]]) =
    copy(actorsAndRoles = actorsAndRoles)
}

object Treatment extends WithDateTimeFormatters with ConservationEventType {
  val eventTypeId = EventTypeId(2)

  val reads: Reads[Treatment]   = Json.reads[Treatment]
  val writes: Writes[Treatment] = Json.writes[Treatment]

}

/**
 * Representation of typical subevent related to a conservationProcess of museum objects.
 * The specific event types are encoded as {{{EventTypeId}}}.
 */
case class TechnicalDescription(
    id: Option[EventId],
    eventTypeId: EventTypeId,
    caseNumber: Option[String],
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    updatedBy: Option[ActorId],
    updatedDate: Option[DateTime],
    completedBy: Option[ActorId],
    completedDate: Option[DateTime],
    partOf: Option[EventId],
    note: Option[String],
    actorsAndRoles: Option[Seq[ActorRoleDate]],
    affectedThings: Option[Seq[ObjectUUID]]
) extends ConservationEvent {
  // These fields are not relevant for the ConservationProcess type
  //override val affectedThing: Option[ObjectUUID] = None

  override def withId(id: Option[EventId]) = copy(id = id)

  override def withUpdatedInfo(
      updatedBy: Option[ActorId],
      updatedDate: Option[DateTime]
  ) = copy(updatedBy = updatedBy, updatedDate = updatedDate)

  override def withRegisteredInfo(
      registeredBy: Option[ActorId],
      registeredDate: Option[DateTime]
  ) = copy(registeredBy = registeredBy, registeredDate = registeredDate)

  override def asPartOf(partOf: Option[EventId]) = copy(partOf = partOf)

  override def withAffectedThings(objects: Option[Seq[ObjectUUID]]): ConservationEvent =
    copy(affectedThings = objects)

  override def withActorRoleAndDates(actorsAndRoles: Option[Seq[ActorRoleDate]]) =
    copy(actorsAndRoles = actorsAndRoles)
}

object TechnicalDescription extends WithDateTimeFormatters with ConservationEventType {
  val eventTypeId = EventTypeId(3)
  // The below formatters cannot be implicit due to undesirable implicit ambiguities
  val reads: Reads[TechnicalDescription]   = Json.reads[TechnicalDescription]
  val writes: Writes[TechnicalDescription] = Json.writes[TechnicalDescription]

}

/**
 * Representation of a typical subevent related to a conservationProcess of museum objects.
 * The specific event types are encoded as {{{EventTypeId}}}.
 */
case class StorageAndHandling(
    id: Option[EventId],
    eventTypeId: EventTypeId,
    caseNumber: Option[String],
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    updatedBy: Option[ActorId],
    updatedDate: Option[DateTime],
    completedBy: Option[ActorId],
    completedDate: Option[DateTime],
    partOf: Option[EventId],
    note: Option[String],
    lightAndUvLevel: Option[String],
    relativeHumidity: Option[String],
    temperature: Option[String],
    actorsAndRoles: Option[Seq[ActorRoleDate]],
    affectedThings: Option[Seq[ObjectUUID]]
) extends ConservationEvent {
  // These fields are not relevant for the ConservationProcess type
  //override val affectedThing: Option[ObjectUUID] = None

  override def withId(id: Option[EventId]) = copy(id = id)

  override def withUpdatedInfo(
      updatedBy: Option[ActorId],
      updatedDate: Option[DateTime]
  ) = copy(updatedBy = updatedBy, updatedDate = updatedDate)

  override def withRegisteredInfo(
      registeredBy: Option[ActorId],
      registeredDate: Option[DateTime]
  ) = copy(registeredBy = registeredBy, registeredDate = registeredDate)

  override def asPartOf(partOf: Option[EventId]) = copy(partOf = partOf)

  override def withAffectedThings(objects: Option[Seq[ObjectUUID]]): ConservationEvent =
    copy(affectedThings = objects)

  override def withActorRoleAndDates(actorsAndRoles: Option[Seq[ActorRoleDate]]) =
    copy(actorsAndRoles = actorsAndRoles)
}

object StorageAndHandling extends WithDateTimeFormatters with ConservationEventType {
  val eventTypeId = EventTypeId(4)
  // The below formatters cannot be implicit due to undesirable implicit ambiguities
  val reads: Reads[StorageAndHandling]   = Json.reads[StorageAndHandling]
  val writes: Writes[StorageAndHandling] = Json.writes[StorageAndHandling]

}

/**
 * Representation of a typical subevent related to a conservationProcess of museum objects.
 * The specific event types are encoded as {{{EventTypeId}}}.
 */
case class HseRiskAssessment(
    id: Option[EventId],
    eventTypeId: EventTypeId,
    caseNumber: Option[String],
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    updatedBy: Option[ActorId],
    updatedDate: Option[DateTime],
    completedBy: Option[ActorId],
    completedDate: Option[DateTime],
    partOf: Option[EventId],
    note: Option[String],
    actorsAndRoles: Option[Seq[ActorRoleDate]],
    affectedThings: Option[Seq[ObjectUUID]]
) extends ConservationEvent {
  // These fields are not relevant for the ConservationProcess type
  //override val affectedThing: Option[ObjectUUID] = None

  override def withId(id: Option[EventId]) = copy(id = id)

  override def withUpdatedInfo(
      updatedBy: Option[ActorId],
      updatedDate: Option[DateTime]
  ) = copy(updatedBy = updatedBy, updatedDate = updatedDate)

  override def withRegisteredInfo(
      registeredBy: Option[ActorId],
      registeredDate: Option[DateTime]
  ) = copy(registeredBy = registeredBy, registeredDate = registeredDate)

  override def asPartOf(partOf: Option[EventId]) = copy(partOf = partOf)

  override def withAffectedThings(objects: Option[Seq[ObjectUUID]]): ConservationEvent =
    copy(affectedThings = objects)

  override def withActorRoleAndDates(actorsAndRoles: Option[Seq[ActorRoleDate]]) =
    copy(actorsAndRoles = actorsAndRoles)
}

object HseRiskAssessment extends WithDateTimeFormatters with ConservationEventType {
  val eventTypeId = EventTypeId(5)
  // The below formatters cannot be implicit due to undesirable implicit ambiguities
  val reads: Reads[HseRiskAssessment]   = Json.reads[HseRiskAssessment]
  val writes: Writes[HseRiskAssessment] = Json.writes[HseRiskAssessment]

}

/**
 * Representation of a typical subevent related to a conservationProcess of museum objects.
 * The specific event types are encoded as {{{EventTypeId}}}.
 */
case class ConditionAssessment(
    id: Option[EventId],
    eventTypeId: EventTypeId,
    caseNumber: Option[String],
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    updatedBy: Option[ActorId],
    updatedDate: Option[DateTime],
    completedBy: Option[ActorId],
    completedDate: Option[DateTime],
    partOf: Option[EventId],
    note: Option[String],
    conditionCode: Option[Int],
    actorsAndRoles: Option[Seq[ActorRoleDate]],
    affectedThings: Option[Seq[ObjectUUID]]
) extends ConservationEvent {
  // These fields are not relevant for the ConservationProcess type
  //override val affectedThing: Option[ObjectUUID] = None

  override def withId(id: Option[EventId]) = copy(id = id)

  override def withUpdatedInfo(
      updatedBy: Option[ActorId],
      updatedDate: Option[DateTime]
  ) = copy(updatedBy = updatedBy, updatedDate = updatedDate)

  override def withRegisteredInfo(
      registeredBy: Option[ActorId],
      registeredDate: Option[DateTime]
  ) = copy(registeredBy = registeredBy, registeredDate = registeredDate)

  override def asPartOf(partOf: Option[EventId]) = copy(partOf = partOf)

  override def withAffectedThings(objects: Option[Seq[ObjectUUID]]): ConservationEvent =
    copy(affectedThings = objects)

  override def withActorRoleAndDates(actorsAndRoles: Option[Seq[ActorRoleDate]]) =
    copy(actorsAndRoles = actorsAndRoles)
}

object ConditionAssessment extends WithDateTimeFormatters with ConservationEventType {
  val eventTypeId = EventTypeId(6)
  // The below formatters cannot be implicit due to undesirable implicit ambiguities
  val reads: Reads[ConditionAssessment]   = Json.reads[ConditionAssessment]
  val writes: Writes[ConditionAssessment] = Json.writes[ConditionAssessment]

}
