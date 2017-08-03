package models.storage.event.move

import models.storage.Move._
import models.storage.event.EventTypeRegistry.TopLevelEvents.{
  MoveNodeType,
  MoveObjectType
}
import models.storage.event.{StorageFacilityEvent, StorageFacilityEventType}
import no.uio.musit.formatters.WithDateTimeFormatters
import no.uio.musit.models.ObjectTypes.{Node, ObjectType}
import no.uio.musit.models._
import no.uio.musit.time.dateTimeNow
import org.joda.time.DateTime
import play.api.libs.json._

sealed trait MoveEvent extends StorageFacilityEvent {
  val objectType: ObjectType
  val from: Option[StorageNodeId]
  val to: StorageNodeId

  override type T = MoveEvent
}

object MoveEvent {

  implicit val writes: Writes[MoveEvent] = Writes {
    case mo: MoveObject => MoveObject.format.writes(mo)
    case mn: MoveNode   => MoveNode.format.writes(mn)
  }

  implicit val reads: Reads[MoveEvent] = Reads { jsv =>
    (jsv \ "eventType").validate[StorageFacilityEventType] match {
      case JsSuccess(tpe, path) =>
        tpe.registeredEventId match {
          case MoveObjectType.id =>
            MoveObject.format.reads(jsv)

          case MoveNodeType.id =>
            MoveNode.format.reads(jsv)

          case bad =>
            JsError(path, s"Illegal EventType ${tpe.name} for Move operations")
        }

      case err: JsError => err
    }
  }

}

case class MoveObject(
    id: Option[EventId],
    doneBy: Option[ActorId],
    doneDate: Option[DateTime],
    affectedThing: Option[ObjectUUID],
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    eventType: StorageFacilityEventType,
    objectType: ObjectType,
    from: Option[StorageNodeId],
    to: StorageNodeId
) extends MoveEvent {

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
        doneDate = Some(now),
        affectedThing = Option(movables.id),
        registeredBy = Option(currUserId),
        registeredDate = Option(now),
        eventType = StorageFacilityEventType.fromEventTypeId(MoveObjectType.id),
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
    doneDate: Option[DateTime],
    affectedThing: Option[StorageNodeId],
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    eventType: StorageFacilityEventType,
    from: Option[StorageNodeId],
    to: StorageNodeId
) extends MoveEvent {

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
      doneDate = Some(now),
      affectedThing = Option(nodeId),
      registeredBy = Option(currUserId),
      registeredDate = Option(now),
      eventType = StorageFacilityEventType.fromEventTypeId(MoveNodeType.id),
      from = None,
      to = cmd.destination
    )
  }
}
