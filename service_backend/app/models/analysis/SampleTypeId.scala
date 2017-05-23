package models.analysis

import no.uio.musit.models.MusitId
import play.api.libs.json._

case class SampleTypeId(underlying: Long) extends MusitId

object SampleTypeId {

  implicit val reads: Reads[SampleTypeId] = __.read[Long].map(SampleTypeId.apply)

  implicit val writes: Writes[SampleTypeId] = Writes(eid => JsNumber(eid.underlying))

  val empty: SampleTypeId = SampleTypeId(-1)

  implicit def fromLong(l: Long): SampleTypeId = SampleTypeId(l)

  implicit def toLong(id: SampleTypeId): Long = id.underlying

  implicit def fromOptLong(ml: Option[Long]): Option[SampleTypeId] = ml.map(fromLong)

  implicit def toOptLong(id: Option[SampleTypeId]): Option[Long] = id.map(toLong)

}
