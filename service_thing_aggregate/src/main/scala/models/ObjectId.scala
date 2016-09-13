package models

import play.api.libs.json._

case class ObjectId(underlying: Long) extends AnyVal

object ObjectId {

  implicit val reads: Reads[ObjectId] = __.read[Long].map(ObjectId.apply)

  implicit val writes: Writes[ObjectId] = Writes { oid =>
    JsNumber(oid.underlying)
  }

}
