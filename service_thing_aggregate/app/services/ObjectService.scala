package services

import com.google.inject.Inject
import models.{MusitObject, ObjectSearchResult}
import no.uio.musit.MusitResults._
import no.uio.musit.models._
import no.uio.musit.security.AuthenticatedUser
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.actor.dao.{ObjectDao, StorageNodeDao}

import scala.concurrent.Future
import scala.util.control.NonFatal

class ObjectService @Inject()(
    val objDao: ObjectDao,
    val nodeDao: StorageNodeDao
) {

  private val logger = Logger(classOf[ObjectService])

  /**
   * Service that looks up objects using the old primary key in for the old DB
   * schema name. Implementation is specific to the Delphi client integration.
   *
   * @param oldSchema    The old DB schema name
   * @param oldObjectIds The local primary key for the given schema name.
   * @return A list containing the _new_ ObjectIds for the objects.
   */
  def findByOldObjectIds(
      oldSchema: String,
      oldObjectIds: Seq[Long]
  ): Future[MusitResult[Seq[ObjectId]]] = {
    objDao.findObjectIdsForOld(oldSchema, oldObjectIds)
  }

  def findByUUID(
      mid: MuseumId,
      objectUUID: ObjectUUID,
      cids: Seq[MuseumCollection]
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Option[MusitObject]]] = {
    objDao.findByUUID(mid, objectUUID, cids)
  }

  /**
   * A helper method for getting the current location of an object
   *
   * @param mid         The MuseumId to look in
   * @param obj         The MusitObject to look for
   * @return The augmented object with path, pathNames and currentLocationId
   */
  private def getCurrentLocation(
      mid: MuseumId,
      obj: MusitObject
  ): Future[MusitObject] =
    obj.uuid.map { oid =>
      nodeDao.currentLocation(mid, oid).flatMap {
        case Some(nodeIdAndPath) =>
          nodeDao.namesForPath(nodeIdAndPath._2).map { pathNames =>
            obj.copy(
              currentLocationId = Some(nodeIdAndPath._1),
              path = Some(nodeIdAndPath._2),
              pathNames = Some(pathNames)
            )
          }
        case None =>
          Future.successful(obj)
      }
    }.getOrElse {
      Future.successful(obj)
    }

  /**
   * Locate object(s) based on museum, old barcode and collection(s).
   *
   * @param mid          The MuseumId to look for objects in.
   * @param oldBarcode   The bar code to look for.
   * @param collections  Which collections to look in.
   * @param currUsr      The currently authenticated user.
   * @return A list of objects that share tha same bare code
   */
  def findByOldBarcode(
      mid: MuseumId,
      oldBarcode: Long,
      collections: Seq[MuseumCollection]
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Seq[MusitObject]]] = {
    objDao.findByOldBarcode(mid, oldBarcode, collections).flatMap {
      case MusitSuccess(objs) =>
        Future
          .sequence(objs.map(getCurrentLocation(mid, _)))
          .map(MusitSuccess.apply)
          .recover {
            case NonFatal(ex) =>
              val msg = s"An error occured when executing object search by old barcode"
              logger.error(msg, ex)
              MusitInternalError(msg)
          }
      case err: MusitError =>
        Future.successful(err)
    }
  }

  /**
   * Locate objects that share the same main object UUID.
   *
   * @param mid           The MuseumId to look for objects in.
   * @param mainObjectId  The main object UUID to look for.
   * @param collectionIds Which collections to look in.
   * @param currUsr       The currently authenticated user.
   * @return A list of objects that share the same main object ID.
   */
  def findMainObjectChildren(
      mid: MuseumId,
      mainObjectId: ObjectUUID,
      collectionIds: Seq[MuseumCollection]
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[Seq[MusitObject]]] = {
    objDao.findMainObjectChildren(mid, mainObjectId, collectionIds)
  }

  /**
   * Locate objects in the specified museum, node and collection(s).
   *
   * @param mid           The MuseumId to look for objects in.
   * @param nodeId        The specific StorageNodeId to look for objects in.
   * @param collectionIds Specifies collections to fetch objects for.
   * @param page          The page number to retrieve.
   * @param limit         The number of results per page.
   * @param currUsr       The currently authenticated user.
   * @return A list of objects matching the given criteria.
   */
  def findObjects(
      mid: MuseumId,
      nodeId: StorageNodeId,
      collectionIds: Seq[MuseumCollection],
      page: Int,
      limit: Int
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[PagedResult[MusitObject]]] = {
    objDao.pagedObjects(mid, nodeId, collectionIds, page, limit)
  }

  /**
   * Search for objects based on the given criteria.
   *
   * @param mid           The MuseumId to search for objects in
   * @param collectionIds The collections to search for objects in.
   * @param page          The page number to retrieve.
   * @param limit         The number of results per page.
   * @param museumNo      The MuseumNo to find matches for.
   * @param subNo         The SubNo to find matches for.
   * @param term          The object term to find matches for.
   * @param currUsr       The currently authenticated user.
   * @return A list of search results matching the given criteria.
   */
  def search(
      mid: MuseumId,
      collectionIds: Seq[MuseumCollection],
      page: Int,
      limit: Int,
      museumNo: Option[MuseumNo],
      subNo: Option[SubNo],
      term: Option[String]
  )(implicit currUsr: AuthenticatedUser): Future[MusitResult[ObjectSearchResult]] = {
    objDao.search(mid, page, limit, museumNo, subNo, term, collectionIds).flatMap {
      case MusitSuccess(searchResult) =>
        // We found some objects...now we need to find the current location for each.
        Future
          .sequence(searchResult.matches.map(getCurrentLocation(mid, _)))
          .map { objects =>
            MusitSuccess(searchResult.copy(matches = objects))
          }
          .recover {
            case NonFatal(ex) =>
              val msg = s"An error occured when executing object search"
              logger.error(msg, ex)
              MusitInternalError(msg)
          }

      case err: MusitError =>
        Future.successful(err)
    }
  }
}
