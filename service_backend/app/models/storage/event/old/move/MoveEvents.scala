package models.storage.event.old.move

import models.storage.Move_Old._
import models.storage.event.EventTypeRegistry.TopLevelEvents.{
  MoveNodeType,
  MoveObjectType
}
import models.storage.event.{EventType, MusitEvent_Old}
import no.uio.musit.formatters.WithDateTimeFormatters
import no.uio.musit.models.ObjectTypes.{CollectionObjectType, Node, ObjectType}
import no.uio.musit.models.{ActorId, EventId, ObjectId, StorageNodeDatabaseId}
import no.uio.musit.time.dateTimeNow
import org.joda.time.DateTime
import play.api.libs.json.{Format, Json}

// TODO: DELETE ME when Migration is performed in production

sealed trait MoveEvent extends MusitEvent_Old {
  val objectType: ObjectType
  val from: Option[StorageNodeDatabaseId]
  val to: StorageNodeDatabaseId
}

case class MoveObject(
    id: Option[EventId],
    doneBy: Option[ActorId],
    doneDate: DateTime,
    affectedThing: Option[ObjectId],
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    eventType: EventType,
    objectType: ObjectType,
    from: Option[StorageNodeDatabaseId],
    to: StorageNodeDatabaseId
) extends MoveEvent

object MoveObject extends WithDateTimeFormatters {

  implicit val format: Format[MoveObject] = Json.format[MoveObject]

  def fromCommand(currUserId: ActorId, cmd: ObjectMoveCmd[_]): Seq[MoveObject] = {
    cmd match {
      case m: MoveObjectsCmd => fromCommand(currUserId, m)
      case d: DelphiMove     => fromDelphiCommand(currUserId, d)
    }
  }

  private[this] def fromCommand(
      currUserId: ActorId,
      cmd: MoveObjectsCmd
  ): Seq[MoveObject] = {
    cmd.items.map { movables =>
      val now = dateTimeNow
      MoveObject(
        id = None,
        doneBy = Option(currUserId),
        doneDate = now,
        affectedThing = Option(movables.id),
        registeredBy = Option(currUserId),
        registeredDate = Option(now),
        eventType = EventType.fromEventTypeId(MoveObjectType.id),
        objectType = movables.objectType,
        from = None,
        to = cmd.destination
      )
    }
  }

  private[this] def fromDelphiCommand(
      currUserId: ActorId,
      cmd: DelphiMove
  ): Seq[MoveObject] = {
    cmd.items.map { objectId =>
      val now = dateTimeNow
      MoveObject(
        id = None,
        doneBy = Option(currUserId),
        doneDate = now,
        affectedThing = Option(objectId),
        registeredBy = Option(currUserId),
        registeredDate = Option(now),
        eventType = EventType.fromEventTypeId(MoveObjectType.id),
        objectType = CollectionObjectType,
        from = None,
        to = cmd.destination
      )
    }
  }
}

case class MoveNode(
    id: Option[EventId],
    doneBy: Option[ActorId],
    doneDate: DateTime,
    affectedThing: Option[StorageNodeDatabaseId],
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    eventType: EventType,
    from: Option[StorageNodeDatabaseId],
    to: StorageNodeDatabaseId
) extends MoveEvent {

  override val objectType: ObjectType = Node

}

object MoveNode {

  implicit val format: Format[MoveNode] = Json.format[MoveNode]

  def fromCommand(
      currUserId: ActorId,
      cmd: MoveNodesCmd
  ): Seq[MoveNode] = cmd.items.map { nodeId =>
    val now = dateTimeNow
    MoveNode(
      id = None,
      doneBy = Option(currUserId),
      doneDate = now,
      affectedThing = Option(nodeId),
      registeredBy = Option(currUserId),
      registeredDate = Option(now),
      eventType = EventType.fromEventTypeId(MoveNodeType.id),
      from = None,
      to = cmd.destination
    )
  }
}
