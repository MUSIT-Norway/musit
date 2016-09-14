package models

import play.api.libs.json.Json

case class ObjectAggregation(id: ObjectId, identifier: MuseumIdentifier, displayName: Option[String])

object ObjectAggregation {
  implicit val format = Json.format[ObjectAggregation]
}