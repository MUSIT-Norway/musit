package no.uio.musit.models

import play.api.libs.json._

case class EventTypeId(underlying: Int) extends AnyVal

object EventTypeId {

  implicit val reads: Reads[EventTypeId]   = __.read[Int].map(EventTypeId.apply)
  implicit val writes: Writes[EventTypeId] = Writes(n => JsNumber(n.underlying))

}
