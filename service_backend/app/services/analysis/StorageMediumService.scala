package services.analysis

import com.google.inject.Inject
import models.analysis.StorageMedium
import no.uio.musit.MusitResults.MusitResult
import play.api.Logger
import repositories.analysis.dao.StorageMediumDao

import scala.concurrent.Future

class StorageMediumService @Inject()(
    val smDao: StorageMediumDao
) {

  val logger = Logger(classOf[StorageMediumService])

  def getStorageMediumList: Future[MusitResult[Seq[StorageMedium]]] = {
    smDao.getStorageMediumList
  }
}
