package no.uio.musit.microservice.geoLocation.domain

import play.api.libs.json.Json

/**
 * Created by ellenjo on 4/29/16.
 */

case class GeoNorwayAddress(street: String, streetNo: String, place: String, zip: String)

object GeoNorwayAddress {
  implicit val format = Json.format[GeoNorwayAddress]
}