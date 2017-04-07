package models.loan.event

import models.loan.LoanType
import no.uio.musit.models.{ActorId, EventId, ExternalRef, ObjectUUID}
import org.joda.time.DateTime
import play.api.libs.json.{Format, Json}

trait LoanEvent {
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

case class LentObject( //todo flip wording
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

object LentObject {
  implicit val format: Format[LentObject] = Json.format[LentObject]
}

case class ReturnedObject( //todo flip wording
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

object ReturnedObject {
  implicit val format: Format[ReturnedObject] = Json.format[ReturnedObject]
}
