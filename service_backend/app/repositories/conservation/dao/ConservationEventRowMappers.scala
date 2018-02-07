package repositories.conservation.dao

import models.conservation.events.{
  ConservationEvent,
  ConservationModuleEvent,
  ConservationProcess
}
import no.uio.musit.MusitResults.{MusitInternalError, MusitResult, MusitSuccess}
import no.uio.musit.models.{EventId, MuseumId, MusitEvent, MusitUUID}
import no.uio.musit.repositories.events.EventRowMappers
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.time.dateTimeNow
import org.joda.time.DateTime
import play.api.libs.json._

trait ConservationEventRowMappers extends EventRowMappers[ConservationModuleEvent] {
  self: ConservationEventTableProvider =>

  override protected def asRow(
      mid: MuseumId,
      e: ConservationModuleEvent
  )(
      implicit jsw: Writes[ConservationModuleEvent],
      currUsr: AuthenticatedUser
  ): EventRow = {
    val js = Json.toJson[ConservationModuleEvent](e)

    require(e.registeredDate.isDefined)
    require(e.registeredBy.isDefined)

    val row = (
      e.id,
      e.eventTypeId,
      mid,
      e.registeredBy.get,
      e.registeredDate.get,
      e.updatedDate,
      e.partOf,
      e.note,
      if (e.eventTypeId == ConservationProcess.eventTypeId) e.caseNumber else None,
      e.updatedBy,
      js
    )
    row
  }

  protected def fromConservationRow(
      maybeEventId: Option[EventId],
      rowAsJson: JsValue
  )(
      implicit jsr: Reads[ConservationModuleEvent]
  ): MusitResult[ConservationModuleEvent] = {
    //println(Json.prettyPrint(rowAsJson))
    require(maybeEventId.isDefined)
    val eventFromJson = Json.fromJson[ConservationModuleEvent](rowAsJson)
    eventFromJson match {
      case JsSuccess(event, path) =>
        MusitSuccess(event.withId(maybeEventId).asInstanceOf[ConservationModuleEvent])

      case JsError(err) =>
        MusitInternalError(
          s"Problems parsing json in fromConservationRow: $rowAsJson, errorMsg: $err"
        )

    }
    /*
    Json.fromJson[ConservationModuleEvent](rowAsJson).asOpt.map { row =>
      row.withId(maybeEventId).asInstanceOf[ConservationModuleEvent]
    }
   */
  }

  override protected def fromRow(
      maybeEventId: Option[EventId],
      maybeDoneDate: Option[DateTime],
      maybeAffectedThing: Option[MusitUUID],
      rowAsJson: JsValue
  )(implicit jsr: Reads[ConservationModuleEvent]) = {
    throw new IllegalStateException(
      "Internal error, ConservationEventRowMappers.fromRow is not supposed to be called anymore"
    )
  }
}
