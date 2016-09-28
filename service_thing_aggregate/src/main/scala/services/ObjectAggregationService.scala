package services

import com.google.inject.Inject
import dao.{ ObjectAggregationDao, StorageNodeDao }
import no.uio.musit.service.MusitResults.MusitResult
import models.{ MuseumId, ObjectAggregation }

import scala.concurrent.Future

class ObjectAggregationService @Inject() (
    dao: ObjectAggregationDao,
    storageNodeDao: StorageNodeDao
) {

  def getObjects(mid: MuseumId, nodeId: Long): Future[MusitResult[Seq[ObjectAggregation]]] = dao.getObjects(mid, nodeId)

}
