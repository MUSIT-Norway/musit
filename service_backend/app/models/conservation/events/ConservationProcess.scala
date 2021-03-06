package models.conservation.events

import no.uio.musit.formatters.WithDateTimeFormatters
import no.uio.musit.models._
import org.joda.time.DateTime
import play.api.libs.json._

sealed trait ConservationModuleEvent extends ModernMusitEvent {

  val eventTypeId: EventTypeId
  val partOf: Option[EventId]
  val note: Option[String]
  def caseNumber: Option[String]
  val updatedBy: Option[ActorId]
  val actorsAndRoles: Option[Seq[ActorRoleDate]]
  val affectedThings: Option[Seq[ObjectUUID]]
  def withoutActorRoleAndDates: ConservationModuleEvent = withActorRoleAndDates(None)

  def withActorRoleAndDates(
      actorsAndRoles: Option[Seq[ActorRoleDate]]
  ): ConservationModuleEvent

}

trait TypedConservationEvent {
  protected val missingEventTypeMsg        = "No event type was specified"
  protected val discriminatorAttributeName = "eventTypeId"
}

case class EventIdWithEventTypeId(val eventId: EventId, eventTypeId: EventTypeId)

object ConservationModuleEvent extends TypedConservationEvent {

  /**
   * The implicit Reads for all events in the conservation module. It ensures that
   * the JSON message aligns with one of the types defined in
   * eventTypes in Conservation. If not the parsing will (and should) fail.
   */
  implicit val reads: Reads[ConservationModuleEvent] = Reads { jsv =>
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
  }

  /**
   * Implicit Writes for conservation module events. Ensures that each type
   * is written with their specific type discriminator. This ensure that the
   * JSON message is readable on the other end.
   */
  implicit val writes: Writes[ConservationModuleEvent] = Writes {
    case cpe: ConservationProcess =>
      ConservationProcess.writes.writes(cpe).as[JsObject] ++ Json.obj(
        discriminatorAttributeName -> ConservationProcess.eventTypeId
      )

    case te: Treatment =>
      Treatment.writes.writes(te).as[JsObject] ++ Json.obj(
        discriminatorAttributeName -> Treatment.eventTypeId
      )

    case tde: TechnicalDescription =>
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
      )
  }
}

/**
 * Describes the least common denominator for conservation events. Different
 * implementations may contain more fields than this trait.
 */
sealed trait ConservationEvent extends ConservationModuleEvent {
  val updatedBy: Option[ActorId]
  val updatedDate: Option[DateTime]
  val actorsAndRoles: Option[Seq[ActorRoleDate]]
  val affectedThings: Option[Seq[ObjectUUID]]
  val documents: Option[Seq[FileId]]
  val isUpdated: Option[Boolean]
  // todo val extraAttributes: Option[ExtraAttributes]

  //A new copy, appropriate when updating the event in the database.
  def withUpdatedInfo(
      updatedBy: Option[ActorId],
      updatedDate: Option[DateTime]
  ): ConservationEvent

  def asPartOf(partOf: Option[EventId]): ConservationEvent

  //A new copy, appropriate when adding/inserting the event in the database.
  def withRegisteredInfo(
      registeredBy: Option[ActorId],
      registeredDate: Option[DateTime]
  ): ConservationEvent

  def withUpdatedInfoEx(actorDate: ActorDate) =
    withUpdatedInfo(Some(actorDate.user), Some(actorDate.date))
  def withRegisteredInfoEx(actorDate: ActorDate) =
    withRegisteredInfo(Some(actorDate.user), Some(actorDate.date))

  def withAffectedThings(objects: Option[Seq[ObjectUUID]]): ConservationEvent

  def withDocuments(fileIds: Option[Seq[FileId]]): ConservationEvent

  override def withoutActorRoleAndDates: ConservationEvent = withActorRoleAndDates(None)

  def withActorRoleAndDates(actorsAndRoles: Option[Seq[ActorRoleDate]]): ConservationEvent

  override def caseNumber = throw new IllegalStateException(
    "Don't use caseNumber in subEvents"
  )

  def cleanupBeforeInsertIntoDatabase: ConservationEvent

}

object ConservationEvent extends TypedConservationEvent with WithDateTimeFormatters {

  override protected val missingEventTypeMsg =
    "Type must be a subevent of Conservation"

