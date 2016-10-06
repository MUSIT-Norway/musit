package services

/**
 * Created by jarle on 05.10.16.
 */
import com.google.inject.Inject
import dao.ObjectSearchDao
import models.{MusitThing, ObjectAggregation}
import no.uio.musit.service.MusitResults.MusitResult

import scala.concurrent.Future

class ObjectSearchService @Inject() (
    objectSearchDao: ObjectSearchDao
) {

  val maxLimit = 100

  def search(mid: Int, museumNo: String = "", subNo: String = "", term: String = "", page: Int = 1, limit: Int = 25): Future[MusitResult[Seq[MusitThing]]] = {
    objectSearchDao.search(mid, museumNo, subNo, term, page, limit)
  }
}
