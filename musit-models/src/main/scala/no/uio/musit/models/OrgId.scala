package no.uio.musit.models

import play.api.libs.json.{JsNumber, Reads, Writes, _}

case class OrgId(underlying: Long) extends MusitId

object OrgId {
  implicit val reads: Reads[OrgId] = __.read[Long].map(OrgId.apply)

  implicit val writes: Writes[OrgId] = Writes(oid => JsNumber(oid.underlying))

  implicit def fromLong(l: Long): OrgId = OrgId(l)

  implicit def toLong(oid: OrgId): Long = oid.underlying

  implicit def fromOptLong(ml: Option[Long]): Option[OrgId] = ml.map(fromLong)

  implicit def toOptLong(moid: Option[OrgId]): Option[Long] = moid.map(toLong)
}
