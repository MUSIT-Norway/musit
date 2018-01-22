package repositories.conservation.dao

import com.google.inject.{Inject, Singleton}
import no.uio.musit.MusitResults.{MusitSuccess, MusitValidationError}
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.models._
import no.uio.musit.repositories.DbErrorHandlers
import no.uio.musit.repositories.events.EventActions
import no.uio.musit.security.AuthenticatedUser
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
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

  def deleteSubEvent(mid: MuseumId, eventId: EventId): FutureMusitResult[Unit] = {
    val q = eventTable
      .filter(de => de.museumId === mid && de.eventId === eventId)
      .map(e => e.isDeleted)
      .update(Some(1))
    daoUtils.dbRun(
      q,
      s"An unexpected error occurred deleting event for eventId: $eventId"
    )
  }.mapAndFlattenMusitResult(
    m =>
      if (m == 1) MusitSuccess(())
      else (MusitValidationError(s"Trying to delete a non-existing eventId $eventId"))
  )

  def isValidObject(oid: ObjectUUID): FutureMusitResult[Boolean] = {
    val uuid = oid.asString
    val isObject =
      sql"""select count(*) from MUSIT_MAPPING.MUSITTHING t
           where t.musitthing_uuid =${uuid}
         """.as[Int].head
    daoUtils.dbRun(isObject, "Unexpected error in isValidObject").map(m => m == 1)
  }

}
