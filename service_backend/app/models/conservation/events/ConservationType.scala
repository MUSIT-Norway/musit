package models.conservation.events

import no.uio.musit.models.{CollectionUUID, EventTypeId}
import play.api.libs.json._

/**
 * Represents an {{{ConservationType}}} as it is stored in the database. The field
 * {{{extraDescriptionAttributes}}}  is maps with
 * key value pairs that are populated from JSON representations in the DB.
 *
 * @param id                         The unique ID for this type.
 *
 * @param noName                     The name in norwegian
 * @param enName                     The name in english

 * @param collections                A list of collections that have this type
 *                                   specifically.
 * @param extraDescriptionType       String value containing the value of
 *                                   {{{typeName}}} in an {{{ExtraAttributes}}}
 *                                   implementation companion object.
 * @param extraDescriptionAttributes A Map of key/value pairs, where the key is
 *                                   the name of the attribute. And the value is
 *                                   the data type of the attribute.
 *
 * @see [[models.conservation.events.ConservationExtras.ExtraAttributes]]
 */
case class ConservationType(
    id: EventTypeId,
    noName: String,
    enName: String,
    collections: Seq[CollectionUUID] = Seq.empty,
    extraDescriptionType: Option[String] = None,
    extraDescriptionAttributes: Option[Map[String, String]] = None
)

object ConservationType {

  implicit val reads: Reads[ConservationType]   = Json.reads[ConservationType]
  implicit val writes: Writes[ConservationType] = Json.writes[ConservationType]

}
