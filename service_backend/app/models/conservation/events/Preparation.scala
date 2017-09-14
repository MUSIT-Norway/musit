package models.conservation.events

import no.uio.musit.formatters.WithDateTimeFormatters
import no.uio.musit.models.ObjectTypes.ObjectType
import no.uio.musit.models._
import org.joda.time.DateTime
import play.api.libs.json.{Json, Reads, Writes}

case class Preparation(
    id: Option[EventId],
    conservationTypeId: ConservationTypeId,
    caseNumbers: Option[CaseNumbers],
    doneBy: Option[ActorId],
    doneDate: Option[DateTime],
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    responsible: Option[ActorId],
    administrator: Option[ActorId],
    updatedBy: Option[ActorId],
    updatedDate: Option[DateTime],
    completedBy: Option[ActorId],
    completedDate: Option[DateTime],
    affectedThing: Option[ObjectUUID],
    affectedType: Option[ObjectType],
    partOf: Option[EventId],
    note: Option[String],
    doneByActors: Option[Seq[ActorId]],
    affectedThings: Option[Seq[ObjectUUID]]
    // todo extraAttributes: Option[ExtraAttributes]
) extends ConservationEvent {

  override def withId(id: Option[EventId]) = copy(id = id)

  override def withAffectedThing(at: Option[MusitUUID]) = at.fold(this) {
    case oid: ObjectUUID => copy(affectedThing = Some(oid))
    case _               => this
  }

  override def withDoneDate(dd: Option[DateTime]) = copy(doneDate = dd)

}

object Preparation extends WithDateTimeFormatters {
  val discriminator = "Preservation"

  implicit val reads: Reads[Preparation]   = Json.reads[Preparation]
  implicit val writes: Writes[Preparation] = Json.writes[Preparation]

}
