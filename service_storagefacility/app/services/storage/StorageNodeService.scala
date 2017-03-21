package services.storage

import com.google.inject.Inject
import no.uio.musit.MusitResults.MusitResult
import no.uio.musit.models.{MuseumId, StorageNodeId}
import repositories.storage.dao.nodes.StorageUnitDao

import scala.concurrent.Future

class StorageNodeService @Inject()(
    val unitDao: StorageUnitDao
) {

  /**
   * Simple check to see if a node with the given exists in a museum.
   *
   * @param mid
   * @param id
   * @return
   */
  def exists(mid: MuseumId, id: StorageNodeId): Future[MusitResult[Boolean]] = {
    unitDao.exists(mid, id)
  }

}
