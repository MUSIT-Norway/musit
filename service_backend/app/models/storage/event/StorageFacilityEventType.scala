package models.storage.event

import no.uio.musit.models.EventTypeId
import play.api.libs.json._

case class StorageFacilityEventType(name: String) extends AnyVal {

  def registeredEventId: EventTypeId =
    EventTypeRegistry.withNameInsensitive(name).id

}

object StorageFacilityEventType {

  def fromEventTypeId(id: EventTypeId): StorageFacilityEventType =
    StorageFacilityEventType(EventTypeRegistry.unsafeFromId(id).name)

  def fromInt(i: Int): StorageFacilityEventType = fromEventTypeId(EventTypeId(i))

  implicit val reads: Reads[StorageFacilityEventType] =
    __.read[String]
      .filter(JsonValidationError("Unsupported event type")) { et =>
        EventTypeRegistry.withNameInsensitiveOption(et).isDefined
      }
      .map(StorageFacilityEventType.apply)

  implicit val writes: Writes[StorageFacilityEventType] = Writes { et =>
    JsString(et.name)
  }

}
