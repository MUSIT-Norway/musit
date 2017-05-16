package services

import com.google.inject.Inject
import no.uio.musit.MusitResults.{MusitError, MusitResult, MusitSuccess}
import no.uio.musit.models.Museums.Museum
import no.uio.musit.models.{MuseumId, StorageNodeDatabaseId, StorageNodeId}
import no.uio.musit.security.AuthenticatedUser
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.actor.dao.{ObjectDao, StorageNodeDao}

import scala.concurrent.Future

class StorageNodeService @Inject()(
    val nodeDao: StorageNodeDao,
    val objDao: ObjectDao
) {

  val logger = Logger(classOf[StorageNodeService])

  /**
   * Checks if the node exists in the storage facility for given museum ID.
   *
   * @param mid    MuseumId
   * @param nodeId StorageNodeId
   * @return
   */
  def nodeExists(
      mid: MuseumId,
      nodeId: StorageNodeId
  ): Future[MusitResult[Boolean]] = nodeDao.nodeExists(mid, nodeId)

  /**
   *
   * @param oldObjectId
   * @param oldSchemaName
   * @return
   */
  def currNodeForOldObject(
      oldObjectId: Long,
      oldSchemaName: String
  )(
      implicit currUsr: AuthenticatedUser
  ): Future[MusitResult[Option[(StorageNodeId, String)]]] = {
    // Look up object using it's old object ID and the old DB schema name.
    objDao.findByOldId(oldObjectId, oldSchemaName).flatMap {
      case MusitSuccess(mobj) =>
        val res = for {
          obj <- mobj
          oid <- obj.uuid
        } yield {
          nodeDao.currentLocation(obj.museumId, oid).flatMap {
            case Some(sn) =>
              nodeDao.namesForPath(sn._2).map { np =>
                // Only authorized users are allowed to see the full path
                // TODO: We probably need to verify the _group_ and not the museum.
                if (currUsr.isAuthorized(obj.museumId)) {
                  MusitSuccess(Option((sn._1, np.map(_.name).mkString(", "))))
                } else {
                  MusitSuccess(Option((sn._1, Museum.museumIdToString(obj.museumId))))
                }
              }

            case None =>
              Future.successful(MusitSuccess(None))
          }
        }
        res.getOrElse(Future.successful(MusitSuccess(None)))

      case err: MusitError =>
        Future.successful(err)
    }
  }

  def nodesOutsideMuseum(
      museumId: MuseumId
  ): Future[MusitResult[Seq[(StorageNodeDatabaseId, String)]]] = {
    nodeDao.getRootLoanNodes(museumId).flatMap {
      case MusitSuccess(rids) =>
        logger.debug(s"Found ${rids.size} external Root nodes: ${rids.mkString(", ")}")
        if (rids.nonEmpty) nodeDao.listAllChildrenFor(museumId, rids)
        else Future.successful(MusitSuccess(Seq.empty))
      case err: MusitError => Future.successful(err)
    }
  }

}
