package models.events

import java.util.UUID

import no.uio.musit.models.{MusitUUID, MusitUUIDOps}
import play.api.libs.json.{JsString, Reads, Writes, __}

case class EventTypeId(underlying: UUID) extends MusitUUID

object EventTypeId extends MusitUUIDOps[EventTypeId] {
  implicit val reads: Reads[EventTypeId] = __.read[String].map { s =>
    EventTypeId(UUID.fromString(s))
  }

  implicit val writes: Writes[EventTypeId] = Writes(id => JsString(id.asString))

  override implicit def fromUUID(uuid: UUID): EventTypeId = EventTypeId(uuid)

  /**
   * Unsafe converter from String to CollectionUUID
   */
  @throws(classOf[IllegalArgumentException]) // scalastyle:ignore
  def unsafeFromString(str: String): EventTypeId = UUID.fromString(str)

  override def generate() = EventTypeId(UUID.randomUUID())

}

case class EventType(
  typeId: EventTypeId,
  category: Category,
  shortName: Option[String] = None,
  extraAttributes: Option[Map[String, String]] = None
)

object EventType {

}