package models

import play.api.libs.json.Json

case class ObjectId(value: Long)

object ObjectId {
  implicit val format = Json.format[ObjectId]
}
