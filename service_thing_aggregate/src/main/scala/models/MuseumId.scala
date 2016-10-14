package models

import play.api.libs.json._

case class MuseumId(underlying: Int) extends AnyVal

object MuseumId {
  implicit val reads: Reads[MuseumId] = __.read[Int].map(MuseumId.apply)
  implicit val writes: Writes[MuseumId] = Writes { value: MuseumId =>
    JsNumber(value.underlying)
  }

  implicit def fromInt(id: Int): MuseumId = MuseumId(id)
}

