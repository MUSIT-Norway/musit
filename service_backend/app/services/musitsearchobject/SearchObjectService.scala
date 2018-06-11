package services.musitsearchobject

import com.google.inject.Inject
import models.musitsearchobject.SearchObjectResult
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.models._
import no.uio.musit.security.AuthenticatedUser
import play.api.Logger
import repositories.musitsearchobject.dao.MusitSearchObjectDao
import repositories.storage.dao.nodes.StorageUnitDao

import scala.concurrent.ExecutionContext

class SearchObjectService @Inject()(
    implicit
    val searchObjDao: MusitSearchObjectDao,
    val nodeDao: StorageUnitDao,
    val ec: ExecutionContext
) {

  private val logger = Logger(classOf[SearchObjectService])

  /** page starts at 1 **/
  def findObjects(
      mid: MuseumId,
      museumNo: Option[MuseumNo],
      museumNoAsANumber: Option[String],
      subNo: Option[SubNo],
      term: Option[String],
      collectionIds: Seq[MuseumCollection],
      page: Int,
      pageSize: Int
  )(
      implicit currUsr: AuthenticatedUser
  ): FutureMusitResult[PagedResult[SearchObjectResult]] = {
    searchObjDao.executeSearch(
      mid,
      museumNo,
      museumNoAsANumber,
      subNo,
      term,
      collectionIds,
      page,
      pageSize
    )
  }

  def recreateSearchTable()(implicit ec: ExecutionContext) = {
    searchObjDao.recreateSearchTable()

  }
}
