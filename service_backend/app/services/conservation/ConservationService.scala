package services.conservation

import com.google.inject.Inject
import no.uio.musit.MusitResults.MusitResult
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.models.{EventId, EventTypeId}
import no.uio.musit.security.AuthenticatedUser
import play.api.Logger
import repositories.conservation.dao.ConservationDao

import scala.concurrent.{ExecutionContext, Future}

class ConservationService @Inject()(
    implicit
    val dao: ConservationDao,
    val ec: ExecutionContext
) {

  val logger = Logger(classOf[ConservationService])

  def getEventTypeId(eventId: EventId)(
      implicit currUser: AuthenticatedUser
  ): FutureMusitResult[Option[EventTypeId]] = {
    dao.getEventTypeId(eventId)
  }
}
