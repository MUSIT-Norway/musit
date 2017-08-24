package services.musitobject

import com.google.inject.Inject
import models.musitobject.{MusitObject, ObjectSearchResult}
import no.uio.musit.MusitResults.{
  MusitError,
  MusitInternalError,
  MusitResult,
  MusitSuccess
}
import no.uio.musit.functional.Implicits.futureMonad
import no.uio.musit.functional.MonadTransformers.MusitResultT
import no.uio.musit.models._
import no.uio.musit.security.AuthenticatedUser
import play.api.Logger
import repositories.musitobject.dao.ObjectDao
import repositories.storage.dao.nodes.StorageUnitDao

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class ObjectService @Inject()(
    implicit
    val objDao: ObjectDao,
    val nodeDao: StorageUnitDao,
    val ec: ExecutionContext
) {

  private val logger = Logger(classOf[ObjectService])

  def getUUIDsFor(ids: Seq[ObjectId]): Future[MusitResult[Seq[(ObjectId, ObjectUUID)]]] =
    objDao.uuidsForIds(ids)

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
    val r = for {
      mo <- MusitResultT(objDao.findByUUID(mid, objectUUID, cids))
      matr <- MusitResultT(mo.flatMap { o =>
               o.collection.map { c =>
                 objDao.getObjectMaterial(mid, c, o.id)
               }
             }.getOrElse(Future.successful(MusitSuccess(Seq.empty))))
      loc <- MusitResultT(mo.flatMap { o =>
              o.collection.map { c =>
                objDao.getObjectLocation(mid, c, o.id)
              }
            }.getOrElse(Future.successful(MusitSuccess(Seq.empty))))
      coord <- MusitResultT(mo.flatMap { o =>
                o.collection.map { c =>
                  objDao.getObjectCoordinate(mid, c, o.id)
                }
              }.getOrElse(Future.successful(MusitSuccess(Seq.empty))))
    } yield {
      mo.map { obj =>
        obj.copy(
          materials = Option(matr),
          locations = Option(loc),
          coordinates = Option(coord)
        )
      }
    }

    r.value
  }

  /**
   * A helper method for getting the current location of an object
   *
   * @param mid The MuseumId to look in
   * @param obj The MusitObject to look for
   * @return The augmented object with path, pathNames and currentLocationId
   */
  private def getCurrentLocation(
      mid: MuseumId,
      obj: MusitObject
  ): Future[MusitResult[MusitObject]] =
    obj.uuid.map { oid =>
      val mrt = for {
        maybeNodeIdAndPath <- MusitResultT(nodeDao.currentLocation(mid, oid))
        pathNames <- MusitResultT(
                      maybeNodeIdAndPath
                        .map(idp => nodeDao.namesForPath(idp._2))
                        .getOrElse(
                          Future.successful(MusitSuccess(Seq.empty[NamedPathElement]))
                        )
                    )
      } yield
        maybeNodeIdAndPath.map { idp =>
          obj.copy(
            currentLocationId = Some(idp._1),
            path = Some(idp._2),
            pathNames = Some(pathNames)
          )
        }.getOrElse(obj)

      mrt.value
    }.getOrElse {
      Future.successful(MusitSuccess(obj))
    }

  /**
   * Locate object(s) based on museum, old barcode and collection(s).
   *
   * @param mid         The MuseumId to look for objects in.
   * @param oldBarcode  The bar code to look for.
   * @param collections Which collections to look in.
   * @param currUsr     The currently authenticated user.
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
          .map(MusitResult.sequence)
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
  )(
      implicit currUsr: AuthenticatedUser
  ): Future[MusitResult[PagedResult[MusitObject]]] = {
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
          .map(MusitResult.sequence)
          .map { objects =>
            objects.map(objs => searchResult.copy(matches = objs))
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
