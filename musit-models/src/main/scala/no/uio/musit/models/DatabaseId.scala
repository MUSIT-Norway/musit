package no.uio.musit.models

import play.api.libs.json.{JsNumber, Reads, Writes, _}

case class DatabaseId(underlying: Long) extends AnyVal

object DatabaseId {

  implicit val reads: Reads[DatabaseId] = __.read[Long].map(DatabaseId.apply)

  implicit val writes: Writes[DatabaseId] = Writes(did => JsNumber(did.underlying))

  implicit def fromLong(l: Long): DatabaseId = DatabaseId(l)

  implicit def toLong(id: DatabaseId): Long = id.underlying

  implicit def fromOptLong(ml: Option[Long]): Option[DatabaseId] = ml.map(fromLong)

  implicit def toOptLong(mdid: Option[DatabaseId]): Option[Long] = mdid.map(toLong)

}
