package models.conservation.events

import models.conservation.{
  ConditionCode,
  MaterialBase,
  TreatmentKeyword,
  TreatmentMaterial
}
import no.uio.musit.formatters.WithDateTimeFormatters
import no.uio.musit.models._
import org.joda.time.DateTime
import play.api.libs.json._
import models.musitobject._

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

  val eventType: Option[ConservationType]

  val registeredByName: Option[String]

  val updatedByName: Option[String]

  val actorsAndRolesDetails: Seq[ActorRoleDateDetails]

  val affectedThingsDetails: Seq[MusitObject]

  def withoutActorRoleAndDates: ConservationReportSubEvent = withActorRoleAndDates(None)
  def withActorRoleAndDates(
      actorsAndRoles: Option[Seq[ActorRoleDate]]
  ): ConservationReportSubEvent
  def withUpdatedInfo(
      updatedBy: Option[ActorId],
      updatedDate: Option[DateTime]
  ): ConservationReportSubEvent

  def asPartOf(partOf: Option[EventId]): ConservationReportSubEvent

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

  def cleanupBeforeInsertIntoDatabase: ConservationReportSubEvent
}

object ConservationReportSubEvent extends TypedConservationEvent {

  implicit val writes: Writes[ConservationReportSubEvent] = Writes {
    case cpe: ConservationProcessForReport =>
      ConservationProcessForReport.writes.writes(cpe).as[JsObject] ++ Json.obj(
        discriminatorAttributeName -> ConservationProcessForReport.eventTypeId
      )
    case te: TreatmentReport =>
      TreatmentReport.writes.writes(te).as[JsObject] ++ Json.obj(
        discriminatorAttributeName -> TreatmentReport.eventTypeId
      )
    case tde: TechnicalDescriptionReport =>
      TechnicalDescriptionReport.writes.writes(tde).as[JsObject] ++ Json.obj(
        discriminatorAttributeName -> TechnicalDescriptionReport.eventTypeId
      )
    case sahe: StorageAndHandlingReport =>
      StorageAndHandlingReport.writes.writes(sahe).as[JsObject] ++ Json.obj(
        discriminatorAttributeName -> StorageAndHandlingReport.eventTypeId
      )
    case hsera: HseRiskAssessmentReport =>
      HseRiskAssessmentReport.writes.writes(hsera).as[JsObject] ++ Json.obj(
        discriminatorAttributeName -> HseRiskAssessmentReport.eventTypeId
      )
    case ca: ConditionAssessmentReport =>
      ConditionAssessmentReport.writes.writes(ca).as[JsObject] ++ Json.obj(
        discriminatorAttributeName -> ConditionAssessmentReport.eventTypeId
      )
    case re: ReportReport =>
      ReportReport.writes.writes(re).as[JsObject] ++ Json.obj(
        discriminatorAttributeName -> ReportReport.eventTypeId
      )
    case md: MaterialDeterminationReport =>
      MaterialDeterminationReport.writes.writes(md).as[JsObject] ++ Json.obj(
        discriminatorAttributeName -> MaterialDeterminationReport.eventTypeId
      )
    case msmd: MeasurementDeterminationReport =>
      MeasurementDeterminationReport.writes.writes(msmd).as[JsObject] ++ Json.obj(
        discriminatorAttributeName -> MeasurementDeterminationReport.eventTypeId
      )
    case note: NoteReport =>
      NoteReport.writes.writes(note).as[JsObject] ++ Json.obj(
        discriminatorAttributeName -> NoteReport.eventTypeId
      )
  }
}

sealed trait ConservationSubEventType {
  val eventTypeId: EventTypeId
}

