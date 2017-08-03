package services.storage

import models.storage.nodes.Building
import no.uio.musit.MusitResults.{
  MusitError,
  MusitResult,
  MusitSuccess,
  MusitValidationError
}
import no.uio.musit.models.{MuseumId, StorageNodeDatabaseId, StorageNodeId}
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.time.dateTimeNow
import repositories.storage.dao.nodes.BuildingDao

import scala.concurrent.{ExecutionContext, Future}

trait BuildingServiceOps { self: NodeService =>

  val buildingDao: BuildingDao

  def getBuildingByDatabaseId(
      mid: MuseumId,
      id: StorageNodeDatabaseId
  )(
      implicit currUsr: AuthenticatedUser,
      ec: ExecutionContext
  ): Future[MusitResult[Option[Building]]] = {
    val eventuallyBuilding = buildingDao.getById(mid, id)
    getNode(mid, eventuallyBuilding) { (n, maybeReq, maybeNames) =>
      n.copy(
        environmentRequirement = maybeReq,
        pathNames = maybeNames
      )
    }
  }

  def addBuilding(
      mid: MuseumId,
      building: Building
  )(
      implicit currUsr: AuthenticatedUser,
      ec: ExecutionContext
  ): Future[MusitResult[Option[Building]]] = {
    addNode[Building](
      mid = mid,
      node = building.copy(
        updatedBy = Some(currUsr.id),
        updatedDate = Some(dateTimeNow)
      ),
      insert = buildingDao.insert,
      setEnvReq = (node, maybeEnvReq) => node.copy(environmentRequirement = maybeEnvReq),
      updateWithPath = (id, path) => buildingDao.setPath(id, path),
      getNode = getBuildingByDatabaseId
    )
  }

  def updateBuilding(
      mid: MuseumId,
      id: StorageNodeId,
      building: Building
  )(
      implicit currUsr: AuthenticatedUser,
      ec: ExecutionContext
  ): Future[MusitResult[Option[Building]]] = {
    val updateBuilding = building.copy(
      updatedBy = Some(currUsr.id),
      updatedDate = Some(dateTimeNow)
    )
    building.id.map { dbId =>
      buildingDao.update(mid, id, updateBuilding).flatMap {
        case MusitSuccess(maybeRes) =>
          maybeRes.map { _ =>
            for {
              _ <- updateBuilding.environmentRequirement
                    .map(er => saveEnvReq(mid, dbId, er))
                    .getOrElse(Future.successful(None))
              node <- getBuildingByDatabaseId(mid, dbId)
            } yield {
              node
            }
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
