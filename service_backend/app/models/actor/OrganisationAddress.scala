package models.actor

import no.uio.musit.models.{DatabaseId, OrgId}
import play.api.libs.json.Json

/**
 * Address specialized for Organization
 */
case class OrganisationAddress(
    id: Option[DatabaseId],
    organisationId: Option[OrgId],
    streetAddress: Option[String],
    streetAddress2: Option[String],
    postalCodePlace: String,
    countryName: String
)

object OrganisationAddress {
  val tupled          = (OrganisationAddress.apply _).tupled
  implicit val format = Json.format[OrganisationAddress]
}
