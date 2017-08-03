package services.storage

import models.storage.nodes.StorageUnit
import no.uio.musit.MusitResults.{
  MusitError,
  MusitResult,
  MusitSuccess,
  MusitValidationError
}
import no.uio.musit.models.{MuseumId, StorageNodeDatabaseId, StorageNodeId}
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.time.dateTimeNow

import scala.concurrent.{ExecutionContext, Future}

trait StorageUnitServiceOps { self: NodeService =>

  def getStorageUnitByDatabaseId(
      mid: MuseumId,
      id: StorageNodeDatabaseId
  )(
      implicit currUsr: AuthenticatedUser,
      ec: ExecutionContext
  ): Future[MusitResult[Option[StorageUnit]]] = {
    val eventuallyUnit = unitDao.getByDatabaseId(mid, id)
    getNode(mid, eventuallyUnit) { (n, maybeReq, maybeNames) =>
      n.copy(
        environmentRequirement = maybeReq,
        pathNames = maybeNames
      )
    }
  }

  def addStorageUnit(
      mid: MuseumId,
      storageUnit: StorageUnit
  )(
      implicit currUsr: AuthenticatedUser,
      ec: ExecutionContext
  ): Future[MusitResult[Option[StorageUnit]]] = {
    addNode[StorageUnit](
      mid = mid,
      node = storageUnit.copy(
        updatedBy = Some(currUsr.id),
        updatedDate = Some(dateTimeNow)
      ),
      insert = unitDao.insert,
      setEnvReq = (node, mer) => node.copy(environmentRequirement = mer),
      updateWithPath = (id, path) => unitDao.setPath(id, path),
      getNode = getStorageUnitByDatabaseId
    )
  }

  def updateStorageUnit(
      mid: MuseumId,
      id: StorageNodeId,
      storageUnit: StorageUnit
  )(
      implicit currUsr: AuthenticatedUser,
      ec: ExecutionContext
  ): Future[MusitResult[Option[StorageUnit]]] = {
    val su = storageUnit.copy(
      updatedBy = Some(currUsr.id),
      updatedDate = Some(dateTimeNow)
    )
    su.id.map { dbId =>
      unitDao.update(mid, id, su).flatMap {
        case MusitSuccess(maybeRes) =>
          maybeRes.map { _ =>
            for {
              _ <- su.environmentRequirement
                    .map(er => saveEnvReq(mid, dbId, er))
                    .getOrElse(Future.successful(None))
              node <- getStorageUnitByDatabaseId(mid, dbId)
            } yield node
          }.getOrElse(Future.successful(MusitSuccess(None)))

        case err: MusitError =>
          Future.successful(err)
      }
    }.getOrElse {
      Future.successful {
        MusitValidationError("Node to update did not contain a valid ID")
      }
    }
  }

}
