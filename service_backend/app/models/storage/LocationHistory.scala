package models.storage

import models.storage.event.move.MoveObject
import no.uio.musit.formatters.WithDateTimeFormatters
import no.uio.musit.models.ObjectTypes.ObjectType
import no.uio.musit.models.{ActorId, NamedPathElement, NodePath, ObjectUUID}
import org.joda.time.DateTime
import play.api.libs.json.{Json, Writes}

case class LocationHistory(
    registeredBy: ActorId,
    registeredDate: DateTime,
    doneBy: Option[ActorId],
    doneDate: DateTime,
    id: ObjectUUID,
    objectType: ObjectType,
    from: FacilityLocation,
    to: FacilityLocation
)

object LocationHistory extends WithDateTimeFormatters {

  def fromMoveObject(
      moveObject: MoveObject,
      from: FacilityLocation,
      to: FacilityLocation
  ): LocationHistory = {
    LocationHistory(
      // registered by and date is required on Event, so they must be there.
      registeredBy = moveObject.registeredBy.get,
      registeredDate = moveObject.registeredDate.get,
      doneBy = moveObject.doneBy,
      doneDate = moveObject.doneDate,
      id = moveObject.affectedThing.get,
      objectType = moveObject.objectType,
      from = from,
      to = to
    )
  }

  implicit val writes: Writes[LocationHistory] = Json.writes[LocationHistory]
}

case class LocationHistory_Old(
    registeredBy: ActorId,
    registeredDate: DateTime,
    doneBy: Option[ActorId],
    doneDate: DateTime,
    id: Long,
    objectType: ObjectType,
    from: FacilityLocation,
    to: FacilityLocation
)

object LocationHistory_Old extends WithDateTimeFormatters {
  implicit val writes: Writes[LocationHistory_Old] = Json.writes[LocationHistory_Old]
}

case class FacilityLocation(
    path: NodePath,
    pathNames: Seq[NamedPathElement]
)

object FacilityLocation {

  def fromTuple(t: (NodePath, Seq[NamedPathElement])) = FacilityLocation(t._1, t._2)

  implicit val writes: Writes[FacilityLocation] = Json.writes[FacilityLocation]
}
