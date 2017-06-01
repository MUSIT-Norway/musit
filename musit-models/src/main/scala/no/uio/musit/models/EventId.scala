package no.uio.musit.models

import play.api.libs.json._

case class EventId(underlying: Long) extends MusitId

object EventId {

  implicit val reads: Reads[EventId] = __.read[Long].map(EventId.apply)

  implicit val writes: Writes[EventId] = Writes(eid => JsNumber(eid.underlying))

  val empty: EventId = EventId(-1)

  implicit def fromLong(l: Long): EventId = EventId(l)

  implicit def toLong(id: EventId): Long = id.underlying

  implicit def fromOptLong(ml: Option[Long]): Option[EventId] = ml.map(fromLong)

  implicit def toOptLong(id: Option[EventId]): Option[Long] = id.map(toLong)

}
