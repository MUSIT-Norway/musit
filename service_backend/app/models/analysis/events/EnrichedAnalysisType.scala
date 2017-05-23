package models.analysis.events

import models.analysis.events.AnalysisExtras.{
  DescriptionAttributeValue,
  DescriptionAttributeValueOps,
  ExtraAttributes
}
import no.uio.musit.models.CollectionUUID
import play.api.libs.json._

case class EnrichedDescriptionAttribute(
    attributeKey: String,
    attributeType: String,
    allowedValues: Option[Seq[DescriptionAttributeValue]] = None
)

object EnrichedDescriptionAttribute extends DescriptionAttributeValueOps {

  implicit val writes = Json.writes[EnrichedDescriptionAttribute]

  def from(
      typeName: Option[String],
      attributes: Map[String, String]
  ): Seq[EnrichedDescriptionAttribute] = {
    attributes.map { kv =>
      EnrichedDescriptionAttribute(
        attributeKey = kv._1,
        attributeType = kv._2,
        allowedValues = typeName.flatMap(tn => ExtraAttributes.valuesFor(tn))
      )
    }.toSeq
  }

}

case class EnrichedAnalysisType(
    id: AnalysisTypeId,
    category: Category,
    noName: String,
    enName: String,
    shortName: Option[String] = None,
    collections: Seq[CollectionUUID] = Seq.empty,
    extraDescriptionType: Option[String] = None,
    extraDescriptionAttributes: Option[Seq[EnrichedDescriptionAttribute]] = None,
    extraResultAttributes: Option[Map[String, String]] = None
)

object EnrichedAnalysisType {

  implicit val writes: Writes[EnrichedAnalysisType] = Json.writes[EnrichedAnalysisType]

  def fromAnalysisType(at: AnalysisType): EnrichedAnalysisType = {
    EnrichedAnalysisType(
      id = at.id,
      category = at.category,
      noName = at.noName,
      enName = at.enName,
      shortName = at.shortName,
      collections = at.collections,
      extraDescriptionType = at.extraDescriptionType,
      extraDescriptionAttributes = at.extraDescriptionAttributes.map { m =>
        EnrichedDescriptionAttribute.from(at.extraDescriptionType, m)
      },
      extraResultAttributes = at.extraResultAttributes
    )
  }

  def fromAnalysisTypes(ats: Seq[AnalysisType]): Seq[EnrichedAnalysisType] = {
    ats.map(fromAnalysisType)
  }

}
