package no.uio.musit.models

import play.api.libs.json.{JsNumber, Reads, Writes, __}

case class CollectionId(underlying: Int) extends AnyVal

object CollectionId {
  implicit val reads: Reads[CollectionId]   = __.read[Int].map(CollectionId.apply)
  implicit val writes: Writes[CollectionId] = Writes(id => JsNumber(id.underlying))

  implicit def fromInt(id: Int): CollectionId = CollectionId(id)

  implicit def toInt(mid: CollectionId): Int = mid.underlying
}