  val reads: Reads[ConservationEvent] = Reads { jsv =>
    implicit val _t    = Treatment.reads
    implicit val _td   = TechnicalDescription.reads
    implicit val _sah  = StorageAndHandling.reads
    implicit val _hse  = HseRiskAssessment.reads
    implicit val _ca   = ConditionAssessment.reads
    implicit val _re   = Report.reads
    implicit val _md   = MaterialDetermination.reads
    implicit val _msmd = MeasurementDetermination.reads
    implicit val _note = Note.reads

    (jsv \ discriminatorAttributeName).validateOpt[EventTypeId] match {
      case JsSuccess(maybeType, path) =>
        maybeType.map { eventTypeId =>
          ConservationEventType(eventTypeId) match {
            case Some(eventType) =>
              eventType match {
                case Treatment =>
                  jsv.validate[Treatment]

                case TechnicalDescription =>
                  jsv.validate[TechnicalDescription]

                case StorageAndHandling =>
                  jsv.validate[StorageAndHandling]

                case HseRiskAssessment =>
                  jsv.validate[HseRiskAssessment]

                case ConditionAssessment =>
                  jsv.validate[ConditionAssessment]
                case Report =>
                  jsv.validate[Report]
                case MaterialDetermination =>
                  jsv.validate[MaterialDetermination]
                case MeasurementDetermination =>
                  jsv.validate[MeasurementDetermination]
                case Note =>
                  jsv.validate[Note]
              }
            case None =>
              JsError(path, s"$eventTypeId is not a valid type. $missingEventTypeMsg")
          }
        }.getOrElse {
          JsError(path, missingEventTypeMsg)
        }

      case err: JsError =>
        err
    }
  }

  val writes: Writes[ConservationEvent] = Writes {
    case pres: Treatment =>
      ConservationEvent.writes.writes(pres).as[JsObject] ++
        Json.obj(discriminatorAttributeName -> Treatment.eventTypeId)
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
}

/**
 * Representation of typical events related to the analysis of museum objects.
 * The specific event types are encoded as {{{EventTypeId}}}.
 *
 *
 * Note: When you add a new subEvent type, you need to add a new ConservationSubEventType, to enable
 * the compiler to find the other places which needs modification when a new event type gets added
 */
case class ConservationProcess(
    id: Option[EventId],
    eventTypeId: EventTypeId,
    caseNumber: Option[String],
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    updatedBy: Option[ActorId],
    updatedDate: Option[DateTime],
    //completedBy: Option[ActorId],
    //completedDate: Option[DateTime],
    partOf: Option[EventId],
    note: Option[String],
    actorsAndRoles: Option[Seq[ActorRoleDate]],
    affectedThings: Option[Seq[ObjectUUID]],
    events: Option[Seq[ConservationEvent]],
    isUpdated: Option[Boolean]
) extends ConservationModuleEvent {
  // These fields are not relevant for the ConservationProcess type
  //override val affectedThing: Option[ObjectUUID] = None

  override def withId(id: Option[EventId]) = copy(id = id)

  def cleanupBeforeInsertIntoDatabase =
    copy(
      isUpdated = None,
      events = events.map(_.map(child => child.cleanupBeforeInsertIntoDatabase))
    )

  def withoutEvents = copy(events = None)

  def withUpdatedInfo(
      updatedBy: Option[ActorId],
      updatedDate: Option[DateTime]
  ) = copy(updatedBy = updatedBy, updatedDate = updatedDate)

  def withUpdatedInfoEx(actorDate: ActorDate) =
    withUpdatedInfo(Some(actorDate.user), Some(actorDate.date))
  def withRegisteredInfoEx(actorDate: ActorDate) =
    withRegisteredInfo(Some(actorDate.user), Some(actorDate.date))

  def withRegisteredInfo(
      registeredBy: Option[ActorId],
      registeredDate: Option[DateTime]
  ) = copy(registeredBy = registeredBy, registeredDate = registeredDate)

  def withEvents(newEvents: Seq[ConservationEvent]) = copy(events = Some(newEvents))

  override def withoutActorRoleAndDates: ConservationProcess = withActorRoleAndDates(None)

  override def withActorRoleAndDates(actorsAndRoles: Option[Seq[ActorRoleDate]]) =
    copy(actorsAndRoles = actorsAndRoles)

  def withoutAfftectedThings = copy(affectedThings = None)

  def withAffectedThings(objects: Option[Seq[ObjectUUID]]): ConservationProcess =
    copy(affectedThings = objects)

}
object ConservationProcess extends WithDateTimeFormatters {
  val eventTypeId = EventTypeId(1)

