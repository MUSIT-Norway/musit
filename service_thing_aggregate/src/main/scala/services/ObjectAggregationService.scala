package services

import com.google.inject.Inject
import dao.ObjectAggregationDao
import models.ObjectAggregation

import scala.concurrent.Future

class ObjectAggregationService @Inject() (
    dao: ObjectAggregationDao
) {

  def getObjects(nodeId: Long): Future[Seq[ObjectAggregation]] = {
    dao.getObjects(nodeId)
  }

}
