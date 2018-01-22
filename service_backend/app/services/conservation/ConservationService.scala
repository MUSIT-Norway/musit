package services.conservation

import com.google.inject.Inject
import models.conservation.MaterialBase
import models.conservation.events.EventRole
import no.uio.musit.MusitResults.{MusitSuccess, MusitValidationError}
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.models.MuseumCollections.{
  Archeology,
  Collection,
  Ethnography,
  Numismatics
}
import no.uio.musit.models._
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

  def deleteSubEvents(
      mid: MuseumId,
      eventIds: Seq[EventId]
  ): FutureMusitResult[Unit] = {
    FutureMusitResult
      .collectAllOrFail[EventId, Unit](
        eventIds,
        eid => dao.deleteSubEvent(mid, eid).map(m => Some(m)),
        eventIds => MusitValidationError(s"Unable to delete eventIds:$eventIds")
      )
      .map(m => ())
  }

  def checkTypeOfObjects(objects: Seq[ObjectUUID]): FutureMusitResult[Unit] = {
    val res = objects.map(obj => dao.isValidObject(obj))
    FutureMusitResult.sequence(res).map(m => m.find(b => !b)).mapAndFlattenMusitResult {
      case Some(b) => {
        MusitValidationError("One or more objectIds are not in the object table")
      }
      case None => {
        MusitSuccess(())
      }
    }
  }

}
