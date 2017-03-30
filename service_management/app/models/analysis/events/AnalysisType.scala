package models.analysis.events

import java.util.UUID

import no.uio.musit.models.{CollectionUUID, MusitUUID, MusitUUIDOps}
import play.api.libs.json._

case class AnalysisTypeId(underlying: UUID) extends MusitUUID

object AnalysisTypeId extends MusitUUIDOps[AnalysisTypeId] {

  implicit val reads: Reads[AnalysisTypeId] = __.read[String].map { s =>
    AnalysisTypeId(UUID.fromString(s))
  }

  implicit val writes: Writes[AnalysisTypeId] = Writes(id => JsString(id.asString))

  override implicit def fromUUID(uuid: UUID): AnalysisTypeId = AnalysisTypeId(uuid)

  override def generate() = AnalysisTypeId(UUID.randomUUID())

}

case class AnalysisType(
    id: AnalysisTypeId,
    category: Category,
    name: String,
    shortName: Option[String] = None,
    collections: Seq[CollectionUUID],
    extraAttributes: Option[Map[String, String]] = None
)

object AnalysisType {
  implicit val format: Format[AnalysisType] = Json.format[AnalysisType]
}
