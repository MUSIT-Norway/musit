package no.uio.musit.models

import play.api.libs.json._

case class ObjectId(underlying: Long) extends MusitId

object ObjectId {

  implicit val reads: Reads[ObjectId] = __.read[Long].map(ObjectId.apply)

  implicit val writes: Writes[ObjectId] = Writes(id => JsNumber(id.underlying))

  implicit def fromLong(l: Long): ObjectId = ObjectId(l)

  implicit def toLong(oid: ObjectId): Long = oid.underlying

  implicit def fromOptLong(l: Option[Long]): Option[ObjectId] = l.map(fromLong)

  implicit def toOptLong(id: Option[ObjectId]): Option[Long] = id.map(toLong)

}
