package models.storage.event

import no.uio.musit.models.{ActorId, ObjectId, StorageNodeDatabaseId}
import play.api.libs.json.{Format, Json}

case class ActorRole(roleId: Int, actorId: ActorId)

object ActorRole {
  implicit val format: Format[ActorRole] = Json.format[ActorRole]
}

case class ObjectRole(roleId: Int, objectId: ObjectId)

object ObjectRole {
  implicit val format: Format[ObjectRole] = Json.format[ObjectRole]
}

case class PlaceRole(roleId: Int, nodeId: StorageNodeDatabaseId)

object PlaceRole {
  implicit val format: Format[PlaceRole] = Json.format[PlaceRole]
}