  implicit val readsConsEvents: Reads[Seq[ConservationEvent]] =
    Reads.seq(ConservationEvent.reads)

  // The below formatters cannot be implicit due to undesirable implicit ambiguities
  val reads: Reads[ConservationProcess]   = Json.reads[ConservationProcess]
  val writes: Writes[ConservationProcess] = Json.writes[ConservationProcess]

}

sealed trait ConservationEventType {
  val eventTypeId: EventTypeId
}

object ConservationEventType {
  def apply(eventTypeId: EventTypeId): Option[ConservationEventType] = {
    eventTypeId match {
      case Treatment.eventTypeId                => Some(Treatment)
      case TechnicalDescription.eventTypeId     => Some(TechnicalDescription)
      case StorageAndHandling.eventTypeId       => Some(StorageAndHandling)
      case HseRiskAssessment.eventTypeId        => Some(HseRiskAssessment)
      case ConditionAssessment.eventTypeId      => Some(ConditionAssessment)
      case Report.eventTypeId                   => Some(Report)
      case MaterialDetermination.eventTypeId    => Some(MaterialDetermination)
      case MeasurementDetermination.eventTypeId => Some(MeasurementDetermination)
      case Note.eventTypeId                     => Some(Note)
      case _                                    => None
    }
  }
  def mustFind(eventTypeId: EventTypeId): ConservationEventType = {
    apply(eventTypeId) match {
      case Some(t) => t
      case None =>
        throw new IllegalStateException(
          s"Unknown/unhandled eventTypeId: $eventTypeId in ConservationProcess.ConservationProcess.subEventTypeIdToConservationEventType"
        )
    }
  }
}

case class Treatment(
    id: Option[EventId],
    eventTypeId: EventTypeId,
    registeredBy: Option[ActorId],
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
) extends ConservationEvent {

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

  override def withAffectedThings(objects: Option[Seq[ObjectUUID]]): ConservationEvent =
    copy(affectedThings = objects)

  override def withDocuments(fileIds: Option[Seq[FileId]]): ConservationEvent =
    copy(documents = fileIds)

  override def withActorRoleAndDates(actorsAndRoles: Option[Seq[ActorRoleDate]]) =
    copy(actorsAndRoles = actorsAndRoles)

}

object Treatment extends WithDateTimeFormatters with ConservationEventType {
  val eventTypeId = EventTypeId(2)

