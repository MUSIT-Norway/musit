package models.storage.event.move

import models.storage.Move._
import models.storage.event.EventTypeRegistry.TopLevelEvents.{
  MoveNodeType,
  MoveObjectType
}
import models.storage.event.{EventType, MusitEvent}
import no.uio.musit.formatters.WithDateTimeFormatters
import no.uio.musit.models.ObjectTypes.{Node, ObjectType}
import no.uio.musit.models._
import no.uio.musit.time.dateTimeNow
import org.joda.time.DateTime
import play.api.libs.json.{Format, Json}

sealed trait MoveEvent extends MusitEvent {
  val objectType: ObjectType
  val from: Option[StorageNodeId]
  val to: StorageNodeId
}

case class MoveObject(
    id: Option[EventId],
    doneBy: Option[ActorId],
    doneDate: DateTime,
    affectedThing: Option[ObjectUUID],
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    eventType: EventType,
    objectType: ObjectType,
    from: Option[StorageNodeId],
    to: StorageNodeId
) extends MoveEvent {

  override type T = MoveObject

  override def withId(id: Option[EventId]) = copy(id = id)

}

object MoveObject extends WithDateTimeFormatters {

  implicit val format: Format[MoveObject] = Json.format[MoveObject]

  def fromCommand(
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
}

case class MoveNode(
    id: Option[EventId],
    doneBy: Option[ActorId],
    doneDate: DateTime,
    affectedThing: Option[StorageNodeId],
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    eventType: EventType,
    from: Option[StorageNodeId],
    to: StorageNodeId
) extends MoveEvent {

  override type T = MoveNode

  override val objectType: ObjectType = Node

  override def withId(id: Option[EventId]) = copy(id = id)

}

object MoveNode extends WithDateTimeFormatters {

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
