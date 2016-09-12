package models

import play.api.libs.json.Json

case class ObjectAggregation(id: ObjectId, name: String, nodeId: NodeId)

object ObjectAggregation {
  implicit val format = Json.format[ObjectAggregation]
}