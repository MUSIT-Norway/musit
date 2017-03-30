package no.uio.musit.models

import java.util.UUID

import play.api.libs.json.{JsString, Reads, Writes, __}

case class ObjectUUID(underlying: UUID) extends MusitUUID

object ObjectUUID extends MusitUUIDOps[ObjectUUID] {
  implicit val reads: Reads[ObjectUUID] =
    __.read[String].map(s => ObjectUUID(UUID.fromString(s)))

  implicit val writes: Writes[ObjectUUID] = Writes(id => JsString(id.asString))

  override implicit def fromUUID(uuid: UUID): ObjectUUID = ObjectUUID(uuid)

  override def generate() = ObjectUUID(UUID.randomUUID())
}
