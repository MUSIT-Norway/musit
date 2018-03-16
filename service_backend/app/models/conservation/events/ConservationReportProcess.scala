package models.conservation.events

import no.uio.musit.formatters.WithDateTimeFormatters
import no.uio.musit.models._
import org.joda.time.DateTime
import play.api.libs.json._
import models.musitobject._
import models.conservation.events.ConservationEvent._

case class ConservationProcessForReport(
    id: Option[EventId],
    eventTypeId: EventTypeId,
    eventType: Option[ConservationType],
    caseNumber: Option[String],
    registeredBy: Option[ActorId],
    registeredByName: Option[String],
    registeredDate: Option[DateTime],
    updatedBy: Option[ActorId],
    updatedByName: Option[String],
    updatedDate: Option[DateTime],
    //completedBy: Option[ActorId],
    //completedDate: Option[DateTime],
    partOf: Option[EventId],
    note: Option[String],
    actorsAndRoles: Seq[ActorRoleDate],
    affectedThings: Seq[ObjectUUID],
    events: Seq[ConservationEvent],
    isUpdated: Option[Boolean],
    affectedThingsDetails: Seq[MusitObject]
);

object ConservationProcessForReport extends WithDateTimeFormatters {
  implicit val writes3 = MusitObject.writes

  // implicit val reads2 = ConservationEvent.reads
  val writes2 = ConservationEvent.writes
  // implicit val reads: Reads[ConservationProcessForReport]   = Json.reads[ConservationProcessForReport]
  implicit val writes: Writes[ConservationProcessForReport] =
    Json.writes[ConservationProcessForReport]

  /* implicit val reads3: Reads[MusitObject]   = Json.reads[MusitObject]

  implicit val reads4: Reads[NumismaticsAttribute]   = Json.reads[NumismaticsAttribute]
  implicit val reads5: Reads[MusitObjectMaterial]   = Json.reads[MusitObjectMaterial]
  implicit val reads6: Reads[MusitObjectLocation]   = Json.reads[MusitObjectLocation]
  implicit val reads7: Reads[MusitObjectCoordinate]   = Json.reads[MusitObjectCoordinate]*/

}

sealed trait ConservationEventReport {}

object ConservationEventReport
    extends TypedConservationEvent
    with WithDateTimeFormatters {

  override protected val missingEventTypeMsg =
    "Type must be a subevent of Conservation"

  val writes: Writes[ConservationEventReport] = Writes {
    case pres: TreatmentReport =>
      ConservationEventReport.writes.writes(pres).as[JsObject] ++
        Json.obj(discriminatorAttributeName -> TreatmentReport.eventTypeId)

  }
}

case class TreatmentReport(
    id: Option[EventId],
    eventTypeId: EventTypeId,
    registeredBy: Option[ActorId],
    registeredByName: Option[String],
    registeredDate: Option[DateTime],
    updatedBy: Option[ActorId],
    updatedDate: Option[DateTime],
    //completedBy: Option[ActorId],
    //completedDate: Option[DateTime],
    partOf: Option[EventId],
    note: Option[String],
    actorsAndRoles: Option[Seq[ActorRoleDate]],
    affectedThings: Option[Seq[ObjectUUID]],
    keywords: Option[Seq[Int]],
    materials: Option[Seq[Int]],
    documents: Option[Seq[FileId]],
    isUpdated: Option[Boolean]
) extends ConservationEventReport {}

object TreatmentReport extends WithDateTimeFormatters {
  val eventTypeId = EventTypeId(2)

  //val reads: Reads[TreatmentReport]   = Json.reads[TreatmentReport]
  val writes: Writes[TreatmentReport] = Json.writes[TreatmentReport]

}
