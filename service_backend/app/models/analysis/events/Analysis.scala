package models.analysis.events

import models.analysis.AnalysisStatuses.AnalysisStatus
import models.analysis.events.AnalysisExtras._
import models.analysis.events.AnalysisResults._
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess, MusitValidationError}
import no.uio.musit.formatters.WithDateTimeFormatters
import no.uio.musit.models.ObjectTypes.ObjectType
import no.uio.musit.models.{ActorId, CaseNumbers, EventId, ObjectUUID}
import org.joda.time.DateTime
import play.api.libs.json._

import scala.concurrent.Future
import scala.reflect.ClassTag

sealed trait AnalysisModuleEvent {
  val id: Option[EventId]
  val analysisTypeId: AnalysisTypeId
  val registeredBy: Option[ActorId]
  val registeredDate: Option[DateTime]
  val doneBy: Option[ActorId]
  val doneDate: Option[DateTime]
  val objectId: Option[ObjectUUID]
  val partOf: Option[EventId]
  val note: Option[String]
  val status: Option[AnalysisStatus]
  val caseNumbers: Option[CaseNumbers]
}

trait TypedAnalysisEvent {
  protected val mustBeTypeMsg = "No event type was specified"
  protected val tpe           = "type"
}

object AnalysisModuleEvent extends TypedAnalysisEvent {

