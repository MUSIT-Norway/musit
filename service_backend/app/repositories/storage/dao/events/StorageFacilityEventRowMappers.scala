package repositories.storage.dao.events

import models.storage.event.StorageFacilityEvent
import no.uio.musit.models.{EventId, MuseumId, ObjectTypes}
import no.uio.musit.repositories.events.EventRowMappers
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.time.dateTimeNow
import play.api.libs.json.{JsValue, Json, Reads, Writes}

trait StorageFacilityEventRowMappers[A <: StorageFacilityEvent]
    extends EventRowMappers[A] { self: StorageEventTableProvider =>

  override protected def asRow(
      mid: MuseumId,
      e: A
  )(implicit jsw: Writes[A], currUsr: AuthenticatedUser): EventRow =
    (
      None,
      e.eventType.registeredEventId,
      mid,
      e.registeredBy.getOrElse(currUsr.id),
      e.registeredDate.getOrElse(dateTimeNow),
      e.doneDate,
      None,
      e.affectedThing.map(_.asString),
      Some(ObjectTypes.Node),
      None,
      Json.toJson[A](e)
    )

  override protected def fromRow(
      maybeEventId: Option[EventId],
      rowAsJson: JsValue
  )(implicit jsr: Reads[A]) =
    Json.fromJson[A](rowAsJson).asOpt.map(_.withId(maybeEventId))

}
