package models

import play.api.libs.json.Json

case class ObjectId(value: Long)

object ObjectId {
  implicit val format = Json.format[ObjectId]
}

case class MuseumId(value: Long)

object MuseumId {
  implicit val format = Json.format[MuseumId]
}

case class ObjectAggregation(id: ObjectId, name: String, museumId: MuseumId)

object ObjectAggregation {
  implicit val format = Json.format[ObjectAggregation]
}