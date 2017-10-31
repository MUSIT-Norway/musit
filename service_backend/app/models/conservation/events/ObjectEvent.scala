package models.conservation.events

import no.uio.musit.models.{EventId, ObjectUUID}
import play.api.libs.json.Json

case class ObjectEvent(
    objectUuid: ObjectUUID,
    eventId: EventId
)

object ObjectEvent {
  val tupled          = (ObjectEvent.apply _).tupled
  implicit val format = Json.format[ObjectEvent]
}
