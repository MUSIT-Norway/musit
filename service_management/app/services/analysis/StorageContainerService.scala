package services.analysis

import com.google.inject.Inject
import models.analysis.StorageContainer
import no.uio.musit.MusitResults.MusitResult
import play.api.Logger
import repositories.analysis.dao.StorageContainerDao

import scala.concurrent.Future

class StorageContainerService @Inject()(
    val scDao: StorageContainerDao
) {

  val logger = Logger(classOf[StorageContainerService])

  def getStorageContainerList: Future[MusitResult[Seq[StorageContainer]]] = {
    scDao.getStorageContainerList
  }
}
