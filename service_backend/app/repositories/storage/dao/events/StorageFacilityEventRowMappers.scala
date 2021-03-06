package repositories.storage.dao.events

import models.storage.event.StorageFacilityEvent
import no.uio.musit.models.{MuseumId, ObjectTypes}
import no.uio.musit.repositories.events.EventRowMappers
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.time.dateTimeNow
import play.api.libs.json.{Json, Writes}

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
      None,
      e.affectedThing.map(_.asString),
      Some(ObjectTypes.Node),
      None,
      Json.toJson[A](e)
    )

}
