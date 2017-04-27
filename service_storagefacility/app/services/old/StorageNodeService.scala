package services.old

import com.google.inject.Inject
import models.storage._
import models.storage.nodes._
import no.uio.musit.MusitResults._
import no.uio.musit.functional.Implicits.futureMonad
import no.uio.musit.functional.MonadTransformers.MusitResultT
import no.uio.musit.models._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.storage.dao.nodes.StorageUnitDao
import repositories.storage.old_dao.LocalObjectDao

import scala.concurrent.Future

/**
 * TODO: Document me!!!
 */
class StorageNodeService @Inject()(
    unitDao: StorageUnitDao,
    localObjectDao: LocalObjectDao
) {

  val logger = Logger(classOf[StorageNodeService])

  /**
   *
   * @param mid
   * @param mobjs
   * @return
   */
  def currentObjectLocations(
      mid: MuseumId,
      mobjs: Seq[MovableObject_Old]
  ): Future[MusitResult[Seq[ObjectsLocation_Old]]] = {

    def findObjectLocations(
        objNodeMap: Map[MovableObject_Old, Option[StorageNodeDatabaseId]],
        nodes: Seq[GenericStorageNode]
    ): Future[MusitResult[Seq[ObjectsLocation_Old]]] = {
      nodes
        .foldLeft(Future.successful(List.empty[Future[ObjectsLocation_Old]])) {
          case (ols, node) =>
            unitDao.namesForPath(node.path).flatMap {
              case MusitSuccess(namedPaths) =>
                val objects = objNodeMap.filter(_._2 == node.id).keys.map(_.id).toSeq
                // Copy node and set path to it
                ols.map { objLoc =>
                  objLoc :+ Future.successful(
                    ObjectsLocation_Old(
                      node.copy(pathNames = Option(namedPaths)),
                      objects
                    )
                  )
                }

              case _ => ols
            }

        }
        .flatMap(fl => Future.sequence(fl))
        .map(MusitSuccess.apply)
    }

    localObjectDao.currentLocationsForMovableObjects(mobjs).flatMap {
      case MusitSuccess(objNodeMap) =>
        val nodeIds = objNodeMap.values.flatten.toSeq.distinct

        val res = for {
          nodes  <- MusitResultT(unitDao.getNodesByDatabaseIds(mid, nodeIds))
          objLoc <- MusitResultT(findObjectLocations(objNodeMap, nodes))
        } yield objLoc
        res.value

      case err: MusitError => Future.successful(err)
    }
  }

}
