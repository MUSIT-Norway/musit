package models.analysis.events

import no.uio.musit.models.{CollectionUUID, MusitId}
import play.api.libs.json._

case class AnalysisTypeId(underlying: Long) extends MusitId

object AnalysisTypeId {

  implicit val reads: Reads[AnalysisTypeId] = __.read[Long].map(AnalysisTypeId.apply)

  implicit val writes: Writes[AnalysisTypeId] = Writes(n => JsNumber(n.underlying))

}

case class AnalysisType(
    id: AnalysisTypeId,
    category: Category,
    noName: String,
    enName: String,
    shortName: Option[String] = None,
    collections: Seq[CollectionUUID] = Seq.empty,
    extraDescriptionType: Option[String] = None,
    extraDescriptionAttributes: Option[Map[String, String]] = None,
    extraResultAttributes: Option[Map[String, String]] = None
)

object AnalysisType {
  implicit val format: Format[AnalysisType] = Json.format[AnalysisType]
}
