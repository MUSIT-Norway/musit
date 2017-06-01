package no.uio.musit.models

import java.util.UUID

import play.api.libs.json.{JsString, Reads, Writes, __}

case class CollectionUUID(underlying: UUID) extends MusitUUID

object CollectionUUID extends MusitUUIDOps[CollectionUUID] {
  implicit val reads: Reads[CollectionUUID] =
    __.read[String].map(s => CollectionUUID(UUID.fromString(s)))

  implicit val writes: Writes[CollectionUUID] = Writes(id => JsString(id.asString))

  override implicit def fromUUID(uuid: UUID): CollectionUUID = CollectionUUID(uuid)

  override def generate() = CollectionUUID(UUID.randomUUID())

}
