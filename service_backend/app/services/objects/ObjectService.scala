package services.objects

import com.google.inject.Inject
import no.uio.musit.MusitResults.MusitResult
import no.uio.musit.models.{ObjectId, ObjectUUID}
import repositories.objects.ObjectDao

import scala.concurrent.Future

class ObjectService @Inject()(
    objDao: ObjectDao
) {

  def getUUIDsFor(ids: Seq[ObjectId]): Future[MusitResult[Seq[(ObjectId, ObjectUUID)]]] =
    objDao.uuidsForIds(ids)

}
