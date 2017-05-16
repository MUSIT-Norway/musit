package models.actor

import no.uio.musit.models.{DatabaseId, OrgId}
import play.api.libs.json.Json

/**
 * Address specialized for Organization
 */
case class OrganisationAddress(
    id: Option[DatabaseId],
    organisationId: Option[OrgId],
    addressType: String,
    streetAddress: String,
    locality: String,
    postalCode: String,
    countryName: String,
    latitude: Double,
    longitude: Double
)

object OrganisationAddress {
  val tupled          = (OrganisationAddress.apply _).tupled
  implicit val format = Json.format[OrganisationAddress]
}
