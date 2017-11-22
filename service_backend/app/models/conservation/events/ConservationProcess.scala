package models.conservation.events

import no.uio.musit.formatters.WithDateTimeFormatters
import no.uio.musit.models._
import org.joda.time.DateTime
import play.api.libs.json._

sealed trait ConservationModuleEvent extends MusitEvent {

  val eventTypeId: EventTypeId
  val partOf: Option[EventId]
  val note: Option[String]
  val caseNumber: Option[String]
  //val doneByActors: Option[Seq[ActorId]]
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
   * The implicit Reads for all events in the analysis module. It ensures that
   * the JSON message aligns with one of the types defined in the
   * AnalysisModuleEvent ADT. If not the parsing will (and should) fail.
   */
  implicit val reads: Reads[ConservationModuleEvent] = Reads { jsv =>
    (jsv \ "eventTypeId").validateOpt[EventTypeId] match {
      case JsSuccess(maybeType, path) =>
        maybeType.map {
          /*  case Preservation =>
            Preservation.reads.reads(jsv)*/
          case ConservationProcess.eventTypeId =>
            ConservationProcess.reads.reads(jsv)
          case TechnicalDescription.eventTypeId =>
            TechnicalDescription.reads.reads(jsv)
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
  //val doneByActors: Option[Seq[ActorId],
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
    "Type must be either Analysis or AnalysisCollection"

  val reads: Reads[ConservationEvent] = Reads { jsv =>
    // implicit val ar = ConservationEvent.reads
    // implicit val ac = AnalysisCollection.reads
    implicit val _t  = Treatment.reads
    implicit val _td = TechnicalDescription.reads

    (jsv \ discriminatorAttributeName).validateOpt[EventTypeId] match {
      case JsSuccess(maybeType, path) =>
        maybeType.map {
          case Treatment.eventTypeId =>
            jsv.validate[Treatment]

          case TechnicalDescription.eventTypeId =>
            jsv.validate[TechnicalDescription]

          case unknown =>
            JsError(path, s"$unknown is not a valid type. $missingEventTypeMsg")

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
    doneBy: Option[ActorId],
    doneDate: Option[DateTime],
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    //responsible: Option[ActorId],
    //administrator: Option[ActorId],
    updatedBy: Option[ActorId],
    updatedDate: Option[DateTime],
    completedBy: Option[ActorId],
    completedDate: Option[DateTime],
    affectedThing: Option[ObjectUUID],
    partOf: Option[EventId],
    note: Option[String],
    //doneByActors: Option[Seq[ActorId]],
    actorsAndRoles: Option[Seq[ActorRoleDate]],
    affectedThings: Option[Seq[ObjectUUID]],
    events: Option[Seq[ConservationEvent]]
) extends ConservationModuleEvent {
  // These fields are not relevant for the ConservationProcess type
  //override val affectedThing: Option[ObjectUUID] = None

  override def withId(id: Option[EventId]) = copy(id = id)

  override def withAffectedThing(at: Option[MusitUUID]) = at.fold(this) {
    case oid: ObjectUUID => copy(affectedThing = Some(oid))
    case _               => this
  }

  override def withDoneDate(dd: Option[DateTime]) = copy(doneDate = dd)

  def withoutChildren = copy(events = None)

  def withUpdatedInfo(
      updatedBy: Option[ActorId],
      updatedDate: Option[DateTime]
  ) = copy(updatedBy = updatedBy, updatedDate = updatedDate)

  def withRegisteredInfo(
      registeredBy: Option[ActorId],
      registeredDate: Option[DateTime]
  ) = copy(registeredBy = registeredBy, registeredDate = registeredDate)

  def withEvents(newEvents: Seq[ConservationEvent]) = copy(events = Some(newEvents))

  override def withActorRoleAndDates(actorsAndRoles: Option[Seq[ActorRoleDate]]) =
    copy(actorsAndRoles = actorsAndRoles)

}
object ConservationProcess extends WithDateTimeFormatters {
  val eventTypeId = EventTypeId(1)

  implicit val readsConsEvents: Reads[Seq[ConservationEvent]] =
    Reads.seq(ConservationEvent.reads)
  //val writesConsEvent = ConservationEvent.writes
  //implicit val writesConsEvents: Writes[Seq[ConservationEvent]] = Writes.seq(writesConsEvent)

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
      case _                                => None
    }
  }
  def mustFind(eventTypeId: EventTypeId): ConservationEventType = {
    apply(eventTypeId) match {
      case Some(t) => t
      case None =>
        throw new IllegalStateException(
          s"Unhandled eventTypeId: $eventTypeId in ConservationProcess.ConservationProcess.subEventTypeIdToConservationEventType"
        )
    }
  }
}

case class Treatment(
    id: Option[EventId],
    eventTypeId: EventTypeId,
    caseNumber: Option[String],
    doneBy: Option[ActorId],
    doneDate: Option[DateTime],
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    //responsible: Option[ActorId],
    //administrator: Option[ActorId],
    updatedBy: Option[ActorId],
    updatedDate: Option[DateTime],
    completedBy: Option[ActorId],
    completedDate: Option[DateTime],
    affectedThing: Option[ObjectUUID],
    partOf: Option[EventId],
    note: Option[String],
    // doneByActors: Option[Seq[ActorId]],
    actorsAndRoles: Option[Seq[ActorRoleDate]],
    affectedThings: Option[Seq[ObjectUUID]],
    keywords: Option[Seq[Int]],
    materials: Option[Seq[Int]]
) extends ConservationEvent {

  override def withId(id: Option[EventId]) = copy(id = id)

  override def withAffectedThing(at: Option[MusitUUID]) = at.fold(this) {
    case oid: ObjectUUID => copy(affectedThing = Some(oid))
    case _               => this
  }

  override def withDoneDate(dd: Option[DateTime]) = copy(doneDate = dd)

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
 * Representation of typical events related to the analysis of museum objects.
 * The specific event types are encoded as {{{EventTypeId}}}.
 */
case class TechnicalDescription(
    id: Option[EventId],
    eventTypeId: EventTypeId,
    caseNumber: Option[String],
    doneBy: Option[ActorId],
    doneDate: Option[DateTime],
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    //responsible: Option[ActorId],
    //administrator: Option[ActorId],
    updatedBy: Option[ActorId],
    updatedDate: Option[DateTime],
    completedBy: Option[ActorId],
    completedDate: Option[DateTime],
    affectedThing: Option[ObjectUUID],
    partOf: Option[EventId],
    note: Option[String],
    //doneByActors: Option[Seq[ActorId]],
    actorsAndRoles: Option[Seq[ActorRoleDate]],
    affectedThings: Option[Seq[ObjectUUID]]
) extends ConservationEvent {
  // These fields are not relevant for the ConservationProcess type
  //override val affectedThing: Option[ObjectUUID] = None

  override def withId(id: Option[EventId]) = copy(id = id)

  override def withAffectedThing(at: Option[MusitUUID]) = at.fold(this) {
    case oid: ObjectUUID => copy(affectedThing = Some(oid))
    case _               => this
  }

  override def withDoneDate(dd: Option[DateTime]) = copy(doneDate = dd)

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
