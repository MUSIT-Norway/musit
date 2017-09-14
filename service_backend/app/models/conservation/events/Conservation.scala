package models.conservation.events

import no.uio.musit.formatters.WithDateTimeFormatters
import no.uio.musit.models.ObjectTypes.ObjectType
import no.uio.musit.models._
import org.joda.time.DateTime
import play.api.libs.json._

sealed trait ConservationModuleEvent extends MusitEvent {

  val conservationTypeId: ConservationTypeId
  val partOf: Option[EventId]
  val note: Option[String]
  val caseNumbers: Option[CaseNumbers]

}
trait TypedConservationEvent {
  protected val mustBeTypeMsg = "No event type was specified"
  protected val tpe           = "type"
}

object ConservationModuleEvent extends TypedConservationEvent {

  /**
   * The implicit Reads for all events in the analysis module. It ensures that
   * the JSON message aligns with one of the types defined in the
   * AnalysisModuleEvent ADT. If not the parsing will (and should) fail.
   */
  implicit val reads: Reads[ConservationModuleEvent] = Reads { jsv =>
    (jsv \ tpe).validateOpt[String] match {
      case JsSuccess(maybeType, path) =>
        maybeType.map {
          case Preservation.discriminator =>
            Preservation.reads.reads(jsv)
          case _ =>
            ConservationEvent.reads.reads(jsv)

        }.getOrElse {
          JsError(path, mustBeTypeMsg)
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
    case ae: ConservationEvent =>
      ConservationEvent.writes.writes(ae)
    case _ => ???
  }
}

/**
 * Describes the least common denominator for conservation events. Different
 * implementations may contain more fields than this trait.
 */
trait ConservationEvent extends ConservationModuleEvent {
  val responsible: Option[ActorId]
  val administrator: Option[ActorId]
  val updatedBy: Option[ActorId]
  val updatedDate: Option[DateTime]
  val completedBy: Option[ActorId]
  val completedDate: Option[DateTime]
  val doneByActors: Option[Seq[ActorId]]
  val affectedThings: Option[Seq[ObjectUUID]]
  // todo val extraAttributes: Option[ExtraAttributes]

}

object ConservationEvent extends TypedConservationEvent with WithDateTimeFormatters {

  override protected val mustBeTypeMsg =
    "Type must be either Analysis or AnalysisCollection"

  val reads: Reads[ConservationEvent] = Reads { jsv =>
    implicit val ar = ConservationEvent.reads
    // implicit val ac = AnalysisCollection.reads

    (jsv \ tpe).validateOpt[String] match {
      case JsSuccess(maybeType, path) =>
        maybeType.map {
          case Preservation.discriminator =>
            jsv.validate[Preservation]

          /*case AnalysisCollection.discriminator =>
            jsv.validate[AnalysisCollection]
           */
          case unknown =>
            JsError(path, s"$unknown is not a valid type. $mustBeTypeMsg")

        }.getOrElse {
          JsError(path, mustBeTypeMsg)
        }

      case err: JsError =>
        err
    }
  }

  val writes: Writes[ConservationEvent] = Writes {
    case a: Preservation =>
      ConservationEvent.writes.writes(a).as[JsObject] ++
        Json.obj(tpe -> Preservation.discriminator)

    /*case c: AnalysisCollection =>
      AnalysisCollection.writes.writes(c).as[JsObject] ++
        Json.obj(tpe -> AnalysisCollection.discriminator)*/
  }
}

/**
 * Representation of typical events related to the analysis of museum objects.
 * The specific event types are encoded as {{{EventTypeId}}}.
 */
case class ConservationProcess(
    id: Option[EventId],
    conservationTypeId: ConservationTypeId,
    //caseNumbers: CaseNumbers,
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
    note: Option[String]
    // todo extraAttributes: Option[ExtraAttributes]
) extends ConservationModuleEvent {
  // These fields are not relevant for the ConservationProcess type
  override val caseNumbers: Option[CaseNumbers] = None
  //override val affectedThing: Option[ObjectUUID] = None

  override def withId(id: Option[EventId]) = copy(id = id)

  override def withAffectedThing(at: Option[MusitUUID]) = at.fold(this) {
    case oid: ObjectUUID => copy(affectedThing = Some(oid))
    case _               => this
  }

  override def withDoneDate(dd: Option[DateTime]) = copy(doneDate = dd)

}

object ConservationProcess extends WithDateTimeFormatters {
  val discriminator = "ConservationProcess"

  // The below formatters cannot be implicit due to undesirable implicit ambiguities
  val reads: Reads[ConservationProcess]   = Json.reads[ConservationProcess]
  val writes: Writes[ConservationProcess] = Json.writes[ConservationProcess]

}
