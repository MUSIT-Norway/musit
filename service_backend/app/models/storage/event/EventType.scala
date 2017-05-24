package models.storage.event

import play.api.data.validation.ValidationError
import play.api.libs.json._

case class EventType(name: String) extends AnyVal {

  def registeredEventId: EventTypeId =
    EventTypeRegistry.withNameInsensitive(name).id

}

object EventType {

  def fromEventTypeId(id: EventTypeId): EventType =
    EventType(EventTypeRegistry.unsafeFromId(id).name)

  def fromInt(i: Int): EventType = fromEventTypeId(EventTypeId(i))

  implicit val reads: Reads[EventType] =
    __.read[String]
      .filter(ValidationError("Unsupported event type")) { et =>
        EventTypeRegistry.withNameInsensitiveOption(et).isDefined
      }
      .map(EventType.apply)

  implicit val writes: Writes[EventType] = Writes { et =>
    JsString(et.name)
  }

}
