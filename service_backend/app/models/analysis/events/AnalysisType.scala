package models.analysis.events

import no.uio.musit.models.CollectionUUID
import play.api.libs.json._

/**
 * Represents an {{{AnalysisType}}} as it is stored in the database. The fields
 * {{{extraDescriptionAttributes}}} and {{{extraResultAttributes}}} are maps with
 * key value pairs that are populated from JSON representations in the DB.
 *
 * @param id                         The unique ID for this type.
 * @param category                   The event {{{Category}}} this analysis type
 *                                   belongs to.
 * @param noName                     The name in norwegian
 * @param enName                     The name in english
 * @param shortName                  A shorter name for the analyis type. Typically
 *                                   an acronym.
 * @param collections                A list of collections that have this type
 *                                   specifically.
 * @param extraDescriptionType       String value containing the value of
 *                                   {{{typeName}}} in an {{{ExtraAttributes}}}
 *                                   implementation companion object.
 * @param extraDescriptionAttributes A Map of key/value pairs, where the key is
 *                                   the name of the attribute. And the value is
 *                                   the data type of the attribute.
 * @param extraResultAttributes      A Map of key/value pairs, where the key is
 *                                   the name fo the extra result attribute. And
 *                                   the value is the data type of the attribute.
 * @see [[models.analysis.events.AnalysisExtras.ExtraAttributes]]
 * @see [[models.analysis.events.Category]]
 */
case class AnalysisType(
    id: AnalysisTypeId,
    category: Category,
    noName: String,
    enName: String,
    shortName: Option[String] = None,
    collections: Seq[CollectionUUID] = Seq.empty,
    extraDescriptionType: Option[String] = None,
    extraDescriptionAttributes: Option[Map[String, String]] = None,
    extraResultType: Option[String] = None,
    extraResultAttributes: Option[Map[String, String]] = None
)

object AnalysisType {
  implicit val reads: Reads[AnalysisType] = Json.reads[AnalysisType]
}