object ConservationSubEventType {
  def apply(eventTypeId: EventTypeId): Option[ConservationSubEventType] = {
    eventTypeId match {
      case TreatmentReport.eventTypeId             => Some(TreatmentReport)
      case TechnicalDescriptionReport.eventTypeId  => Some(TechnicalDescriptionReport)
      case StorageAndHandlingReport.eventTypeId    => Some(StorageAndHandlingReport)
      case HseRiskAssessmentReport.eventTypeId     => Some(HseRiskAssessmentReport)
      case ConditionAssessmentReport.eventTypeId   => Some(ConditionAssessmentReport)
      case ReportReport.eventTypeId                => Some(ReportReport)
      case MaterialDeterminationReport.eventTypeId => Some(MaterialDeterminationReport)
      case MeasurementDeterminationReport.eventTypeId =>
        Some(MeasurementDeterminationReport)
      case NoteReport.eventTypeId => Some(NoteReport)
      case _                      => None
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
    documentsDetails: Seq[String],
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

object TreatmentReport extends WithDateTimeFormatters with ConservationSubEventType {
  val eventTypeId = EventTypeId(2)
  //val reads: Reads[TreatmentReport]   = Json.reads[TreatmentReport]
  val writes: Writes[TreatmentReport] = Json.writes[TreatmentReport]
}

case class TechnicalDescriptionReport(
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
    documents: Option[Seq[FileId]],
    documentsDetails: Seq[String],
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

  override def withActorRoleAndDates(actorsAndRoles: Option[Seq[ActorRoleDate]]) =
    copy(actorsAndRoles = actorsAndRoles)

  override def withDocuments(fileIds: Option[Seq[FileId]]): ConservationReportSubEvent =
    copy(documents = fileIds)

}

object TechnicalDescriptionReport
    extends WithDateTimeFormatters
    with ConservationSubEventType {
  val eventTypeId = EventTypeId(3)
  //val reads: Reads[TechnicalDescriptionReport]   = Json.reads[TechnicalDescriptionReport]
  val writes: Writes[TechnicalDescriptionReport] = Json.writes[TechnicalDescriptionReport]
}

case class StorageAndHandlingReport(
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
    lightLevel: Option[String],
    uvLevel: Option[String],
    relativeHumidity: Option[String],
    temperature: Option[String],
    documents: Option[Seq[FileId]],
    documentsDetails: Seq[String],
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

  override def withActorRoleAndDates(actorsAndRoles: Option[Seq[ActorRoleDate]]) =
    copy(actorsAndRoles = actorsAndRoles)

  override def withDocuments(fileIds: Option[Seq[FileId]]): ConservationReportSubEvent =
    copy(documents = fileIds)

}

object StorageAndHandlingReport
    extends WithDateTimeFormatters
    with ConservationSubEventType {
  val eventTypeId = EventTypeId(4)
  //val reads: Reads[StorageAndHandlingReport]   = Json.reads[StorageAndHandlingReport]
  val writes: Writes[StorageAndHandlingReport] = Json.writes[StorageAndHandlingReport]
}

case class HseRiskAssessmentReport(
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
    documents: Option[Seq[FileId]],
    documentsDetails: Seq[String],
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

  override def withActorRoleAndDates(actorsAndRoles: Option[Seq[ActorRoleDate]]) =
    copy(actorsAndRoles = actorsAndRoles)

  override def withDocuments(fileIds: Option[Seq[FileId]]): ConservationReportSubEvent =
    copy(documents = fileIds)

}

object HseRiskAssessmentReport
    extends WithDateTimeFormatters
    with ConservationSubEventType {
  val eventTypeId = EventTypeId(5)
  // val reads: Reads[HseRiskAssessmentReport]   = Json.reads[HseRiskAssessmentReport]
  val writes: Writes[HseRiskAssessmentReport] = Json.writes[HseRiskAssessmentReport]
}

case class ConditionAssessmentReport(
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
    conditionCode: Option[Int],
    conditionCodeDetails: Option[ConditionCode],
    documents: Option[Seq[FileId]],
    documentsDetails: Seq[String],
    isUpdated: Option[Boolean]
) extends ConservationReportSubEvent {

  override def cleanupBeforeInsertIntoDatabase = copy(isUpdated = None)
  override def withId(id: Option[EventId])     = copy(id = id)

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

  override def withActorRoleAndDates(actorsAndRoles: Option[Seq[ActorRoleDate]]) =
    copy(actorsAndRoles = actorsAndRoles)

  override def withDocuments(fileIds: Option[Seq[FileId]]): ConservationReportSubEvent =
    copy(documents = fileIds)

}

object ConditionAssessmentReport
    extends WithDateTimeFormatters
    with ConservationSubEventType {
  val eventTypeId = EventTypeId(6)
  // val reads: Reads[ConditionAssessmentReport]   = Json.reads[ConditionAssessmentReport]
  val writes: Writes[ConditionAssessmentReport] = Json.writes[ConditionAssessmentReport]

}

case class ReportReport(
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
    documents: Option[Seq[FileId]],
    documentsDetails: Seq[String],
    archiveReference: Option[String],
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

  override def withActorRoleAndDates(actorsAndRoles: Option[Seq[ActorRoleDate]]) =
    copy(actorsAndRoles = actorsAndRoles)

  override def withDocuments(fileIds: Option[Seq[FileId]]): ConservationReportSubEvent =
    copy(documents = fileIds)

}

object ReportReport extends WithDateTimeFormatters with ConservationSubEventType {
  val eventTypeId = EventTypeId(7)
  //val reads: Reads[ReportReport]   = Json.reads[ReportReport]
  val writes: Writes[ReportReport] = Json.writes[ReportReport]

}

case class MaterialInfoDetails(
    materialId: Int,
    // material: Option[MaterialBase],
    materialExtra: Option[String],
    sorting: Option[Int]
)

object MaterialInfoDetails {
  implicit val format: Format[MaterialInfoDetails] =
    Json.format[MaterialInfoDetails]
}

case class MaterialDeterminationReport(
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
    documents: Option[Seq[FileId]],
    documentsDetails: Seq[String],
    materialInfo: Option[Seq[MaterialInfo]],
    MaterialInfoDetails: Seq[MaterialInfoDetails],
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

  override def withActorRoleAndDates(actorsAndRoles: Option[Seq[ActorRoleDate]]) =
    copy(actorsAndRoles = actorsAndRoles)

  override def withDocuments(fileIds: Option[Seq[FileId]]): ConservationReportSubEvent =
    copy(documents = fileIds)

  def withOutSpesialMatrAndSorting: ConservationReportSubEvent =
    withSpesialMatrAndSorting(None)

  def withSpesialMatrAndSorting(materialInfo: Option[Seq[MaterialInfo]]) =
    copy(materialInfo = materialInfo)
}

object MaterialDeterminationReport
    extends WithDateTimeFormatters
    with ConservationSubEventType {
  val eventTypeId = EventTypeId(8)
  //val reads: Reads[MaterialDeterminationReport] = Json.reads[MaterialDeterminationReport]
  val writes: Writes[MaterialDeterminationReport] =
    Json.writes[MaterialDeterminationReport]
}

case class MeasurementDeterminationReport(
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
    documents: Option[Seq[FileId]],
    documentsDetails: Seq[String],
    measurementData: Option[MeasurementData],
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

  override def withActorRoleAndDates(actorsAndRoles: Option[Seq[ActorRoleDate]]) =
    copy(actorsAndRoles = actorsAndRoles)

  override def withDocuments(fileIds: Option[Seq[FileId]]): ConservationReportSubEvent =
    copy(documents = fileIds)

}

object MeasurementDeterminationReport
    extends WithDateTimeFormatters
    with ConservationSubEventType {
  val eventTypeId = EventTypeId(9)
  //val reads: Reads[MeasurementDeterminationReport] =
  //Json.reads[MeasurementDeterminationReport]
  val writes: Writes[MeasurementDeterminationReport] =
    Json.writes[MeasurementDeterminationReport]
}

case class NoteReport(
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
    documents: Option[Seq[FileId]],
    documentsDetails: Seq[String],
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
  ): ConservationReportSubEvent = copy(affectedThings = objects)

  override def withActorRoleAndDates(actorsAndRoles: Option[Seq[ActorRoleDate]]) =
    copy(actorsAndRoles = actorsAndRoles)

  override def withDocuments(fileIds: Option[Seq[FileId]]): ConservationReportSubEvent =
    copy(documents = fileIds)
}

object NoteReport extends WithDateTimeFormatters with ConservationSubEventType {
  val eventTypeId = EventTypeId(10)
  //  val reads: Reads[NoteReport] = Json.reads[NoteReport]
  val writes: Writes[NoteReport] = Json.writes[NoteReport]
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
    partOf: Option[EventId],
    note: Option[String],
    actorsAndRoles: Seq[ActorRoleDate],
    actorsAndRolesDetails: Seq[ActorRoleDateDetails],
    affectedThings: Seq[ObjectUUID],
    affectedThingsDetails: Seq[MusitObject],
    events: Seq[ConservationEvent],
    eventsDetails: Seq[ConservationReportSubEvent],
    isUpdated: Option[Boolean]
);

object ConservationProcessForReport extends WithDateTimeFormatters {
  val eventTypeId                      = EventTypeId(1)
  implicit val writesMusitObject       = MusitObject.writes
  val writesConservationReportSubEvent = ConservationReportSubEvent.writes
  implicit val writes: Writes[ConservationProcessForReport] =
    Json.writes[ConservationProcessForReport]
}
