package services.storage

import models.storage.nodes.Organisation
import no.uio.musit.MusitResults.{
  MusitError,
  MusitResult,
  MusitSuccess,
  MusitValidationError
}
import no.uio.musit.models.{MuseumId, StorageNodeDatabaseId, StorageNodeId}
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.time.dateTimeNow
import repositories.storage.dao.nodes.OrganisationDao

import scala.concurrent.{ExecutionContext, Future}

trait OrganisationServiceOps { self: NodeService =>

  val orgDao: OrganisationDao

  def getOrganisationById(
      mid: MuseumId,
      id: StorageNodeDatabaseId
  )(implicit ec: ExecutionContext): Future[MusitResult[Option[Organisation]]] = {
    val eventuallyOrg = orgDao.getById(mid, id)
    getNode(mid, eventuallyOrg) { (n, maybeReq, maybeNames) =>
      n.copy(
        environmentRequirement = maybeReq,
        pathNames = maybeNames
      )
    }
  }

  def addOrganisation(
      mid: MuseumId,
      organisation: Organisation
  )(
      implicit currUsr: AuthenticatedUser,
      ec: ExecutionContext
  ): Future[MusitResult[Option[Organisation]]] = {
    addNode[Organisation](
      mid = mid,
      node = organisation.copy(
        updatedBy = Some(currUsr.id),
        updatedDate = Some(dateTimeNow)
      ),
      insert = orgDao.insert,
      setEnvReq = (node, mer) => node.copy(environmentRequirement = mer),
      updateWithPath = (id, path) => orgDao.setPath(id, path),
      getNode = getOrganisationById
    )
  }

  def updateOrganisation(
      mid: MuseumId,
      id: StorageNodeId,
      organisation: Organisation
  )(
      implicit currUsr: AuthenticatedUser,
      ec: ExecutionContext
  ): Future[MusitResult[Option[Organisation]]] = {
    val updateOrg = organisation.copy(
      updatedBy = Some(currUsr.id),
      updatedDate = Some(dateTimeNow)
    )

    organisation.id.map { dbId =>
      orgDao.update(mid, id, updateOrg).flatMap {
        case MusitSuccess(maybeRes) =>
          maybeRes.map { _ =>
            for {
              _ <- updateOrg.environmentRequirement
                    .map(er => saveEnvReq(mid, dbId, er))
                    .getOrElse(Future.successful(None))
              node <- getOrganisationById(mid, dbId)
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
