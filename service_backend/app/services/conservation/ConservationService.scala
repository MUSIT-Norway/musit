package services.conservation

import com.google.inject.Inject
import models.conservation.events.EventRole
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.models.{EventId, EventTypeId}
import no.uio.musit.security.AuthenticatedUser
import play.api.Logger
import repositories.conservation.dao.{ActorRoleDateDao, ConservationDao}

import scala.concurrent.ExecutionContext

class ConservationService @Inject()(
    implicit
    val dao: ConservationDao,
    val actorRoleDateDao: ActorRoleDateDao,
    val ec: ExecutionContext
) {

  val logger = Logger(classOf[ConservationService])

  def getEventTypeId(eventId: EventId)(
      implicit currUser: AuthenticatedUser
  ): FutureMusitResult[Option[EventTypeId]] = {
    dao.getEventTypeId(eventId)
  }

  def getRoleList: FutureMusitResult[Seq[EventRole]] = {
    actorRoleDateDao.getRoleList
  }
}
