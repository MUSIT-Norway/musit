package services.conservation

import com.google.inject.Inject
import models.conservation.events.ConservationEvent
import no.uio.musit.MusitResults.MusitResult
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.models.{MuseumId, ObjectUUID}
import no.uio.musit.security.AuthenticatedUser
import play.api.Logger
import repositories.conservation.dao.TreatmentDao

import scala.concurrent.{ExecutionContext, Future}

class ObjectEventService @Inject()(
    implicit
    val dao: TreatmentDao,
    val ec: ExecutionContext
) {

  val logger = Logger(classOf[ObjectEventService])

  def getEventsForObject(mid: MuseumId, objectUuid: ObjectUUID)(
      implicit currUser: AuthenticatedUser
  ): FutureMusitResult[Seq[ConservationEvent]] = {
    dao.getEventsForObject(mid, objectUuid)
  }
}
