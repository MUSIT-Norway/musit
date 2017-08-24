package models.document

import java.util.UUID

import no.uio.musit.models.{MusitUUID, MusitUUIDOps}
import play.api.libs.json._

case class ArchiveId(underlying: UUID) extends MusitUUID

object ArchiveId extends MusitUUIDOps[ArchiveId] {
  implicit val reads: Reads[ArchiveId] =
    __.read[String].map(s => ArchiveId(UUID.fromString(s)))

  implicit val writes: Writes[ArchiveId] = Writes(id => JsString(id.asString))

  override implicit def fromUUID(uuid: UUID): ArchiveId = ArchiveId(uuid)

  implicit def fromOptUUID(ouid: Option[UUID]): Option[ArchiveId] = ouid.map(fromUUID)

  implicit def asUUID(aid: ArchiveId): UUID = aid.underlying

  implicit def optAsUUID(oid: Option[ArchiveId]): Option[UUID] = oid.map(asUUID)

  def generate(): ArchiveId = ArchiveId(UUID.randomUUID())
}
