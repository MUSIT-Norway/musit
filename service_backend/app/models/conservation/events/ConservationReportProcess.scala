package models.conservation.events

import models.conservation.TreatmentMaterial
import models.conservation.TreatmentKeyword
import no.uio.musit.formatters.WithDateTimeFormatters
import no.uio.musit.models._
import org.joda.time.DateTime
import play.api.libs.json._
import models.musitobject._
import models.conservation.events.ConservationEvent.{discriminatorAttributeName, _}

sealed trait ConservationReportSubEvent extends ModernMusitEvent {

  val eventTypeId: EventTypeId
  val partOf: Option[EventId]
  val note: Option[String]

  val updatedBy: Option[ActorId]
  val actorsAndRoles: Option[Seq[ActorRoleDate]]
  val affectedThings: Option[Seq[ObjectUUID]]
  val updatedDate: Option[DateTime]
  val documents: Option[Seq[FileId]]
  val isUpdated: Option[Boolean]
  def withoutActorRoleAndDates: ConservationReportSubEvent = withActorRoleAndDates(None)

  def withActorRoleAndDates(
      actorsAndRoles: Option[Seq[ActorRoleDate]]
  ): ConservationReportSubEvent
  def withUpdatedInfo(
      updatedBy: Option[ActorId],
      updatedDate: Option[DateTime]
  ): ConservationReportSubEvent

  def asPartOf(partOf: Option[EventId]): ConservationReportSubEvent

  //A new copy, appropriate when adding/inserting the event in the database.
  def withRegisteredInfo(
      registeredBy: Option[ActorId],
      registeredDate: Option[DateTime]
  ): ConservationReportSubEvent

  def withUpdatedInfoEx(actorDate: ActorDate) =
    withUpdatedInfo(Some(actorDate.user), Some(actorDate.date))
  def withRegisteredInfoEx(actorDate: ActorDate) =
    withRegisteredInfo(Some(actorDate.user), Some(actorDate.date))

  def withAffectedThings(objects: Option[Seq[ObjectUUID]]): ConservationReportSubEvent

  def withDocuments(fileIds: Option[Seq[FileId]]): ConservationReportSubEvent

  //def withActorRoleAndDates(actorsAndRoles: Option[Seq[ActorRoleDate]]): ConservationReportSubEvent

//  override def caseNumber = throw new IllegalStateException(
//    "Don't use caseNumber in subEvents"
//  )

  def cleanupBeforeInsertIntoDatabase: ConservationReportSubEvent
}

object ConservationReportSubEvent extends TypedConservationEvent {

  /**
   * The implicit Reads for all events in the conservation module. It ensures that
   * the JSON message aligns with one of the types defined in
   * eventTypes in Conservation. If not the parsing will (and should) fail.
   */
  /*  implicit val reads: Reads[ConservationReportSubEvent] = Reads { jsv =>
    (jsv \ "eventTypeId").validateOpt[EventTypeId] match {
      case JsSuccess(maybeType, path) =>
        maybeType.map {
          case ConservationProcess.eventTypeId =>
            ConservationProcess.reads.reads(jsv)
          case _ =>
            ConservationEvent.reads.reads(jsv)

        }.getOrElse {
          JsError(path, missingEventTypeMsg)
        }

      case err: JsError =>
        err
    }
  }*/

  /**
   * Implicit Writes for conservation module events. Ensures that each type
   * is written with their specific type discriminator. This ensure that the
   * JSON message is readable on the other end.
   */
  /* implicit val writes: Writes[ConservationReportSubEvent] = Writes {
    case cpe: ConservationProcess =>
      ConservationProcess.writes.writes(cpe).as[JsObject] ++ Json.obj(
        discriminatorAttributeName -> ConservationProcess.eventTypeId
      )

    case te: Treatment =>
      Treatment.writes.writes(te).as[JsObject] ++ Json.obj(
        discriminatorAttributeName -> Treatment.eventTypeId
      )*/

