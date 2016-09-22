package services

import com.google.inject.Inject
import dao.StorageNodeDao
import no.uio.musit.service.MusitResults.MusitResult

import scala.concurrent.Future

class StorageNodeService @Inject() (
    storageNodeDao: StorageNodeDao
) {
  def nodeExists(nodeId: Long): Future[MusitResult[Boolean]] = {
    storageNodeDao.nodeExists(nodeId)
  }
}
