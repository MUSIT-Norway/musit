package services

import com.google.inject.Inject
import dao.{ ObjectAggregationDao, StorageNodeDao }
import models.MusitResults.MusitResult
import models.ObjectAggregation

import scala.concurrent.Future

class ObjectAggregationService @Inject() (
    dao: ObjectAggregationDao,
    storageNodeDao: StorageNodeDao
) {

  def getObjects(nodeId: Long): Future[MusitResult[Seq[ObjectAggregation]]] = dao.getObjects(nodeId)

}
