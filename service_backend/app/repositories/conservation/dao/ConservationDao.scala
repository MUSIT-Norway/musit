package repositories.conservation.dao

import com.google.inject.{Inject, Singleton}
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess}
import no.uio.musit.models._
import no.uio.musit.repositories.DbErrorHandlers
import no.uio.musit.repositories.events.EventActions
import no.uio.musit.security.AuthenticatedUser
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}
@Singleton
class ConservationDao @Inject()(
    implicit
    val dbConfigProvider: DatabaseConfigProvider,
    val ec: ExecutionContext
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
  )(implicit currUser: AuthenticatedUser): Future[MusitResult[Option[EventTypeId]]] = {

    val query = eventTable.filter { e =>
      e.eventId === eventId
    }.map(event => event.eventTypeId).result.headOption

    db.run(query)
      .map(MusitSuccess.apply)
      .recover(
        nonFatal(
          s"An unexpected error occurred fetching eventTypeId for event with id: $eventId"
        )
      )

  }
}
