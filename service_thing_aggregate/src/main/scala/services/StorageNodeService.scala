package services

import com.google.inject.Inject
import dao.StorageNodeDao
import no.uio.musit.service.MusitResults.MusitResult
import models.MuseumId

import scala.concurrent.Future

class StorageNodeService @Inject() (
    storageNodeDao: StorageNodeDao
) {
  def nodeExists(mid: MuseumId, nodeId: Long): Future[MusitResult[Boolean]] = {
    storageNodeDao.nodeExists(mid, nodeId)
  }
}
