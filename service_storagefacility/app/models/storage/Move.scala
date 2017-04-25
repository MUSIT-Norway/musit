package models.storage

import no.uio.musit.models.StorageNodeId
import play.api.libs.json.{Json, Reads}

object Move {

  /**
   * All move commands must contain a destination and a list of items to move.
   */
  trait MoveCmd[A] {
    val destination: StorageNodeId
    val items: Seq[A]
  }

  /**
   * Moves one or more nodes from their original location to the defined
   * destination. The identifiers fo for the items are the database ID.
   */
  case class MoveNodesCmd(
      destination: StorageNodeId,
      items: Seq[StorageNodeId]
  ) extends MoveCmd[StorageNodeId]

  object MoveNodesCmd {
    implicit val reads: Reads[MoveNodesCmd] = Json.reads[MoveNodesCmd]
  }

  /**
   * Moves one or more objects from their original location to the defined
   * destination. The items must be defined as MovableObjects, which needs both
   * the database ID and the type (collection | sample) of the objects to move.
   *
   * @param destination the StorageNodeId to place the objects
   * @param items       list of MovableObjects with UUIDs + types for the objects to move
   */
  case class MoveObjectsCmd(
      destination: StorageNodeId,
      items: Seq[MovableObject]
  ) extends MoveCmd[MovableObject]

  object MoveObjectsCmd {
    implicit val reads: Reads[MoveObjectsCmd] = Json.reads[MoveObjectsCmd]
  }

}
