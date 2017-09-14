package services.conservation

import com.google.inject.Inject
import models.conservation.events.ConservationType
import no.uio.musit.MusitResults.MusitResult
import no.uio.musit.models.CollectionUUID
import no.uio.musit.security.AuthenticatedUser
import play.api.Logger
import repositories.conservation.dao.{ConservationProcessDao, ConservationTypeDao}

import scala.concurrent.{ExecutionContext, Future}

class ConservationProcesService @Inject()(
    implicit
    val analysisDao: ConservationProcessDao,
    val typeDao: ConservationTypeDao,
    val ec: ExecutionContext
) {

  val logger = Logger(classOf[ConservationProcesService])

  def getTypesFor(coll: Option[CollectionUUID])(
      implicit currUser: AuthenticatedUser
  ): Future[MusitResult[Seq[ConservationType]]] = {
    typeDao.allFor(coll)
  }

}
