package models.events

import models.events.AnalysisResults._
import no.uio.musit.models.{ActorId, EventId, ObjectUUID}
import no.uio.musit.security.AuthenticatedUser
import org.joda.time.DateTime
import play.api.libs.json._

/**
 * Describes the least common denominator for analysis events. Different
 * implementations may contain more fields than this trait.
 */
trait AnalysisEvent {
  val id: Option[EventId]
  val analysisTypeId: AnalysisTypeId
  val eventDate: Option[DateTime]
  val registeredBy: Option[ActorId]
  val registeredDate: Option[DateTime]
  val partOf: Option[EventId]
  val objectId: Option[ObjectUUID]
  val note: Option[String]
}

object AnalysisEvent {

  implicit val reads: Reads[AnalysisEvent] = Reads { jsv =>
    jsv.transform((__ \ "result").json.prune).flatMap(_.validate[Analysis])
  }

  implicit val writes: Writes[AnalysisEvent] = Writes {
    case a: Analysis =>
      val aa = a.copy(result = None)
      Json.format.writes(aa)

    case c: AnalysisCollection =>
      Json.format.writes(c)
  }

  def withResult(ae: AnalysisEvent, res: Option[AnalysisResult]): Analysis = {
    ae match {
      case a: Analysis => a.copy(result = res)
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
  eventDate: Option[DateTime],
  registeredBy: Option[ActorId],
  registeredDate: Option[DateTime],
  objectId: Option[ObjectUUID],
  //  actors: Option[Seq[ActorRelation]],
  partOf: Option[EventId],
  note: Option[String],
  result: Option[AnalysisResult]
) extends AnalysisEvent

object Analysis {

  implicit val format: Format[Analysis] = Json.format[Analysis]

}

/**
 * Represents a "batch" of analysis "events" of the same type.
 * Each "event" will be persisted as a separate analysis, with a reference
 * to the AnalysisCollection in the Analysis.partOf attribute.
 */
case class AnalysisCollection(
    id: Option[EventId],
    analysisTypeId: AnalysisTypeId,
    eventDate: Option[DateTime],
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    events: Seq[Analysis]
) extends AnalysisEvent {

  val partOf: Option[EventId] = None
  val objectId: Option[ObjectUUID] = None
  val note: Option[String] = None

}

object AnalysisCollection {

  implicit val writes: Writes[AnalysisCollection] = Json.writes[AnalysisCollection]

}

case class SaveAnalysisCollection(
    analysisTypeId: AnalysisTypeId,
    eventDate: Option[DateTime],
    note: Option[String],
    objectIds: Seq[ObjectUUID]
) {
  def asAnalyisCollection: AnalysisCollection = {
    AnalysisCollection(
      id = None,
      analysisTypeId = this.analysisTypeId,
      eventDate = this.eventDate,
      registeredBy = None,
      registeredDate = None,
      events = this.objectIds.map { oid =>
        Analysis(
          id = None,
          analysisTypeId = this.analysisTypeId,
          eventDate = this.eventDate,
          registeredBy = None,
          registeredDate = None,
          objectId = Option(oid),
          partOf = None,
          note = this.note,
          result = None
        )
      }
    )
  }
}

object SaveAnalysisCollection {

  implicit val reads: Reads[SaveAnalysisCollection] = Json.reads[SaveAnalysisCollection]

}