  /**
   * The implicit Reads for all events in the analysis module. It ensures that
   * the JSON message aligns with one of the types defined in the
   * AnalysisModuleEvent ADT. If not the parsing will (and should) fail.
   */
  implicit val reads: Reads[AnalysisModuleEvent] = Reads { jsv =>
    (jsv \ tpe).validateOpt[String] match {
      case JsSuccess(maybeType, path) =>
        maybeType.map {
          case SampleCreated.discriminator =>
            SampleCreated.format.reads(jsv)

          case _ =>
            AnalysisEvent.reads.reads(jsv)

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
  implicit val writes: Writes[AnalysisModuleEvent] = Writes {
    case ae: AnalysisEvent =>
      AnalysisEvent.writes.writes(ae)

    case sc: SampleCreated =>
      SampleCreated.format.writes(sc).as[JsObject] ++
        Json.obj(tpe -> SampleCreated.discriminator)
  }
}

/**
 * Describes the least common denominator for analysis events. Different
 * implementations may contain more fields than this trait.
 */
sealed trait AnalysisEvent extends AnalysisModuleEvent {
  val responsible: Option[ActorId]
  val administrator: Option[ActorId]
  val updatedBy: Option[ActorId]
  val updatedDate: Option[DateTime]
  val completedBy: Option[ActorId]
  val completedDate: Option[DateTime]
  val reason: Option[String]
  val extraAttributes: Option[ExtraAttributes]
  val result: Option[AnalysisResult]

  /**
   * Returns an AnalysisEvent that contains a result.
   */
  def withResult(res: Option[AnalysisResult]): AnalysisEvent = {
    this match {
      case a: Analysis            => a.copy(result = res)
      case ac: AnalysisCollection => ac.copy(result = res)
    }
  }

  /**
   * Returns an Option of an AnalysisEvent with a result. If the AnalysisEvent
   * type is Analysis, the Option will be Some(Analysis), otherwise None
   */
  def withResultAsOpt[A <: AnalysisEvent: ClassTag](
      res: Option[AnalysisResult]
  ): Option[A] =
    this match {
      case a: Analysis            => Option(a.copy(result = res).asInstanceOf[A])
      case ac: AnalysisCollection => Option(ac.copy(result = res).asInstanceOf[A])
    }
}

object AnalysisEvent extends TypedAnalysisEvent with WithDateTimeFormatters {

  override protected val mustBeTypeMsg =
    "Type must be either Analysis or AnalysisCollection"

  val reads: Reads[AnalysisEvent] = Reads { jsv =>
    implicit val ar = Analysis.reads
    implicit val ac = AnalysisCollection.reads

    (jsv \ tpe).validateOpt[String] match {
      case JsSuccess(maybeType, path) =>
        maybeType.map {
          case Analysis.discriminator =>
            jsv.validate[Analysis]

          case AnalysisCollection.discriminator =>
            jsv.validate[AnalysisCollection]

          case unknown =>
            JsError(path, s"$unknown is not a valid type. $mustBeTypeMsg")

        }.getOrElse {
          JsError(path, mustBeTypeMsg)
        }

      case err: JsError =>
        err
    }
  }

  val writes: Writes[AnalysisEvent] = Writes {
    case a: Analysis =>
      Analysis.writes.writes(a).as[JsObject] ++
        Json.obj(tpe -> Analysis.discriminator)

    case c: AnalysisCollection =>
      AnalysisCollection.writes.writes(c).as[JsObject] ++
        Json.obj(tpe -> AnalysisCollection.discriminator)
  }

  /**
   * Function for validating that an AnalysisCollection and all its children
   * are valid with respect to the analysis type specified.
   */
  def validateAttributes(
      ac: AnalysisCollection,
      mat: Option[AnalysisType]
  ): Future[MusitResult[AnalysisCollection]] = {
    mat.map { t =>
      Future.successful {
        for {
          validCol      <- validate(ac, t)
          validChildren <- validateAll(ac.events, t)
        } yield validCol
      }
    }.getOrElse {
      val msg = s"Invalid analysis type ID ${ac.analysisTypeId}"
      Future.successful(MusitValidationError(msg))
    }
  }

  /**
   * Validates the given Analysis against the AnalysisType to see if it complies
   * with the extra attributes for the given type.
   */
  def validateAttributes(
      a: Analysis,
      mat: Option[AnalysisType]
  ): Future[MusitResult[Analysis]] = {
    mat.map(t => Future.successful(validate(a, t))).getOrElse {
      val msg = s"Invalid analysis type ID ${a.analysisTypeId}"
      Future.successful(MusitValidationError(msg))
    }
  }

  /**
   * Validation function that checks a collection of analyses valid extra attributes.
   */
  private[this] def validateAll(
      as: Seq[Analysis],
      t: AnalysisType
  ): MusitResult[Seq[Analysis]] = {
    val errors = as.map(a => validate(a, t)).filter(_.isFailure)

    if (errors.nonEmpty) {
      MusitValidationError(
        s"One or more analyses contained invalid attributes" +
          s"for analysis type ${t.id}"
      )
    } else {
      MusitSuccess(as)
    }
  }

  /**
   * Validation function that checks an analysis for valid extra attributes.
   */
  private[this] def validate[A <: AnalysisEvent](
      a: A,
      t: AnalysisType
  ): MusitResult[A] = {
    val isValid = a.extraAttributes.forall {
      case MicroscopyAttributes(_) =>
        t.extraDescriptionType.contains(MicroscopyAttributes.typeName)

      case TomographyAttributes(_) =>
        t.extraDescriptionType.contains(TomographyAttributes.typeName)

      case IsotopeAttributes(_) =>
        t.extraDescriptionType.contains(IsotopeAttributes.typeName)

      case ElementalAASAttributes(_) =>
        t.extraDescriptionType.contains(ElementalAASAttributes.typeName)

      case ElementalICPAttributes(_) =>
        t.extraDescriptionType.contains(ElementalICPAttributes.typeName)

      case ExtractionAttributes(_, _) =>
        t.extraDescriptionType.contains(ExtractionAttributes.typeName)
    }

    if (isValid) {
      MusitSuccess(a)
    } else {
      val errMsg = s"Invalid additional attributes for analysis type ${t.id}"
      MusitValidationError(errMsg)
    }
  }

}

/**
 * Representation of typical events related to the analysis of museum objects.
 * The specific event types are encoded as {{{EventTypeId}}}.
 */
case class Analysis(
    id: Option[EventId],
    analysisTypeId: AnalysisTypeId,
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
    objectId: Option[ObjectUUID],
    objectType: Option[ObjectType],
    partOf: Option[EventId],
    note: Option[String],
    extraAttributes: Option[ExtraAttributes],
    result: Option[AnalysisResult]
) extends AnalysisEvent {
  val reason: Option[String]           = None
  val status: Option[AnalysisStatus]   = None
  val caseNumbers: Option[CaseNumbers] = None
}

object Analysis extends WithDateTimeFormatters {
  val discriminator = "Analysis"

  // The below formatters cannot be implicit due to undesirable implicit ambiguities
  val reads: Reads[Analysis]   = Json.reads[Analysis]
  val writes: Writes[Analysis] = Json.writes[Analysis]

}

/**
 * Represents a "batch" of analysis "events" of the same type.
 * Each "event" will be persisted as a separate analysis, with a reference
 * to the AnalysisCollection in the Analysis.partOf attribute.
 */
case class AnalysisCollection(
    id: Option[EventId],
    analysisTypeId: AnalysisTypeId,
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
    note: Option[String],
    extraAttributes: Option[ExtraAttributes],
    result: Option[AnalysisResult],
    events: Seq[Analysis],
    restriction: Option[Restriction],
    reason: Option[String],
    status: Option[AnalysisStatus],
    caseNumbers: Option[CaseNumbers]
) extends AnalysisEvent {

  val partOf: Option[EventId]      = None
  val objectId: Option[ObjectUUID] = None

  def withoutChildren = copy(events = Seq.empty)

}

object AnalysisCollection extends WithDateTimeFormatters {
  val discriminator = "AnalysisCollection"

  // The below formatters cannot be implicit due to undesirable implicit ambiguities
  def reads(implicit r: Reads[Analysis]): Reads[AnalysisCollection] =
    Json.reads[AnalysisCollection]

  def writes(implicit w: Writes[Analysis]): Writes[AnalysisCollection] =
    Json.writes[AnalysisCollection]

}

case class SampleCreated(
    id: Option[EventId],
    doneBy: Option[ActorId],
    doneDate: Option[DateTime],
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    objectId: Option[ObjectUUID],
    sampleObjectId: Option[ObjectUUID],
    externalLinks: Option[Seq[String]]
) extends AnalysisModuleEvent {
  val partOf         = None
  val analysisTypeId = SampleCreated.sampleEventTypeId
  val note           = None
  val status         = None
  val caseNumbers    = None
}

object SampleCreated extends WithDateTimeFormatters {
  val sampleEventTypeId: AnalysisTypeId = AnalysisTypeId(0L)

  val discriminator = "SampleCreated"

  val format: Format[SampleCreated] = Json.format[SampleCreated]

}
