package models.storage

import models.storage.nodes.StorageNode
import no.uio.musit.models.ObjectUUID
import play.api.libs.json.{Json, Writes}

case class ObjectsLocation(node: StorageNode, objectIds: Seq[ObjectUUID])

object ObjectsLocation {

  implicit val w: Writes[ObjectsLocation] = Json.writes[ObjectsLocation]

}
