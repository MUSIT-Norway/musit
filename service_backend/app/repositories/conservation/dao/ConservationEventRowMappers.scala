package repositories.conservation.dao

import models.conservation.events.ConservationModuleEvent
import no.uio.musit.models.{EventId, MuseumId, MusitUUID}
import no.uio.musit.repositories.events.EventRowMappers
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.time.dateTimeNow
import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json, Reads, Writes}

trait ConservationEventRowMappers extends EventRowMappers[ConservationModuleEvent] {
  self: ConservationEventTableProvider =>

  override protected def asRow(
      mid: MuseumId,
      e: ConservationModuleEvent
  )(implicit jsw: Writes[ConservationModuleEvent], currUsr: AuthenticatedUser): EventRow =
    (
      e.id,
      e.conservationTypeId,
      mid,
      e.registeredBy.getOrElse(currUsr.id),
      e.registeredDate.getOrElse(dateTimeNow),
      e.doneBy,
      e.doneDate,
      e.updatedDate,
      e.partOf,
      e.affectedThing.map(_.asString),
      e.note,
      e.caseNumbers,
      Json.toJson[ConservationModuleEvent](e)
    )
  override protected def fromRow(
      maybeEventId: Option[EventId],
      maybeDoneDate: Option[DateTime],
      maybeAffectedThing: Option[MusitUUID],
      rowAsJson: JsValue
  )(implicit jsr: Reads[ConservationModuleEvent]) =
    Json.fromJson[ConservationModuleEvent](rowAsJson).asOpt.map { row =>
      row
        .withId(maybeEventId)
        .withDoneDate(maybeDoneDate)
        .withAffectedThing(maybeAffectedThing)
        .asInstanceOf[ConservationModuleEvent]
    }

}
