package repositories.conservation.dao

import com.google.inject.{Inject, Singleton}
import models.conservation.events.ConservationEvent
import no.uio.musit.MusitResults.{MusitSuccess, MusitValidationError}
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.models._
import no.uio.musit.repositories.{DBIOUtils, DbErrorHandlers}
import no.uio.musit.repositories.events.EventActions
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.time.dateTimeNow
import org.joda.time.DateTime
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json._
import play.mvc.BodyParser.Json
import repositories.conservation.DaoUtils

import scala.concurrent.ExecutionContext
@Singleton
class ConservationDao @Inject()(
    implicit
    val dbConfigProvider: DatabaseConfigProvider,
    val ec: ExecutionContext,
    val daoUtils: DaoUtils,
    val actorRoleDao: ActorRoleDateDao
) extends ConservationEventTableProvider
    with ConservationTables
    with EventActions
    with ConservationEventRowMappers
    with DbErrorHandlers {

  val logger = Logger(classOf[ConservationDao])

  import profile.api._

  /**
   * Returns the eventTypeId for a given eventId.
   * We ignore collection and museum here, because I feel that can be more confusing than helpful to not find
   * the eventTypeId for a given eventId if the event actually exists but for a different museum.
   *
   */
  def getEventTypeId(
      eventId: EventId
  )(implicit currUser: AuthenticatedUser): FutureMusitResult[Option[EventTypeId]] = {

    val query = eventTable.filter { e =>
      e.eventId === eventId
    }.map(event => event.eventTypeId).result.headOption
    daoUtils.dbRun(
      query,
      s"An unexpected error occurred fetching eventTypeId for event with id: $eventId"
    )

  }

  /*def deleteSubEvent(mid: MuseumId, eventId: EventId)(
      implicit currUser: AuthenticatedUser
  ): FutureMusitResult[Unit] = {
    val q = eventTable
      .filter(de => de.museumId === mid && de.eventId === eventId)
      .map(e => (e.isDeleted, e.updatedBy, e.updatedDate))
      .update((Some(1), Some(currUser.id), Some(dateTimeNow)))
    daoUtils.dbRun(
      q,
      s"An unexpected error occurred deleting event for eventId: $eventId"
    )
  }.mapAndFlattenMusitResult(
    m =>
      if (m == 1) MusitSuccess(())
      else (MusitValidationError(s"Trying to delete a non-existing eventId $eventId"))
  )*/

  def deleteSubEventAction(mid: MuseumId, eventId: EventId)(
      implicit currUser: AuthenticatedUser
  ): DBIO[Int] = {
    eventTable
      .filter(de => de.museumId === mid && de.eventId === eventId)
      .map(e => (e.isDeleted, e.updatedBy, e.updatedDate))
      .update((Some(1), Some(currUser.id), Some(dateTimeNow)))

  }

  def getEventJsonColumnAction(eventId: EventId): DBIO[Option[JsValue]] = {
    val res = eventTable
      .filter(event => event.eventId === eventId && event.isDeleted === 0)
      .map(ej => ej.eventJson)
      .result
      .headOption
    res
  }

  private def findPartOfIdAction(eventId: EventId): DBIO[Option[EventId]] = {
    val res = eventTable
      .filter(event => event.eventId === eventId)
      .map(po => po.partOf)
      .result
      .headOption
      .map(m => m.flatten)
    res
  }

  def UpdateJsonUpdateData(
      eventJson: JsObject,
      updatedDate: DateTime,
      currUserId: ActorId
  ) = {
    val jsObj = play.api.libs.json.Json.obj(
      "updatedDate" -> updatedDate.toString(),
      "updatedBy"   -> currUserId.underlying
    )
    eventJson ++ jsObj
  }

  def updateCPsActorDateAction(
      eventId: EventId
  )(implicit currUser: AuthenticatedUser): DBIO[Int] = {
    val updatedDate = dateTimeNow
    for {
      eventJson <- getEventJsonColumnAction(eventId)

      update <- eventTable
                 .filter(cp => cp.eventId === eventId)
                 .map(m => (m.updatedBy, m.updatedDate, m.eventJson))
                 .update(
                   (
                     Some(currUser.id),
                     Some(updatedDate),
                     UpdateJsonUpdateData(
                       eventJson.get.asInstanceOf[JsObject],
                       updatedDate,
                       currUser.id
                     )
                   )
                 )
    } yield update
  }

  def updateCpAndDeleteSubEvent(mid: MuseumId, eventId: EventId)(
      implicit currUser: AuthenticatedUser
  ): FutureMusitResult[Int] = {
    val action = for {
      cpUpdated <- DBIOUtils.flatMapInsideOption[EventId, Int](
                    findPartOfIdAction(eventId),
                    cpId => updateCPsActorDateAction(cpId)
                  )
      del <- deleteSubEventAction(mid, eventId)
    } yield del
    daoUtils
      .dbRun(
        action.transactionally,
        s"An unexpected error occurred deleting event for eventId: $eventId"
      )
      .mapAndFlattenMusitResult(
        m =>
          if (m == 1) MusitSuccess(m)
          else (MusitValidationError(s"Trying to delete a non-existing eventId $eventId"))
      )
  }

  def isValidObject(oid: ObjectUUID): FutureMusitResult[Boolean] = {
    val uuid = oid.asString
    val isObject =
      sql"""select count(*) from MUSIT_MAPPING.MUSITTHING t
           where t.musitthing_uuid =${uuid}
         """.as[Int].head
    daoUtils.dbRun(isObject, "Unexpected error in isValidObject").map(m => m == 1)
  }

}