  /* case tde: TechnicalDescription =>
      TechnicalDescription.writes.writes(tde).as[JsObject] ++ Json.obj(
        discriminatorAttributeName -> TechnicalDescription.eventTypeId
      )

    case sahe: StorageAndHandling =>
      StorageAndHandling.writes.writes(sahe).as[JsObject] ++ Json.obj(
        discriminatorAttributeName -> StorageAndHandling.eventTypeId
      )

    case hsera: HseRiskAssessment =>
      HseRiskAssessment.writes.writes(hsera).as[JsObject] ++ Json.obj(
        discriminatorAttributeName -> HseRiskAssessment.eventTypeId
      )

    case ca: ConditionAssessment =>
      ConditionAssessment.writes.writes(ca).as[JsObject] ++ Json.obj(
        discriminatorAttributeName -> ConditionAssessment.eventTypeId
      )
    case re: Report =>
      Report.writes.writes(re).as[JsObject] ++ Json.obj(
        discriminatorAttributeName -> Report.eventTypeId
      )
    case md: MaterialDetermination =>
      MaterialDetermination.writes.writes(md).as[JsObject] ++ Json.obj(
        discriminatorAttributeName -> MaterialDetermination.eventTypeId
      )
    case msmd: MeasurementDetermination =>
      MeasurementDetermination.writes.writes(msmd).as[JsObject] ++ Json.obj(
        discriminatorAttributeName -> MeasurementDetermination.eventTypeId
      )
    case note: Note =>
      Note.writes.writes(note).as[JsObject] ++ Json.obj(
        discriminatorAttributeName -> Note.eventTypeId
      )*/
  // }

  implicit val writes: Writes[ConservationReportSubEvent] = Writes {
    case cpe: ConservationProcessForReport =>
      ConservationProcessForReport.writes.writes(cpe).as[JsObject] ++ Json.obj(
        discriminatorAttributeName -> ConservationProcessForReport.eventTypeId
      )

    case te: TreatmentReport =>
      TreatmentReport.writes.writes(te).as[JsObject] ++ Json.obj(
        discriminatorAttributeName -> TreatmentReport.eventTypeId
      )
  }
  /*
    implicit val writes: Writes[ConservationReportSubEvent] = Writes {
      case pres: TreatmentReport =>
        ConservationReportSubEvent.writes.writes(pres).as[JsObject] ++
          Json.obj(discriminatorAttributeName -> TreatmentReport.eventTypeId)
       case prep: TechnicalDescription =>
        ConservationEvent.writes.writes(prep).as[JsObject] ++
          Json.obj(discriminatorAttributeName -> TechnicalDescription.eventTypeId)
      case sahe: StorageAndHandling =>
        ConservationEvent.writes.writes(sahe).as[JsObject] ++
          Json.obj(discriminatorAttributeName -> StorageAndHandling.eventTypeId)
      case hsera: HseRiskAssessment =>
        ConservationEvent.writes.writes(hsera).as[JsObject] ++
          Json.obj(discriminatorAttributeName -> HseRiskAssessment.eventTypeId)
      case ca: ConditionAssessment =>
        ConservationEvent.writes.writes(ca).as[JsObject] ++
          Json.obj(discriminatorAttributeName -> ConditionAssessment.eventTypeId)
      case re: Report =>
        ConservationEvent.writes.writes(re).as[JsObject] ++
          Json.obj(discriminatorAttributeName -> Report.eventTypeId)
      case md: MaterialDetermination =>
        ConservationEvent.writes.writes(md).as[JsObject] ++
          Json.obj(discriminatorAttributeName -> MaterialDetermination.eventTypeId)
      case msmd: MeasurementDetermination =>
        ConservationEvent.writes.writes(msmd).as[JsObject] ++
          Json.obj(discriminatorAttributeName -> MeasurementDetermination.eventTypeId)
      case note: Note =>
        ConservationEvent.writes.writes(note).as[JsObject] ++
          Json.obj(discriminatorAttributeName -> Note.eventTypeId)
  }
 */
}

case class ActorRoleDateDetails(
    roleId: Int,
    role: Option[EventRole],
    actorId: ActorId,
    actor: Option[String],
    date: Option[DateTime]
)

object ActorRoleDateDetails extends WithDateTimeFormatters {
  implicit val format: Format[ActorRoleDateDetails] = Json.format[ActorRoleDateDetails]
}