  val reads: Reads[Treatment]   = Json.reads[Treatment]
  val writes: Writes[Treatment] = Json.writes[Treatment]

}

/**
 * Representation of typical subevent related to a conservationProcess of museum objects.
 * The specific event types are encoded as {{{EventTypeId}}}.
 */
case class TechnicalDescription(
    id: Option[EventId],
    eventTypeId: EventTypeId,
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    updatedBy: Option[ActorId],
    updatedDate: Option[DateTime],
    //completedBy: Option[ActorId],
    //completedDate: Option[DateTime],
    partOf: Option[EventId],
    note: Option[String],
    actorsAndRoles: Option[Seq[ActorRoleDate]],
    affectedThings: Option[Seq[ObjectUUID]],
    documents: Option[Seq[FileId]],
    isUpdated: Option[Boolean]
) extends ConservationEvent {

  override def cleanupBeforeInsertIntoDatabase = copy(isUpdated = None)
  // These fields are not relevant for the ConservationProcess type
  //override val affectedThing: Option[ObjectUUID] = None

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

  override def withAffectedThings(objects: Option[Seq[ObjectUUID]]): ConservationEvent =
    copy(affectedThings = objects)

  override def withActorRoleAndDates(actorsAndRoles: Option[Seq[ActorRoleDate]]) =
    copy(actorsAndRoles = actorsAndRoles)

  override def withDocuments(fileIds: Option[Seq[FileId]]): ConservationEvent =
    copy(documents = fileIds)

}

object TechnicalDescription extends WithDateTimeFormatters with ConservationEventType {
  val eventTypeId = EventTypeId(3)
  // The below formatters cannot be implicit due to undesirable implicit ambiguities
  val reads: Reads[TechnicalDescription]   = Json.reads[TechnicalDescription]
  val writes: Writes[TechnicalDescription] = Json.writes[TechnicalDescription]

}

/**
 * Representation of a typical subevent related to a conservationProcess of museum objects.
 * The specific event types are encoded as {{{EventTypeId}}}.
 */
case class StorageAndHandling(
    id: Option[EventId],
    eventTypeId: EventTypeId,
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    updatedBy: Option[ActorId],
    updatedDate: Option[DateTime],
    // completedBy: Option[ActorId],
    // completedDate: Option[DateTime],
    partOf: Option[EventId],
    note: Option[String],
    lightLevel: Option[String],
    uvLevel: Option[String],
    relativeHumidity: Option[String],
    temperature: Option[String],
    actorsAndRoles: Option[Seq[ActorRoleDate]],
    affectedThings: Option[Seq[ObjectUUID]],
    documents: Option[Seq[FileId]],
    isUpdated: Option[Boolean]
) extends ConservationEvent {

  override def cleanupBeforeInsertIntoDatabase = copy(isUpdated = None)
  // These fields are not relevant for the ConservationProcess type
  //override val affectedThing: Option[ObjectUUID] = None

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

  override def withAffectedThings(objects: Option[Seq[ObjectUUID]]): ConservationEvent =
    copy(affectedThings = objects)

  override def withActorRoleAndDates(actorsAndRoles: Option[Seq[ActorRoleDate]]) =
    copy(actorsAndRoles = actorsAndRoles)

  override def withDocuments(fileIds: Option[Seq[FileId]]): ConservationEvent =
    copy(documents = fileIds)

}

object StorageAndHandling extends WithDateTimeFormatters with ConservationEventType {
  val eventTypeId = EventTypeId(4)
  // The below formatters cannot be implicit due to undesirable implicit ambiguities
  val reads: Reads[StorageAndHandling]   = Json.reads[StorageAndHandling]
  val writes: Writes[StorageAndHandling] = Json.writes[StorageAndHandling]

}

/**
 * Representation of a typical subevent related to a conservationProcess of museum objects.
 * The specific event types are encoded as {{{EventTypeId}}}.
 */
case class HseRiskAssessment(
    id: Option[EventId],
    eventTypeId: EventTypeId,
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    updatedBy: Option[ActorId],
    updatedDate: Option[DateTime],
    // completedBy: Option[ActorId],
    // completedDate: Option[DateTime],
    partOf: Option[EventId],
    note: Option[String],
    actorsAndRoles: Option[Seq[ActorRoleDate]],
    affectedThings: Option[Seq[ObjectUUID]],
    documents: Option[Seq[FileId]],
    isUpdated: Option[Boolean]
) extends ConservationEvent {

  override def cleanupBeforeInsertIntoDatabase = copy(isUpdated = None)
  // These fields are not relevant for the ConservationProcess type
  //override val affectedThing: Option[ObjectUUID] = None

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

  override def withAffectedThings(objects: Option[Seq[ObjectUUID]]): ConservationEvent =
    copy(affectedThings = objects)

  override def withActorRoleAndDates(actorsAndRoles: Option[Seq[ActorRoleDate]]) =
    copy(actorsAndRoles = actorsAndRoles)

  override def withDocuments(fileIds: Option[Seq[FileId]]): ConservationEvent =
    copy(documents = fileIds)

}

object HseRiskAssessment extends WithDateTimeFormatters with ConservationEventType {
  val eventTypeId = EventTypeId(5)
  // The below formatters cannot be implicit due to undesirable implicit ambiguities
  val reads: Reads[HseRiskAssessment]   = Json.reads[HseRiskAssessment]
  val writes: Writes[HseRiskAssessment] = Json.writes[HseRiskAssessment]

}

/**
 * Representation of a typical subevent related to a conservationProcess of museum objects.
 * The specific event types are encoded as {{{EventTypeId}}}.
 */
case class ConditionAssessment(
    id: Option[EventId],
    eventTypeId: EventTypeId,
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    updatedBy: Option[ActorId],
    updatedDate: Option[DateTime],
    //completedBy: Option[ActorId],
    //completedDate: Option[DateTime],
    partOf: Option[EventId],
    note: Option[String],
    conditionCode: Option[Int],
    actorsAndRoles: Option[Seq[ActorRoleDate]],
    affectedThings: Option[Seq[ObjectUUID]],
    documents: Option[Seq[FileId]],
    isUpdated: Option[Boolean]
) extends ConservationEvent {

  override def cleanupBeforeInsertIntoDatabase = copy(isUpdated = None)
  // These fields are not relevant for the ConservationProcess type
  //override val affectedThing: Option[ObjectUUID] = None

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

  override def withAffectedThings(objects: Option[Seq[ObjectUUID]]): ConservationEvent =
    copy(affectedThings = objects)

  override def withActorRoleAndDates(actorsAndRoles: Option[Seq[ActorRoleDate]]) =
    copy(actorsAndRoles = actorsAndRoles)

  override def withDocuments(fileIds: Option[Seq[FileId]]): ConservationEvent =
    copy(documents = fileIds)

}

object ConditionAssessment extends WithDateTimeFormatters with ConservationEventType {
  val eventTypeId = EventTypeId(6)
  // The below formatters cannot be implicit due to undesirable implicit ambiguities
  val reads: Reads[ConditionAssessment]   = Json.reads[ConditionAssessment]
  val writes: Writes[ConditionAssessment] = Json.writes[ConditionAssessment]

}

/**
 * Representation of a typical subevent related to a conservationProcess of museum objects.
 * The specific event types are encoded as {{{EventTypeId}}}.
 */
case class Report(
    id: Option[EventId],
    eventTypeId: EventTypeId,
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    updatedBy: Option[ActorId],
    updatedDate: Option[DateTime],
    //completedBy: Option[ActorId],
    //completedDate: Option[DateTime],
    partOf: Option[EventId],
    note: Option[String],
    actorsAndRoles: Option[Seq[ActorRoleDate]],
    affectedThings: Option[Seq[ObjectUUID]],
    documents: Option[Seq[FileId]],
    archiveReference: Option[String],
    isUpdated: Option[Boolean]
) extends ConservationEvent {

  override def cleanupBeforeInsertIntoDatabase = copy(isUpdated = None)
  // These fields are not relevant for the ConservationProcess type
  //override val affectedThing: Option[ObjectUUID] = None

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

  override def withAffectedThings(objects: Option[Seq[ObjectUUID]]): ConservationEvent =
    copy(affectedThings = objects)

  override def withActorRoleAndDates(actorsAndRoles: Option[Seq[ActorRoleDate]]) =
    copy(actorsAndRoles = actorsAndRoles)

  override def withDocuments(fileIds: Option[Seq[FileId]]): ConservationEvent =
    copy(documents = fileIds)

}

object Report extends WithDateTimeFormatters with ConservationEventType {
  val eventTypeId = EventTypeId(7)
  // The below formatters cannot be implicit due to undesirable implicit ambiguities
  val reads: Reads[Report]   = Json.reads[Report]
  val writes: Writes[Report] = Json.writes[Report]

}

case class MaterialInfo(
    materialId: Int,
    materialExtra: Option[String],
    sorting: Option[Int]
)

object MaterialInfo {
  implicit val format: Format[MaterialInfo] =
    Json.format[MaterialInfo]
}

/**
 * Representation of a typical subevent related to a conservationProcess of museum objects.
 * The specific event types are encoded as {{{EventTypeId}}}.
 */
case class MaterialDetermination(
    id: Option[EventId],
    eventTypeId: EventTypeId,
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    updatedBy: Option[ActorId],
    updatedDate: Option[DateTime],
    partOf: Option[EventId],
    note: Option[String],
    actorsAndRoles: Option[Seq[ActorRoleDate]],
    affectedThings: Option[Seq[ObjectUUID]],
    documents: Option[Seq[FileId]],
    materialInfo: Option[Seq[MaterialInfo]],
    isUpdated: Option[Boolean]
) extends ConservationEvent {

  override def cleanupBeforeInsertIntoDatabase = copy(isUpdated = None)
  // These fields are not relevant for the ConservationProcess type
  //override val affectedThing: Option[ObjectUUID] = None

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

  override def withAffectedThings(objects: Option[Seq[ObjectUUID]]): ConservationEvent =
    copy(affectedThings = objects)

  override def withActorRoleAndDates(actorsAndRoles: Option[Seq[ActorRoleDate]]) =
    copy(actorsAndRoles = actorsAndRoles)

  override def withDocuments(fileIds: Option[Seq[FileId]]): ConservationEvent =
    copy(documents = fileIds)

  def withOutSpesialMatrAndSorting: ConservationEvent = withSpesialMatrAndSorting(None)

  def withSpesialMatrAndSorting(materialInfo: Option[Seq[MaterialInfo]]) =
    copy(materialInfo = materialInfo)
}

object MaterialDetermination extends WithDateTimeFormatters with ConservationEventType {
  val eventTypeId = EventTypeId(8)
  // The below formatters cannot be implicit due to undesirable implicit ambiguities
  val reads: Reads[MaterialDetermination]   = Json.reads[MaterialDetermination]
  val writes: Writes[MaterialDetermination] = Json.writes[MaterialDetermination]

}

case class MeasurementData(
    weight: Option[Double],
    length: Option[Double],
    width: Option[Double],
    thickness: Option[Double],
    height: Option[Double],
    largestLength: Option[Double],
    largestWidth: Option[Double],
    largestThickness: Option[Double],
    largestHeight: Option[Double],
    diameter: Option[Double],
    tverrmaal: Option[Double],
    largestMeasurement: Option[Double],
    measurement: Option[String],
    quantity: Option[Int],
    quantitySymbol: Option[String],
    fragmentQuantity: Option[Int]
)

object MeasurementData {
  implicit val format: Format[MeasurementData] =
    Json.format[MeasurementData]
}

case class MeasurementDetermination(
    id: Option[EventId],
    eventTypeId: EventTypeId,
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    updatedBy: Option[ActorId],
    updatedDate: Option[DateTime],
    //completedBy: Option[ActorId],
    //completedDate: Option[DateTime],
    partOf: Option[EventId],
    note: Option[String],
    actorsAndRoles: Option[Seq[ActorRoleDate]],
    affectedThings: Option[Seq[ObjectUUID]],
    documents: Option[Seq[FileId]],
    measurementData: Option[MeasurementData],
    isUpdated: Option[Boolean]
) extends ConservationEvent {

  override def cleanupBeforeInsertIntoDatabase = copy(isUpdated = None)
  // These fields are not relevant for the ConservationProcess type
  //override val affectedThing: Option[ObjectUUID] = None

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

  override def withAffectedThings(objects: Option[Seq[ObjectUUID]]): ConservationEvent =
    copy(affectedThings = objects)

  override def withActorRoleAndDates(actorsAndRoles: Option[Seq[ActorRoleDate]]) =
    copy(actorsAndRoles = actorsAndRoles)

  override def withDocuments(fileIds: Option[Seq[FileId]]): ConservationEvent =
    copy(documents = fileIds)

}

object MeasurementDetermination
    extends WithDateTimeFormatters
    with ConservationEventType {
  val eventTypeId = EventTypeId(9)
  // The below formatters cannot be implicit due to undesirable implicit ambiguities
  val reads: Reads[MeasurementDetermination] =
    Json.reads[MeasurementDetermination]
  val writes: Writes[MeasurementDetermination] =
    Json.writes[MeasurementDetermination]

}

case class Note(
    id: Option[EventId],
    eventTypeId: EventTypeId,
    registeredBy: Option[ActorId],
    registeredDate: Option[DateTime],
    updatedBy: Option[ActorId],
    updatedDate: Option[DateTime],
    //completedBy: Option[ActorId],
    //completedDate: Option[DateTime],
    partOf: Option[EventId],
    note: Option[String],
    actorsAndRoles: Option[Seq[ActorRoleDate]],
    affectedThings: Option[Seq[ObjectUUID]],
    documents: Option[Seq[FileId]],
    isUpdated: Option[Boolean]
) extends ConservationEvent {

  override def cleanupBeforeInsertIntoDatabase = copy(isUpdated = None)
  // These fields are not relevant for the ConservationProcess type
  //override val affectedThing: Option[ObjectUUID] = None

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

  override def withAffectedThings(objects: Option[Seq[ObjectUUID]]): ConservationEvent =
    copy(affectedThings = objects)

  override def withActorRoleAndDates(actorsAndRoles: Option[Seq[ActorRoleDate]]) =
    copy(actorsAndRoles = actorsAndRoles)

  override def withDocuments(fileIds: Option[Seq[FileId]]): ConservationEvent =
    copy(documents = fileIds)

}

object Note extends WithDateTimeFormatters with ConservationEventType {
  val eventTypeId = EventTypeId(10)
  // The below formatters cannot be implicit due to undesirable implicit ambiguities
  val reads: Reads[Note] =
    Json.reads[Note]
  val writes: Writes[Note] =
    Json.writes[Note]

}
