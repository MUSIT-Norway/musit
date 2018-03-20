package services.conservation

import com.google.inject.Inject
import models.conservation.ConservationProcessKeyData
import models.conservation.events._
import no.uio.musit.MusitResults
import no.uio.musit.MusitResults.{
  MusitError,
  MusitResult,
  MusitSuccess,
  MusitValidationError
}
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
import models.actor.Person
import models.musitobject.MusitObject
import play.api.libs.json.Json
import scalatags.Text.all._

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
    val objService: ObjectService,
    val actorService: ActorService
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
   * Locate an event with the given EventId.
   */
  def conservationReportFromConservationProcess(
      process: ConservationProcess,
      mid: MuseumId,
      colId: Seq[MuseumCollection],
      maybeColl: Option[CollectionUUID]
  )(
      implicit currUser: AuthenticatedUser
  ): FutureMusitResult[ConservationProcessForReport] = {

    // affectedThingsDetails for main event

    def localFindByUUID(o: ObjectUUID) =
      FutureMusitResult(objService.findByUUID(mid, o, colId))
    val ids = process.affectedThings.getOrElse(Seq.empty)
    val futObjectDetails = FutureMusitResult
      .collectAllOrFail[ObjectUUID, MusitObject](
        ids,
        localFindByUUID,
        objectIds =>
          MusitValidationError(s"Missing objects for these objectIds:$objectIds")
      )
      .map { o =>
        //println("inside map " + o.toString())
        var obj = ConservationProcessForReport(
          id = process.id,
          eventTypeId = process.eventTypeId,
          eventType = None,
          caseNumber = process.caseNumber,
          registeredBy = process.registeredBy,
          registeredByName = None,
          registeredDate = process.registeredDate,
          updatedBy = process.updatedBy,
          updatedByName = None,
          updatedDate = process.updatedDate,
          partOf = process.partOf,
          note = process.note,
          actorsAndRoles = process.actorsAndRoles.getOrElse(Seq.empty),
          affectedThings = process.affectedThings.getOrElse(Seq.empty),
          events = process.events.getOrElse(Seq.empty),
          isUpdated = process.isUpdated,
          affectedThingsDetails = o
        )
        obj

      }

    // registeredByName for main event

    val FMRReport = futObjectDetails.flatMap { report =>
      val ofoPerson: Option[Future[Option[Person]]] =
        report.registeredBy.map(actorId => actorService.findByActorId(actorId))

      val foPerson = ofoPerson.getOrElse(Future.successful(None))
      val fPersonName =
        foPerson.map(person => person.map(_.fn).getOrElse("(Finner ikke navnet!)"))
      val fmrPersonName = FutureMusitResult(fPersonName.map(MusitSuccess(_)))
      fmrPersonName.map(personName => report.copy(registeredByName = Some(personName)))

    }

    // updatedByName for main event
    val FMRReport2 = FMRReport.flatMap { report =>
      val ofoPerson: Option[Future[Option[Person]]] =
        report.registeredBy.map(actorId => actorService.findByActorId(actorId))

      val foPerson = ofoPerson.getOrElse(Future.successful(None))
      val fPersonName =
        foPerson.map(person => person.map(_.fn).getOrElse("(Finner ikke navnet!)"))
      val fmrPersonName = FutureMusitResult(fPersonName.map(MusitSuccess(_)))
      fmrPersonName.map(personName => report.copy(updatedByName = Some(personName)))

    }

    // Main event type is added
    val FMRReport3 =
      for {
        conservationTypes <- typeDao.allFor(maybeColl)
        report2           <- FMRReport2
        maybeEventType = conservationTypes.find(t => t.id == report2.eventTypeId)
        report3        = report2.copy(eventType = maybeEventType)
      } yield report3

    FMRReport3

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


    /*
    val conservationReport = conservationReportProcess.map(
      optProcess =>
        optProcess.map(p => ConservationReportFromConservationProcess(p, mid, colId))
    )

    val objs = conservationReportProcess.map { m =>
      m.map { n =>
        n.affectedThings.map { os =>
          os.map { o =>
            {
              println("affectedThings uuid " + o);
              val obj = objService.findByUUID(mid, o, colId);

              obj.map(o1 => {
                println("obj1 " + o1)
                o1.map(o2 => {
                  println("obj2 " + o2)
                  o2.map(o3 => println("obj3 " + o3))
                })
              });
              obj
            }
          }

        }
      }
    }
     */
    //println("RK out ");
    //val objectUUIDs = [];

    //objectUUIDs.map( u => objService.findByUUID(mid, u, colIds));

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
