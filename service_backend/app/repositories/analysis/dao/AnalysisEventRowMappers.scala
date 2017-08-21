package repositories.analysis.dao

import models.analysis.events.AnalysisResults.AnalysisResult
import models.analysis.events.{Analysis, AnalysisCollection, AnalysisModuleEvent}
import no.uio.musit.repositories.events.EventRowMappers
import no.uio.musit.models.{EventId, MuseumId, MusitUUID}
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.time.dateTimeNow
import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json, Reads, Writes}

trait AnalysisEventRowMappers extends EventRowMappers[AnalysisModuleEvent] {
  self: AnalysisEventTableProvider =>

  override protected def asRow(
      mid: MuseumId,
      e: AnalysisModuleEvent
  )(implicit jsw: Writes[AnalysisModuleEvent], currUsr: AuthenticatedUser): EventRow =
    (
      e.id,
      e.analysisTypeId,
      mid,
      e.registeredBy.getOrElse(currUsr.id),
      e.registeredDate.getOrElse(dateTimeNow),
      e.doneBy,
      e.doneDate,
      e.updatedDate,
      e.partOf,
      e.affectedThing.map(_.asString),
      e.note,
      e.status,
      e.caseNumbers,
      Json.toJson[AnalysisModuleEvent](e)
    )

  override protected def fromRow(
      maybeEventId: Option[EventId],
      maybeDoneDate: Option[DateTime],
      maybeAffectedThing: Option[MusitUUID],
      rowAsJson: JsValue
  )(implicit jsr: Reads[AnalysisModuleEvent]) =
    Json.fromJson[AnalysisModuleEvent](rowAsJson).asOpt.map { row =>
      row
        .withId(maybeEventId)
        .withDoneDate(maybeDoneDate)
        .withAffectedThing(maybeAffectedThing)
        .asInstanceOf[AnalysisModuleEvent]
    }

  protected def toAnalysis(
      maybeEventId: Option[EventId],
      maybeDoneDate: Option[DateTime],
      maybeAffectedThing: Option[MusitUUID],
      rowAsJson: JsValue
  ): Option[Analysis] =
    fromRow(maybeEventId, maybeDoneDate, maybeAffectedThing, rowAsJson).flatMap {
      case a: Analysis => Some(a)
      case _           => None
    }

  protected def toAnalysisCollection(
      maybeEventId: Option[EventId],
      maybeDoneDate: Option[DateTime],
      maybeAffectedThing: Option[MusitUUID],
      rowAsJson: JsValue
  ): Option[AnalysisCollection] =
    fromRow(maybeEventId, maybeDoneDate, maybeAffectedThing, rowAsJson).flatMap {
      case ac: AnalysisCollection => Some(ac)
      case _                      => None
    }

  protected def asResultRow(
      mid: MuseumId,
      eid: EventId,
      res: AnalysisResult
  ): ResultRow = {
    (
      eid,
      mid,
      res.registeredBy,
      res.registeredDate,
      Json.toJson[AnalysisResult](res)
    )
  }

  /**
   * Converts a ResultRow tuple into an instance of an AnalysisResult
   *
   * @param tuple the ResultRow tuple to convert
   * @return an Option of the corresponding AnalysisResult
   */
  protected def fromResultRow(tuple: ResultRow): Option[AnalysisResult] =
    Json.fromJson[AnalysisResult](tuple._5).asOpt

  protected def fromResultRowOpt(
      maybeTuple: Option[ResultRow]
  ): Option[AnalysisResult] =
    maybeTuple.flatMap(fromResultRow)

}
