package models.storage

import no.uio.musit.models.{ObjectId, StorageNodeDatabaseId, StorageNodeId}
import play.api.libs.json.{Json, Reads}

object Move {

  trait BaseMoveCmd[A, ID] {
    val destination: ID
    val items: Seq[A]
  }

  /**
   * All move commands must contain a destination and a list of items to move.
   */
  trait MoveCmd[A] extends BaseMoveCmd[A, StorageNodeId] {
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

  /**
   * This is a special case only to be used by the Delphi client, where a couple
   * of assumptions can be made.
   *
   * 1. The Delphi client can only ever move collection objects.
   * 2. The Delphi client does not know about sample objects.
   * 3. All move commands from Delphi will be of type "collection".
   *
   * Given the above, we do not need to process items as MovableObjects. Instead
   * we will make _all_ moves such that they always are collection objects.
   *
   * @param destination the StorageNodeDatabaseId to place the objects
   * @param items       list of ObjectIds for the objects to move
   * @see {{{MoveObjectsCmd}}}
   * @see {{{models.Move.MoveObject#fromDelphiCommand}}}
   */
  // TODO: Can be removed when the Delphi client doesn't need to move objects.
  case class DelphiMoveCmd(
      destination: StorageNodeDatabaseId,
      items: Seq[ObjectId]
  ) extends BaseMoveCmd[ObjectId, StorageNodeDatabaseId]

  object DelphiMoveCmd {
    implicit val reads: Reads[DelphiMoveCmd] = Json.reads[DelphiMoveCmd]
  }

}
