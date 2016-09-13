package models

import play.api.libs.json.Json

case class ObjectId(id: Long)

object ObjectId {
  implicit val format = Json.format[ObjectId]
}
