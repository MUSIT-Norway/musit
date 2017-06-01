package no.uio.musit.models

import java.util.UUID

import play.api.libs.json.{JsString, Writes, _}

case class GroupId(underlying: UUID) extends MusitUUID

object GroupId extends MusitUUIDOps[GroupId] {
  implicit val reads: Reads[GroupId] =
    __.read[String].map(s => GroupId(UUID.fromString(s)))

  implicit val writes: Writes[GroupId] = Writes(id => JsString(id.asString))

  override implicit def fromUUID(uuid: UUID): GroupId = GroupId(uuid)

  override def generate(): GroupId = GroupId(UUID.randomUUID())
}
