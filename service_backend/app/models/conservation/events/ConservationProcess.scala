package models.conservation.events

import no.uio.musit.formatters.WithDateTimeFormatters
import no.uio.musit.models.ObjectTypes.ObjectType
import no.uio.musit.models._
import org.joda.time.DateTime
import play.api.libs.json._

sealed trait ConservationModuleEvent extends MusitEvent {

  val eventTypeId: EventTypeId
  val partOf: Option[EventId]
  val note: Option[String]
  val caseNumber: Option[String]
  val doneByActors: Option[Seq[ActorId]]
  val affectedThings: Option[Seq[ObjectUUID]]

}
trait TypedConservationEvent {
  protected val missingEventTypeMsg        = "No event type was specified"
  protected val discriminatorAttributeName = "eventTypeId"
}

object ConservationModuleEvent extends TypedConservationEvent {

  /**
   * The implicit Reads for all events in the analysis module. It ensures that
   * the JSON message aligns with one of the types defined in the
   * AnalysisModuleEvent ADT. If not the parsing will (and should) fail.
   */
  implicit val reads: Reads[ConservationModuleEvent] = Reads { jsv =>
    (jsv \ "eventTypeId").validateOpt[Int] match {
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
    case ce: ConservationEvent =>
      ConservationEvent.writes.writes(ce)
    case cpe: ConservationProcess =>
      //ConservationProcess.writes.writes(cpe)
      ConservationProcess.writes.writes(cpe).as[JsObject] ++ Json.obj(
        discriminatorAttributeName -> ConservationProcess.eventTypeId
      )
    case tde: TechnicalDescription =>
      //ConservationProcess.writes.writes(cpe)
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
  val doneByActors: Option[Seq[ActorId]]
  val affectedThings: Option[Seq[ObjectUUID]]
  // todo val extraAttributes: Option[ExtraAttributes]

}

object ConservationEvent extends TypedConservationEvent with WithDateTimeFormatters {

  override protected val missingEventTypeMsg =
    "Type must be either Analysis or AnalysisCollection"

  val reads: Reads[ConservationEvent] = Reads { jsv =>
    implicit val ar = ConservationEvent.reads
    // implicit val ac = AnalysisCollection.reads

    (jsv \ discriminatorAttributeName).validateOpt[Int] match {
      case JsSuccess(maybeType, path) =>
        maybeType.map {
          case Preservation.eventTypeId =>
            jsv.validate[Preservation]

          case Preparation.eventTypeId =>
            jsv.validate[Preparation]
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
    case pres: Preservation =>
      ConservationEvent.writes.writes(pres).as[JsObject] ++
        Json.obj(discriminatorAttributeName -> Preservation.eventTypeId)
    case prep: Preparation =>
      ConservationEvent.writes.writes(prep).as[JsObject] ++
        Json.obj(discriminatorAttributeName -> Preparation.eventTypeId)

  }
}

/**
 * Representation of typical events related to the analysis of museum objects.
 * The specific event types are encoded as {{{EventTypeId}}}.
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
    doneByActors: Option[Seq[ActorId]],
    affectedThings: Option[Seq[ObjectUUID]]
    // todo extraAttributes: Option[ExtraAttributes]
) extends ConservationModuleEvent {
  // These fields are not relevant for the ConservationProcess type
  //override val affectedThing: Option[ObjectUUID] = None

  override def withId(id: Option[EventId]) = copy(id = id)

  override def withAffectedThing(at: Option[MusitUUID]) = at.fold(this) {
    case oid: ObjectUUID => copy(affectedThing = Some(oid))
    case _               => this
  }

  override def withDoneDate(dd: Option[DateTime]) = copy(doneDate = dd)

}

object ConservationProcess extends WithDateTimeFormatters {
  val eventTypeId = 1
  // The below formatters cannot be implicit due to undesirable implicit ambiguities
  val reads: Reads[ConservationProcess]   = Json.reads[ConservationProcess]
  val writes: Writes[ConservationProcess] = Json.writes[ConservationProcess]

}

case class Preservation(
    id: Option[EventId],
    eventTypeId: EventTypeId,
    caseNumber: Option[String],
    doneBy: Option[ActorId],
    doneDate: Option[DateTime],
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    responsible: Option[ActorId],
    administrator: Option[ActorId],
    updatedBy: Option[ActorId],
    updatedDate: Option[DateTime],
    completedBy: Option[ActorId],
    completedDate: Option[DateTime],
    affectedThing: Option[ObjectUUID],
    affectedType: Option[ObjectType],
    partOf: Option[EventId],
    note: Option[String],
    doneByActors: Option[Seq[ActorId]],
    affectedThings: Option[Seq[ObjectUUID]]
    // todo extraAttributes: Option[ExtraAttributes]
) extends ConservationEvent {

  override def withId(id: Option[EventId]) = copy(id = id)

  override def withAffectedThing(at: Option[MusitUUID]) = at.fold(this) {
    case oid: ObjectUUID => copy(affectedThing = Some(oid))
    case _               => this
  }

  override def withDoneDate(dd: Option[DateTime]) = copy(doneDate = dd)

}

object Preservation extends WithDateTimeFormatters {
  val eventTypeId = 2

  implicit val reads: Reads[Preservation]   = Json.reads[Preservation]
  implicit val writes: Writes[Preservation] = Json.writes[Preservation]

}

case class Preparation(
    id: Option[EventId],
    eventTypeId: EventTypeId,
    caseNumber: Option[String],
    doneBy: Option[ActorId],
    doneDate: Option[DateTime],
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    responsible: Option[ActorId],
    administrator: Option[ActorId],
    updatedBy: Option[ActorId],
    updatedDate: Option[DateTime],
    completedBy: Option[ActorId],
    completedDate: Option[DateTime],
    affectedThing: Option[ObjectUUID],
    affectedType: Option[ObjectType],
    partOf: Option[EventId],
    note: Option[String],
    doneByActors: Option[Seq[ActorId]],
    affectedThings: Option[Seq[ObjectUUID]]
    // todo extraAttributes: Option[ExtraAttributes]
) extends ConservationEvent {

  override def withId(id: Option[EventId]) = copy(id = id)

  override def withAffectedThing(at: Option[MusitUUID]) = at.fold(this) {
    case oid: ObjectUUID => copy(affectedThing = Some(oid))
    case _               => this
  }

  override def withDoneDate(dd: Option[DateTime]) = copy(doneDate = dd)

}

object Preparation extends WithDateTimeFormatters {
  val eventTypeId = 3

  implicit val reads: Reads[Preparation]   = Json.reads[Preparation]
  implicit val writes: Writes[Preparation] = Json.writes[Preparation]

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
    doneByActors: Option[Seq[ActorId]],
    affectedThings: Option[Seq[ObjectUUID]]
    // todo extraAttributes: Option[ExtraAttributes]
) extends ConservationModuleEvent {
  // These fields are not relevant for the ConservationProcess type
  //override val affectedThing: Option[ObjectUUID] = None

  override def withId(id: Option[EventId]) = copy(id = id)

  override def withAffectedThing(at: Option[MusitUUID]) = at.fold(this) {
    case oid: ObjectUUID => copy(affectedThing = Some(oid))
    case _               => this
  }

  override def withDoneDate(dd: Option[DateTime]) = copy(doneDate = dd)

}

object TechnicalDescription extends WithDateTimeFormatters {
  val eventTypeId = 4
  // The below formatters cannot be implicit due to undesirable implicit ambiguities
  implicit val reads: Reads[TechnicalDescription]   = Json.reads[TechnicalDescription]
  implicit val writes: Writes[TechnicalDescription] = Json.writes[TechnicalDescription]

}