case class TreatmentReport(
    id: Option[EventId],
    eventTypeId: EventTypeId,
    eventType: Option[ConservationType],
    registeredBy: Option[ActorId],
    registeredByName: Option[String],
    registeredDate: Option[DateTime],
    updatedBy: Option[ActorId],
    updatedByName: Option[String],
    updatedDate: Option[DateTime],
    partOf: Option[EventId],
    note: Option[String],
    actorsAndRoles: Option[Seq[ActorRoleDate]],
    actorsAndRolesDetails: Seq[ActorRoleDateDetails],
    affectedThings: Option[Seq[ObjectUUID]],
    affectedThingsDetails: Seq[MusitObject],
    keywords: Option[Seq[Int]],
    keywordsDetails: Seq[TreatmentKeyword],
    materials: Option[Seq[Int]],
    materialsDetails: Seq[TreatmentMaterial],
    documents: Option[Seq[FileId]],
    isUpdated: Option[Boolean]
) extends ConservationReportSubEvent {

  override def cleanupBeforeInsertIntoDatabase = copy(isUpdated = None)

  override def withId(id: Option[EventId]) = copy(id = id)

  override def withUpdatedInfo(
      updatedBy: Option[ActorId],
      updatedDate: Option[DateTime]
  ) = copy(updatedBy = updatedBy, updatedDate = updatedDate)

  override def withRegisteredInfo(
      registeredBy: Option[ActorId],
      registeredDate: Option[DateTime]
  ) = copy(registeredBy = registeredBy, registeredDate = registeredDate)

  override def asPartOf(partOf: Option[EventId]) = copy(partOf = partOf)

  override def withAffectedThings(
      objects: Option[Seq[ObjectUUID]]
  ): ConservationReportSubEvent =
    copy(affectedThings = objects)

  override def withDocuments(fileIds: Option[Seq[FileId]]): ConservationReportSubEvent =
    copy(documents = fileIds)

  override def withActorRoleAndDates(actorsAndRoles: Option[Seq[ActorRoleDate]]) =
    copy(actorsAndRoles = actorsAndRoles)

}

sealed trait ConservationSubEventType {
  val eventTypeId: EventTypeId
}

object ConservationSubEventType {
  def apply(eventTypeId: EventTypeId): Option[ConservationSubEventType] = {
    eventTypeId match {
      case TreatmentReport.eventTypeId => Some(TreatmentReport)
      /*case TechnicalDescription.eventTypeId     => Some(TechnicalDescription)
      case StorageAndHandling.eventTypeId       => Some(StorageAndHandling)
      case HseRiskAssessment.eventTypeId        => Some(HseRiskAssessment)
      case ConditionAssessment.eventTypeId      => Some(ConditionAssessment)
      case Report.eventTypeId                   => Some(Report)
      case MaterialDetermination.eventTypeId    => Some(MaterialDetermination)
      case MeasurementDetermination.eventTypeId => Some(MeasurementDetermination)
      case Note.eventTypeId                     => Some(Note)*/
      case _ => None
    }
  }
  def mustFind(eventTypeId: EventTypeId): ConservationSubEventType = {
    apply(eventTypeId) match {
      case Some(t) => t
      case None =>
        throw new IllegalStateException(
          s"Unknown/unhandled eventTypeId: $eventTypeId in ConservationProcess.ConservationProcess.subEventTypeIdToConservationEventType"
        )
    }
  }
}

object TreatmentReport extends WithDateTimeFormatters with ConservationSubEventType {
  val eventTypeId = EventTypeId(2)

  //val reads: Reads[TreatmentReport]   = Json.reads[TreatmentReport]
  val writes: Writes[TreatmentReport] = Json.writes[TreatmentReport]

}

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
    eventsDetails: Seq[ConservationReportSubEvent],
    isUpdated: Option[Boolean],
    affectedThingsDetails: Seq[MusitObject]
);

object ConservationProcessForReport extends WithDateTimeFormatters {
  implicit val writes3 = MusitObject.writes
  val eventTypeId      = EventTypeId(1)

  // implicit val reads2 = ConservationEvent.reads
  val writes2 = ConservationReportSubEvent.writes
  // implicit val reads: Reads[ConservationProcessForReport]   = Json.reads[ConservationProcessForReport]
  implicit val writes: Writes[ConservationProcessForReport] =
    Json.writes[ConservationProcessForReport]

  /* implicit val reads3: Reads[MusitObject]   = Json.reads[MusitObject]

  implicit val reads4: Reads[NumismaticsAttribute]   = Json.reads[NumismaticsAttribute]
  implicit val reads5: Reads[MusitObjectMaterial]   = Json.reads[MusitObjectMaterial]
  implicit val reads6: Reads[MusitObjectLocation]   = Json.reads[MusitObjectLocation]
  implicit val reads7: Reads[MusitObjectCoordinate]   = Json.reads[MusitObjectCoordinate]*/

}
