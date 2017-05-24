package models.storage

import models.storage.nodes.StorageNode
import no.uio.musit.models.ObjectId
import play.api.libs.json.{Json, Writes}

case class ObjectsLocation_Old(node: StorageNode, objectIds: Seq[ObjectId])

object ObjectsLocation_Old {

  implicit val writes: Writes[ObjectsLocation_Old] = Json.writes[ObjectsLocation_Old]

}
