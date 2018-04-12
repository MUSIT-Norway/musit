package services.conservation

import models.conservation.{
  ConditionCode,
  ConservationProcessKeyData,
  TreatmentKeyword,
  TreatmentMaterial
}
import com.google.inject.Inject
import models.conservation.events._
import no.uio.musit.MusitResults
import no.uio.musit.MusitResults._
import no.uio.musit.functional.Extensions._
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.models._
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.time.dateTimeNow
import org.joda.time.DateTime
import play.api.Logger
import repositories.conservation.dao._
import services.conservation.EventSituation.{
  EventSituation,
  Insert,
  PreserveDates,
  UpdateSelf
}
import no.uio.musit.security.CollectionManagement

import scala.concurrent.{ExecutionContext, Future}
import services.musitobject.ObjectService
import services.actor.ActorService
import controllers._
import controllers.conservation.DocumentMetadataController
import models.actor.Person
import models.musitobject.MusitObject
import play.api.libs.json.Json
import play.api.Configuration
//import DocumentArchiveService
//import net.scalytica.symbiotic.core.DocManagementService

class ConservationProcessService @Inject()(
    implicit
    val conservationProcDao: ConservationProcessDao,
    val typeDao: ConservationTypeDao,
    val dao: ConservationDao,
    val objectDao: ObjectEventDao,
    val subEventDao: TreatmentDao, //Arbitrary choice, to get access to helper functions irrespective of event type
    //Should have been ConservationModuleEventDao (TODO: Make this split)
    val conservationService: ConservationService,
    val ec: ExecutionContext,
    val configuration: Configuration,
    val objService: ObjectService,
    val actorService: ActorService,
    val treatmentService: TreatmentService,
    val conditionAssessmentService: ConditionAssessmentService,
    val materialDeterminationService: MaterialDeterminationService,
    val measurementDeterminationService: MeasurementDeterminationService,
    val documentMetadataService: DocumentMetadataService
    //  val dmService: DocManagementService
) {

  val logger = Logger(classOf[ConservationProcessService])

  def getTypesFor(coll: Option[CollectionUUID])(
      implicit currUser: AuthenticatedUser
  ): FutureMusitResult[Seq[ConservationType]] = {
    typeDao.allFor(coll)
  }

  //Only used from the Elastic Search indexing.
  def getAllEventTypes() = typeDao.allEventTypes()

  def addConservationProcess(
      mid: MuseumId,
      cp: ConservationProcess
  )(implicit currUser: AuthenticatedUser): FutureMusitResult[EventId] = {
    val event =
      fillInAppropriateActorDatesAndExcludeNonUpdatedSubEvents(
        mid,
        cp,
        ActorDate(currUser.id, dateTimeNow),
        true
      ).map(_.cleanupBeforeInsertIntoDatabase)
    event.flatMap { ev =>
      conservationService
        .checkTypeOfObjects(cp.affectedThings.getOrElse(Seq.empty))
        .flatMap(m => conservationProcDao.insert(mid, ev))
    }
  }

  /**
   * Locate an event with the given EventId.
   */
  def findConservationProcessById(
      mid: MuseumId,
      id: EventId
  )(
      implicit currUser: AuthenticatedUser
  ): FutureMusitResult[Option[ConservationProcess]] = {

    def findSubEvent(v: EventIdWithEventTypeId) =
      conservationProcDao.readSubEvent(v.eventTypeId, mid, v.eventId)

    val futOptCp = conservationProcDao.findConservationProcessIgnoreSubEvents(mid, id)
    futOptCp.flatMapInsideOption { cp =>
      for {
        childrenIds <- conservationProcDao.listSubEventIdsWithTypes(mid, id)
        subEvents <- FutureMusitResult.collectAllOrFail(
                      childrenIds,
                      findSubEvent,
                      (failedIdWithTypes: Seq[EventIdWithEventTypeId]) =>
                        MusitValidationError(
                          s"unable to find subevents with ids and types: $failedIdWithTypes of event with id: $id"
                      )
                    )
      } yield cp.copy(events = Some(subEvents))
    }
  }

  /**
   * Get the person name details
   */
  def getPersonName(actor: Option[ActorId]): FutureMusitResult[Option[String]] = {
    val ofoPerson =
      actor.map(actorId => actorService.findByActorId(actorId))
    val foPerson = ofoPerson.getOrElse(Future.successful(None))
    val fPersonName =
      foPerson.map(person => person.map(_.fn))
    FutureMusitResult(fPersonName.map(MusitSuccess(_)))
  }

  def conservationReportFromConservationProcess(
      process: ConservationProcess,
      mid: MuseumId,
      colId: Seq[MuseumCollection],
      maybeColl: Option[CollectionUUID]
  )(
      implicit currUser: AuthenticatedUser
  ): FutureMusitResult[ConservationProcessForReport] = {

    // get all Conservation event types
    val fmrConservationTypes = typeDao.allFor(maybeColl)

    // affectedThingsDetails for main event
    val fmrEventsDetails = getSubEventDetails(process, fmrConservationTypes, mid, colId)

    val obj =
      for {
        affectedThingsDetails <- getObjectDetails(process.affectedThings, mid, colId)
        maybeMainEventType    <- getEventType(fmrConservationTypes, process.eventTypeId)
        updatedByName         <- getPersonName(process.updatedBy)
        registeredByName      <- getPersonName(process.registeredBy)
        eventsDetails         <- fmrEventsDetails
        actorsAndRoles        <- getActorsAndRoles(process.actorsAndRoles.getOrElse(Seq.empty))
      } yield
        ConservationProcessForReport(
          id = process.id,
          eventTypeId = process.eventTypeId,
          eventType = maybeMainEventType,
          caseNumber = process.caseNumber,
          registeredBy = process.registeredBy,
          registeredByName = registeredByName,
          registeredDate = process.registeredDate,
          updatedBy = process.updatedBy,
          updatedByName = updatedByName,
          updatedDate = process.updatedDate,
          partOf = process.partOf,
          note = process.note,
          actorsAndRoles = process.actorsAndRoles.getOrElse(Seq.empty),
          actorsAndRolesDetails = actorsAndRoles,
          affectedThings = process.affectedThings.getOrElse(Seq.empty),
          affectedThingsDetails = affectedThingsDetails,
          events = process.events.getOrElse(Seq.empty),
          eventsDetails = eventsDetails, //Seq.empty[ConservationSubEvent],
          isUpdated = process.isUpdated
        )
    obj
  }

  private def getObjectDetails(
      affectedThings: Option[Seq[ObjectUUID]],
      mid: MuseumId,
      colId: Seq[MuseumCollection]
  )(
      implicit currUser: AuthenticatedUser
  ): FutureMusitResult[Seq[MusitObject]] = {
    def localFindByUUID(o: ObjectUUID) =
      FutureMusitResult(objService.findByUUID(mid, o, colId))

    val ids = affectedThings.getOrElse(Seq.empty)
    FutureMusitResult.collectAllOrFail[ObjectUUID, MusitObject](
      ids,
      localFindByUUID,
      objectIds => MusitValidationError(s"Missing objects for these objectIds:$objectIds")
    )
  }

  private def getEventType(
      fmrConservationTypes: FutureMusitResult[Seq[ConservationType]],
      eventTypeId: EventTypeId
  ): FutureMusitResult[Option[ConservationType]] = {
    for {
      conservationTypes <- fmrConservationTypes
      maybeMainEventType = conservationTypes.find(t => t.id == eventTypeId)
    } yield maybeMainEventType
  }

  private def getConditionCode(
      conditionCode: Int
  ): FutureMusitResult[Option[ConditionCode]] = {
    val fmrConditionCodeList = conditionAssessmentService.getConditionCodeList
    for {
      conditionCodeList <- fmrConditionCodeList
      conditionCodeDetail = conditionCodeList.find(t => t.conditionCode == conditionCode)
    } yield conditionCodeDetail
  }

  private def getKeywordsDetails(
      keywords: Option[scala.Seq[Int]]
  ): FutureMusitResult[Seq[TreatmentKeyword]] = {
    for {
      keywordsList <- treatmentService.getKeywordList
      keywordsDetails = keywordsList.filter(
        t => keywords.getOrElse(Seq.empty).contains(t.id)
      )
    } yield keywordsDetails
  }

  private def getMaterialsDetails(
      materials: Option[scala.Seq[Int]]
  ): FutureMusitResult[Seq[TreatmentMaterial]] = {
    for {
      materialsList <- treatmentService.getMaterialList
      materialsDetails = materialsList.filter(
        t => materials.getOrElse(Seq.empty).contains(t.id)
      )
    } yield materialsDetails
  }

  private def getActorsAndRoles(
      actorsAndRoles: Seq[ActorRoleDate]
  ): FutureMusitResult[Seq[ActorRoleDateDetails]] = {
    val roleList = conservationService.getRoleList
    val obj = FutureMusitResult.sequence(
      actorsAndRoles.map(
        ar =>
          for {
            actor <- getPersonName(Some(ar.actorId))
            roles <- roleList
            role = roles.find(t => t.roleId == ar.roleId)

          } yield
            ActorRoleDateDetails(
              roleId = ar.roleId,
              role = role,
              actorId = ar.actorId,
              actor = actor,
              date = ar.date
          )
      )
    )
    obj
  }

  private def getDocumentsDetails(
      documents: Seq[FileId],
      mid: MuseumId
  )(
      implicit currUser: AuthenticatedUser
  ): FutureMusitResult[Seq[String]] = {
    logger.debug("ConservationProcessService.getDocumentsDetails")
    println("ConservationProcessService.getDocumentsDetails")
    val fileIds = documents.map(x => x.underlying.toString)
    logger.debug(
      "ConservationProcessService.getDocumentsDetails.fileIds: " + fileIds.mkString(",")
    )

    val filenames =
      if (fileIds.length > 0) {
        FutureMusitResult(
          documentMetadataService.getFilenames(mid, fileIds, currUser)
        )
      } else {
        FutureMusitResult(Future(MusitSuccess(Seq(""))))
      }

    println("getDocumentsDetails after filenames")
    filenames
//    val filenames = documents.map { fileId =>
//      FutureMusitResult(
//        documentMetadataService.getFilename(mid, fileId.underlying.toString, currUser)
//      )
//    }
//    FutureMusitResult.sequence(filenames)
  }

  private def getSubEventDetails(
      process: ConservationProcess,
      fmrConservationTypes: FutureMusitResult[Seq[ConservationType]],
      mid: MuseumId,
      colId: Seq[MuseumCollection]
  )(
      implicit currUser: AuthenticatedUser
  ): FutureMusitResult[Seq[ConservationReportSubEvent]] = {
    FutureMusitResult.sequence(
      process.events
        .getOrElse(Seq.empty)
        .map(
          e => {

            /* val documentsDetails = dmService.file(e.documents).map {
              case Some(ad) => MusitSuccess(ad)
              case None     => MusitNotFound(s"Could not find ArchiveDocument $fileId")
            }*/

            val result = for {
              subEventType          <- getEventType(fmrConservationTypes, e.eventTypeId)
              registeredByName      <- getPersonName(e.registeredBy)
              updatedByName         <- getPersonName(e.updatedBy)
              affectedThingsDetails <- getObjectDetails(e.affectedThings, mid, colId)
              actorsAndRoles        <- getActorsAndRoles(e.actorsAndRoles.getOrElse(Seq.empty))
              documentsDetails <- getDocumentsDetails(
                                   e.documents.getOrElse(Seq.empty),
                                   mid
                                 )
            } yield
              e.eventTypeId match {
                case Treatment.eventTypeId =>
                  getTreatmentReport(
                    e,
                    subEventType,
                    registeredByName,
                    updatedByName,
                    affectedThingsDetails,
                    actorsAndRoles,
                    documentsDetails
                  )
                case TechnicalDescription.eventTypeId =>
                  getTechnicalDescriptionReport(
                    e,
                    subEventType,
                    registeredByName,
                    updatedByName,
                    affectedThingsDetails,
                    actorsAndRoles,
                    documentsDetails
                  )
                case StorageAndHandling.eventTypeId =>
                  getStorageAndHandlingReport(
                    e,
                    subEventType,
                    registeredByName,
                    updatedByName,
                    affectedThingsDetails,
                    actorsAndRoles,
                    documentsDetails
                  )
                case HseRiskAssessment.eventTypeId =>
                  getHseRiskAssessmentReport(
                    e,
                    subEventType,
                    registeredByName,
                    updatedByName,
                    affectedThingsDetails,
                    actorsAndRoles,
                    documentsDetails
                  )
                case ConditionAssessment.eventTypeId =>
                  getConditionAssessmentReport(
                    e,
                    subEventType,
                    registeredByName,
                    updatedByName,
                    affectedThingsDetails,
                    actorsAndRoles,
                    documentsDetails
                  )
                case Report.eventTypeId =>
                  getReportReport(
                    e,
                    subEventType,
                    registeredByName,
                    updatedByName,
                    affectedThingsDetails,
                    actorsAndRoles,
                    documentsDetails
                  )
                case MaterialDetermination.eventTypeId =>
                  getMaterialDeterminationReport(
                    e,
                    subEventType,
                    registeredByName,
                    updatedByName,
                    affectedThingsDetails,
                    actorsAndRoles,
                    documentsDetails
                  )
                case MeasurementDetermination.eventTypeId =>
                  getMeasurementDeterminationReport(
                    e,
                    subEventType,
                    registeredByName,
                    updatedByName,
                    affectedThingsDetails,
                    actorsAndRoles,
                    documentsDetails
                  )
                case Note.eventTypeId =>
                  getNoteReport(
                    e,
                    subEventType,
                    registeredByName,
                    updatedByName,
                    affectedThingsDetails,
                    actorsAndRoles,
                    documentsDetails
                  )
              }
            //result.flatMap(identity)
            FutureMusitResult.flatten(result)
          }
        )
    )
  }

  private def getNoteReport(
      e: ConservationEvent,
      subEventType: Option[ConservationType],
      registeredByName: Option[String],
      updatedByName: Option[String],
      affectedThingsDetails: Seq[MusitObject],
      actorsAndRoles: Seq[ActorRoleDateDetails],
      documentsDetails: Seq[String]
  ): FutureMusitResult[ConservationReportSubEvent] = {
    FutureMusitResult.successful(
      NoteReport(
        id = e.id,
        eventTypeId = e.eventTypeId,
        eventType = subEventType,
        registeredBy = e.registeredBy,
        registeredByName = registeredByName,
        registeredDate = e.registeredDate,
        updatedBy = e.updatedBy,
        updatedByName = updatedByName,
        updatedDate = e.updatedDate,
        partOf = e.partOf,
        note = e.note,
        actorsAndRoles = e.actorsAndRoles,
        actorsAndRolesDetails = actorsAndRoles,
        affectedThings = e.affectedThings,
        affectedThingsDetails = affectedThingsDetails,
        documents = e.documents,
        documentsDetails = documentsDetails,
        isUpdated = e.isUpdated
      ).asInstanceOf[ConservationReportSubEvent]
    )
  }

  private def getMeasurementDeterminationReport(
      e: ConservationEvent,
      subEventType: Option[ConservationType],
      registeredByName: Option[String],
      updatedByName: Option[String],
      affectedThingsDetails: Seq[MusitObject],
      actorsAndRoles: Seq[ActorRoleDateDetails],
      documentsDetails: Seq[String]
  ): FutureMusitResult[ConservationReportSubEvent] = {
    FutureMusitResult.successful(
      MeasurementDeterminationReport(
        id = e.id,
        eventTypeId = e.eventTypeId,
        eventType = subEventType,
        registeredBy = e.registeredBy,
        registeredByName = registeredByName,
        registeredDate = e.registeredDate,
        updatedBy = e.updatedBy,
        updatedByName = updatedByName,
        updatedDate = e.updatedDate,
        partOf = e.partOf,
        note = e.note,
        actorsAndRoles = e.actorsAndRoles,
        actorsAndRolesDetails = actorsAndRoles,
        affectedThings = e.affectedThings,
        affectedThingsDetails = affectedThingsDetails,
        measurementData = e.asInstanceOf[MeasurementDetermination].measurementData,
        documents = e.documents,
        documentsDetails = documentsDetails,
        isUpdated = e.isUpdated
      ).asInstanceOf[ConservationReportSubEvent]
    )
  }

  private def getMaterialDeterminationReport(
      e: ConservationEvent,
      subEventType: Option[ConservationType],
      registeredByName: Option[String],
      updatedByName: Option[String],
      affectedThingsDetails: Seq[MusitObject],
      actorsAndRoles: Seq[ActorRoleDateDetails],
      documentsDetails: Seq[String]
  ): FutureMusitResult[ConservationReportSubEvent] = {
    FutureMusitResult.successful(
      MaterialDeterminationReport(
        id = e.id,
        eventTypeId = e.eventTypeId,
        eventType = subEventType,
        registeredBy = e.registeredBy,
        registeredByName = registeredByName,
        registeredDate = e.registeredDate,
        updatedBy = e.updatedBy,
        updatedByName = updatedByName,
        updatedDate = e.updatedDate,
        partOf = e.partOf,
        note = e.note,
        actorsAndRoles = e.actorsAndRoles,
        actorsAndRolesDetails = actorsAndRoles,
        affectedThings = e.affectedThings,
        affectedThingsDetails = affectedThingsDetails,
        materialInfo = e.asInstanceOf[MaterialDetermination].materialInfo,
        MaterialInfoDetails = Seq.empty,
        documents = e.documents,
        documentsDetails = documentsDetails,
        isUpdated = e.isUpdated
      ).asInstanceOf[ConservationReportSubEvent]
    )
  }

  private def getReportReport(
      e: ConservationEvent,
      subEventType: Option[ConservationType],
      registeredByName: Option[String],
      updatedByName: Option[String],
      affectedThingsDetails: Seq[MusitObject],
      actorsAndRoles: Seq[ActorRoleDateDetails],
      documentsDetails: Seq[String]
  ): FutureMusitResult[ConservationReportSubEvent] = {
    FutureMusitResult.successful(
      ReportReport(
        id = e.id,
        eventTypeId = e.eventTypeId,
        eventType = subEventType,
        registeredBy = e.registeredBy,
        registeredByName = registeredByName,
        registeredDate = e.registeredDate,
        updatedBy = e.updatedBy,
        updatedByName = updatedByName,
        updatedDate = e.updatedDate,
        partOf = e.partOf,
        note = e.note,
        actorsAndRoles = e.actorsAndRoles,
        actorsAndRolesDetails = actorsAndRoles,
        affectedThings = e.affectedThings,
        affectedThingsDetails = affectedThingsDetails,
        archiveReference = e.asInstanceOf[Report].archiveReference,
        documents = e.documents,
        documentsDetails = documentsDetails,
        isUpdated = e.isUpdated
      ).asInstanceOf[ConservationReportSubEvent]
    )
  }

  private def getConditionAssessmentReport(
      e: ConservationEvent,
      subEventType: Option[ConservationType],
      registeredByName: Option[String],
      updatedByName: Option[String],
      affectedThingsDetails: Seq[MusitObject],
      actorsAndRoles: Seq[ActorRoleDateDetails],
      documentsDetails: Seq[String]
  ): FutureMusitResult[ConservationReportSubEvent] = {

    for {
      conditionCode <- getConditionCode(
                        e.asInstanceOf[ConditionAssessment].conditionCode.getOrElse(0)
                      )
    } yield
      ConditionAssessmentReport(
        id = e.id,
        eventTypeId = e.eventTypeId,
        eventType = subEventType,
        registeredBy = e.registeredBy,
        registeredByName = registeredByName,
        registeredDate = e.registeredDate,
        updatedBy = e.updatedBy,
        updatedByName = updatedByName,
        updatedDate = e.updatedDate,
        partOf = e.partOf,
        note = e.note,
        actorsAndRoles = e.actorsAndRoles,
        actorsAndRolesDetails = actorsAndRoles,
        affectedThings = e.affectedThings,
        affectedThingsDetails = affectedThingsDetails,
        conditionCode = e.asInstanceOf[ConditionAssessment].conditionCode,
        conditionCodeDetails = conditionCode,
        documents = e.documents,
        documentsDetails = documentsDetails,
        isUpdated = e.isUpdated
      ).asInstanceOf[ConservationReportSubEvent]

  }

  private def getHseRiskAssessmentReport(
      e: ConservationEvent,
      subEventType: Option[ConservationType],
      registeredByName: Option[String],
      updatedByName: Option[String],
      affectedThingsDetails: Seq[MusitObject],
      actorsAndRoles: Seq[ActorRoleDateDetails],
      documentsDetails: Seq[String]
  ): FutureMusitResult[ConservationReportSubEvent] = {
    FutureMusitResult.successful(
      HseRiskAssessmentReport(
        id = e.id,
        eventTypeId = e.eventTypeId,
        eventType = subEventType,
        registeredBy = e.registeredBy,
        registeredByName = registeredByName,
        registeredDate = e.registeredDate,
        updatedBy = e.updatedBy,
        updatedByName = updatedByName,
        updatedDate = e.updatedDate,
        partOf = e.partOf,
        note = e.note,
        actorsAndRoles = e.actorsAndRoles,
        actorsAndRolesDetails = actorsAndRoles,
        affectedThings = e.affectedThings,
        affectedThingsDetails = affectedThingsDetails,
        documents = e.documents,
        documentsDetails = documentsDetails,
        isUpdated = e.isUpdated
      ).asInstanceOf[ConservationReportSubEvent]
    )
  }

  private def getStorageAndHandlingReport(
      e: ConservationEvent,
      subEventType: Option[ConservationType],
      registeredByName: Option[String],
      updatedByName: Option[String],
      affectedThingsDetails: Seq[MusitObject],
      actorsAndRoles: Seq[ActorRoleDateDetails],
      documentsDetails: Seq[String]
  ): FutureMusitResult[ConservationReportSubEvent] = {
    FutureMusitResult.successful(
      StorageAndHandlingReport(
        id = e.id,
        eventTypeId = e.eventTypeId,
        eventType = subEventType,
        registeredBy = e.registeredBy,
        registeredByName = registeredByName,
        registeredDate = e.registeredDate,
        updatedBy = e.updatedBy,
        updatedByName = updatedByName,
        updatedDate = e.updatedDate,
        partOf = e.partOf,
        note = e.note,
        actorsAndRoles = e.actorsAndRoles,
        actorsAndRolesDetails = actorsAndRoles,
        affectedThings = e.affectedThings,
        affectedThingsDetails = affectedThingsDetails,
        lightLevel = e.asInstanceOf[StorageAndHandling].lightLevel,
        uvLevel = e.asInstanceOf[StorageAndHandling].uvLevel,
        relativeHumidity = e.asInstanceOf[StorageAndHandling].relativeHumidity,
        temperature = e.asInstanceOf[StorageAndHandling].temperature,
        documents = e.documents,
        documentsDetails = documentsDetails,
        isUpdated = e.isUpdated
      ).asInstanceOf[ConservationReportSubEvent]
    )
  }

  private def getTechnicalDescriptionReport(
      e: ConservationEvent,
      subEventType: Option[ConservationType],
      registeredByName: Option[String],
      updatedByName: Option[String],
      affectedThingsDetails: Seq[MusitObject],
      actorsAndRoles: Seq[ActorRoleDateDetails],
      documentsDetails: Seq[String]
  ): FutureMusitResult[ConservationReportSubEvent] = {
    FutureMusitResult.successful(
      TechnicalDescriptionReport(
        id = e.id,
        eventTypeId = e.eventTypeId,
        eventType = subEventType,
        registeredBy = e.registeredBy,
        registeredByName = registeredByName,
        registeredDate = e.registeredDate,
        updatedBy = e.updatedBy,
        updatedByName = updatedByName,
        updatedDate = e.updatedDate,
        partOf = e.partOf,
        note = e.note,
        actorsAndRoles = e.actorsAndRoles,
        actorsAndRolesDetails = actorsAndRoles,
        affectedThings = e.affectedThings,
        affectedThingsDetails = affectedThingsDetails,
        documents = e.documents,
        documentsDetails = documentsDetails,
        isUpdated = e.isUpdated
      ).asInstanceOf[ConservationReportSubEvent]
    )
  }

  private def getTreatmentReport(
      e: ConservationEvent,
      subEventType: Option[ConservationType],
      registeredByName: Option[String],
      updatedByName: Option[String],
      affectedThingsDetails: Seq[MusitObject],
      actorsAndRoles: Seq[ActorRoleDateDetails],
      documentsDetails: Seq[String]
  ): FutureMusitResult[ConservationReportSubEvent] = {

    for {
      keywordList <- getKeywordsDetails(
                      e.asInstanceOf[Treatment].keywords
                    )
      materialList <- getMaterialsDetails(
                       e.asInstanceOf[Treatment].materials
                     )
    } yield
      TreatmentReport(
        id = e.id,
        eventTypeId = e.eventTypeId,
        eventType = subEventType,
        registeredBy = e.registeredBy,
        registeredByName = registeredByName,
        registeredDate = e.registeredDate,
        updatedBy = e.updatedBy,
        updatedByName = updatedByName,
        updatedDate = e.updatedDate,
        partOf = e.partOf,
        note = e.note,
        actorsAndRoles = e.actorsAndRoles,
        actorsAndRolesDetails = actorsAndRoles,
        affectedThings = e.affectedThings,
        affectedThingsDetails = affectedThingsDetails,
        keywords = e.asInstanceOf[Treatment].keywords,
        keywordsDetails = keywordList,
        materials = e.asInstanceOf[Treatment].materials,
        materialsDetails = materialList,
        documents = e.documents,
        documentsDetails = documentsDetails,
        isUpdated = e.isUpdated
      ).asInstanceOf[ConservationReportSubEvent]

  }

  def getConservationReportService(
      mid: MuseumId,
      collectionId: String,
      id: EventId
  )(
      implicit currUser: AuthenticatedUser
  ): FutureMusitResult[Option[ConservationProcessForReport]] = {

    val maybeColl = Some(collectionId).flatMap(CollectionUUID.fromString)

    val colId = currUser
      .collectionsFor(mid)
      .filter(mc => mc.uuid.underlying.toString == collectionId)

    val conservationReportProcess =
      findConservationProcessById(mid: MuseumId, id: EventId)

    val conservationReport = conservationReportProcess.flatMapInsideOption { p =>
      conservationReportFromConservationProcess(p, mid, colId, maybeColl)
    }

    //conservationProcess
    conservationReport
  }

  /*
   *This method filters on the attribute isUpdated(for the conservationProcess and its subEvents) for setting
   * the currentDate and currentUser in updated_date and updated_by in the subEvents that is updated.
   * This method returns the whole conservationprocess with the filtered subEvents with its updated
   * info. Since FrontEnd doesn't have a clue about registered_date, -by and
   * updated_date and by, we have to get some date/actors from the database.
   * */

  def fillInAppropriateActorDatesAndExcludeNonUpdatedSubEvents(
      mid: MuseumId,
      cp: ConservationProcess,
      actorDate: ActorDate,
      isInsert: Boolean
  )(
      implicit currUsr: AuthenticatedUser
  ): FutureMusitResult[ConservationProcess] = {
    val mrSituation =
      cp.isUpdated match {
        case Some(true)  => MusitSuccess(if (isInsert) Insert else UpdateSelf)
        case Some(false) => MusitSuccess(PreserveDates)
        case None =>
          MusitValidationError("Missing property isUpdated on conservation process")
      }

    mrSituation match {
      case MusitSuccess(situation) =>
        val newCp =
          conservationService.updateProcessWithDateAndActor(mid, cp, situation, actorDate)
        val origChildren = cp.events.getOrElse(Seq.empty)

        val newSubEvents = newCp.flatMap { cp =>
          val newChildren =
            origChildren
              .filter(
                subEvent => subEvent.isUpdated.isDefined && subEvent.isUpdated.get == true
              )
              .map { subEvent =>
                val situation = subEvent.id.isDefined match {
                  case true  => UpdateSelf
                  case false => Insert
                }
                conservationService.updateSubEventWithDateAndActor(
                  mid,
                  subEvent,
                  situation,
                  actorDate
                )
              }
          FutureMusitResult.sequence(newChildren)

        }
        newSubEvents
          .flatMap(subEvents => {
            newCp.flatMap { m =>
              if (!subEvents.isEmpty) {
                val localSituation = if (isInsert) Insert else UpdateSelf
                conservationService
                  .updateProcessWithDateAndActor(mid, m, localSituation, actorDate)
              } else FutureMusitResult.from(m)
            }
          })
          .flatMap(
            ncp =>
              newSubEvents.map(subEventList => {
                ncp.withEvents(subEventList)
              })
          )

      //case err: MusitValidationError => FutureMusitResult.from(err)
      case err: MusitError => FutureMusitResult.from[ConservationProcess](err)
    }

  }

  /**
   * Update a conservationProcess
   */
  def update(
      mid: MuseumId,
      eventId: EventId,
      cp: ConservationProcess
  )(
      implicit currUser: AuthenticatedUser
  ): FutureMusitResult[Option[ConservationProcess]] = {

    for {
      _ <- FutureMusitResult.requireFromClient(
            Some(eventId) == cp.id,
            s"Inconsistent eventid in url($eventId) vs body (${cp.id})"
          )
      _ <- conservationService.checkTypeOfObjects(cp.affectedThings.getOrElse(Seq.empty))
      newCp <- fillInAppropriateActorDatesAndExcludeNonUpdatedSubEvents(
                mid,
                cp,
                ActorDate(currUser.id, dateTimeNow),
                false
              ).map(_.cleanupBeforeInsertIntoDatabase)
      _ <- {
        conservationProcDao.update(mid, eventId, newCp)
      }
      maybeUpdated <- findConservationProcessById(mid, eventId)

    } yield maybeUpdated
  }

  def getConservationWithKeyDataForObject(mid: MuseumId, objectUuid: ObjectUUID)(
      implicit currUser: AuthenticatedUser
  ): FutureMusitResult[Seq[ConservationProcessKeyData]] = {

    val res1 = objectDao.getConservationProcessIdsAndCaseNumbersForObject(objectUuid)
    val res2 = res1.flatMap { seqTuple =>
      val res3 = seqTuple.map { m =>
        objectDao.getEventsForSpecificCpAndObject(m._1, objectUuid).map { eid =>
          ConservationProcessKeyData(
            m._1.underlying,
            m._2,
            m._3,
            m._4,
            Some(eid.map(_._1)),
            Some(eid.map(_._2))
          )
        }

      }
      FutureMusitResult.sequence(res3)

    }
    res2

  }

}
