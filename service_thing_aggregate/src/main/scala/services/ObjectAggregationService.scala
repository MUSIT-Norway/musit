package services

import com.google.inject.Inject
import dao.ObjectAggregationDao
import models.{ MuseumId, ObjectAggregation, ObjectId }

import scala.concurrent.Future

class ObjectAggregationService @Inject() (
    dao: ObjectAggregationDao
) {

  def getObjects(museumId: Long): Future[Seq[ObjectAggregation]] = {
    dao.getOjects(museumId)
  }

}
