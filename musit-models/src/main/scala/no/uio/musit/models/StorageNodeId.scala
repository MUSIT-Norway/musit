package no.uio.musit.models

import java.util.UUID

import play.api.libs.json._

case class StorageNodeId(underlying: UUID) extends MusitUUID

object StorageNodeId extends MusitUUIDOps[StorageNodeId] {

  implicit val reads: Reads[StorageNodeId] =
    __.read[String].map(s => StorageNodeId(UUID.fromString(s)))

  implicit val writes: Writes[StorageNodeId] = Writes(id => JsString(id.asString))

  override implicit def fromUUID(uuid: UUID): StorageNodeId = StorageNodeId(uuid)

  override def generate(): StorageNodeId = StorageNodeId(UUID.randomUUID())

}
