package models.loan.event

import models.loan.LoanEventTypes.{ObjectLentType, ObjectReturnedType}
import models.loan.LoanType
import no.uio.musit.models.{ActorId, EventId, ExternalRef, ObjectUUID}
import org.joda.time.DateTime
import play.api.libs.json._

sealed trait LoanEvent {
  val id: Option[EventId]
  val loanType: LoanType
  val eventDate: Option[DateTime]
  val registeredBy: Option[ActorId]
  val registeredDate: Option[DateTime]
  val partOf: Option[EventId]
  val objectId: Option[ObjectUUID]
  val externalRef: Option[ExternalRef]
  val note: Option[String]
}

object LoanEvent {
  implicit val write = Writes[LoanEvent] {
    case ol: ObjectsLent     => Json.toJson(ol)
    case or: ObjectsReturned => Json.toJson(or)
  }

  implicit val read = Reads[LoanEvent] { jsv =>
    (jsv \ "loanType").validate[LoanType].flatMap {
      case ObjectLentType     => Json.fromJson[ObjectsLent](jsv)
      case ObjectReturnedType => Json.fromJson[ObjectsReturned](jsv)
    }
  }
}

case class ObjectsLent(
    id: Option[EventId],
    loanType: LoanType,
    eventDate: Option[DateTime],
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    partOf: Option[EventId],
    externalRef: Option[ExternalRef],
    note: Option[String],
    returnDate: DateTime,
    objects: Seq[ObjectUUID]
) extends LoanEvent {
  val objectId: Option[ObjectUUID] = None
}

object ObjectsLent {
  implicit val format: Format[ObjectsLent] = Json.format[ObjectsLent]
}

case class ObjectsReturned(
    id: Option[EventId],
    loanType: LoanType,
    eventDate: Option[DateTime],
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    partOf: Option[EventId],
    externalRef: Option[ExternalRef],
    note: Option[String],
    returnDate: DateTime,
    objects: Seq[ObjectUUID]
) extends LoanEvent {
  val objectId: Option[ObjectUUID] = None
}

object ObjectsReturned {
  implicit val format: Format[ObjectsReturned] = Json.format[ObjectsReturned]
}
