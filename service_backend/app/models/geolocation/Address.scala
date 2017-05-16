package models.geolocation

import play.api.libs.json.Json

case class Address(
    street: String,
    streetNo: String,
    place: String,
    zip: String
)

object Address {
  implicit val format = Json.format[Address]
}
