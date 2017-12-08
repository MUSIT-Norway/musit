package no.uio.musit.models

import java.util.UUID

import play.api.libs.json.{JsString, Reads, Writes, __}

case class FileId(underlying: UUID) extends MusitUUID

object FileId extends MusitUUIDOps[FileId] {
  implicit val reads: Reads[FileId] =
    __.read[String].map(s => FileId(UUID.fromString(s)))

  implicit val writes: Writes[FileId] = Writes(id => JsString(id.asString))

  override implicit def fromUUID(uuid: UUID): FileId = FileId(uuid)

  override def generate() = FileId(UUID.randomUUID())
}
