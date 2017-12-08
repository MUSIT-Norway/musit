package models.conservation.events

import no.uio.musit.models.{EventId, FileId}
import play.api.libs.json.Json

case class EventDocument(
    eventId: EventId,
    fileId: FileId
)

object EventDocument {
  val tupled          = (EventDocument.apply _).tupled
  implicit val format = Json.format[EventDocument]
}
