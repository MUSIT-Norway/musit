package models.conservation.events

import no.uio.musit.models.EventId
import play.api.libs.json.Json

case class EaEventMaterial(
    eventId: EventId,
    materialInfo: MaterialInfo
)

object EaEventMaterial {
  // val tupled          = (EaEventMaterial.apply _).tupled
  implicit val format = Json.format[EaEventMaterial]
}
