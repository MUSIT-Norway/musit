package models

import play.api.libs.json.Json

case class NodeId(value: Long)

object NodeId {
  implicit val format = Json.format[NodeId]
}
