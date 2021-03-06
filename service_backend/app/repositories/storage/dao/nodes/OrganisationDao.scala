package repositories.storage.dao.nodes

import com.google.inject.{Inject, Singleton}
import models.storage.nodes.Organisation
import models.storage.nodes.dto.{ExtendedStorageNode, OrganisationDto, StorageNodeDto}
import no.uio.musit.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
import no.uio.musit.models.{MuseumId, NodePath, StorageNodeDatabaseId, StorageNodeId}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import repositories.storage.dao.StorageTables

import scala.concurrent.{ExecutionContext, Future}

/**
 * TODO: Document me!!!
 */
@Singleton
class OrganisationDao @Inject()(
    implicit
    val dbConfigProvider: DatabaseConfigProvider,
    val ec: ExecutionContext
) extends StorageTables {

  import profile.api._

  val logger = Logger(classOf[OrganisationDao])

  private def updateAction(id: StorageNodeDatabaseId, org: OrganisationDto): DBIO[Int] = {
    organisationTable.filter(_.id === id).update(org)
  }

  private def insertAction(organisationDto: OrganisationDto): DBIO[Int] = {
    organisationTable += organisationDto
  }

  /**
   * TODO: Document me!!!
   */
  def getById(
      mid: MuseumId,
      id: StorageNodeDatabaseId
  ): Future[MusitResult[Option[Organisation]]] = {
    val action = for {
      maybeUnitDto <- getNonRootByDatabaseIdAction(mid, id)
      maybeOrgDto  <- organisationTable.filter(_.id === id).result.headOption
    } yield {
      // Map the results into an ExtendedStorageNode type
      maybeUnitDto.flatMap(u => maybeOrgDto.map(o => ExtendedStorageNode(u, o)))
    }
    // Execute the query
    db.run(action)
      .map(res => MusitSuccess(res.map(StorageNodeDto.toOrganisation)))
      .recover(
        nonFatal(s"Unable to get organisation for museumId $mid and storage node $id")
      )
  }

  /**
   * TODO: Document me!!!
   */
  def update(
      mid: MuseumId,
      id: StorageNodeId,
      organisation: Organisation
  ): Future[MusitResult[Option[Int]]] = {
    val extDto = StorageNodeDto.fromOrganisation(mid, organisation, uuid = Some(id))
    val action = for {
      unitsUpdated <- updateNodeAction(mid, id, extDto.storageUnitDto)
      orgsUpdated <- {
        if (unitsUpdated > 0) {
          organisation.id.map(oid => updateAction(oid, extDto.extension)).getOrElse {
            DBIO.successful[Int](0)
          }
        } else {
          DBIO.successful[Int](0)
        }
      }
    } yield orgsUpdated

    db.run(action.transactionally).map {
      case res: Int if res == 1 => MusitSuccess(Some(res))
      case res: Int if res == 0 => MusitSuccess(None)
      case res: Int =>
        val msg = wrongNumUpdatedRows(id, res)
        logger.warn(msg)
        MusitDbError(msg)
    }
  }

  /**
   * Updates the path for the given StoragNodeId
   *
   * @param id   the StorageNodeId to update
   * @param path the NodePath to set
   * @return MusitResult[Unit]
   */
  def setPath(id: StorageNodeDatabaseId, path: NodePath): Future[MusitResult[Unit]] = {
    db.run(updatePathAction(id, path)).map {
      case res: Int if res == 1 => MusitSuccess(())

      case res: Int =>
        val msg = wrongNumUpdatedRows(id, res)
        logger.warn(msg)
        MusitDbError(msg)
    }
  }

  /**
   * TODO: Document me!!!
   */
  def insert(
      mid: MuseumId,
      organisation: Organisation
  ): Future[MusitResult[StorageNodeDatabaseId]] = {
    val extendedDto = StorageNodeDto.fromOrganisation(mid, organisation)
    val query = for {
      nodeId    <- insertNodeAction(extendedDto.storageUnitDto)
      extWithId <- DBIO.successful(extendedDto.extension.copy(id = Some(nodeId)))
      n         <- insertAction(extWithId)
    } yield {
      nodeId
    }

    db.run(query.transactionally)
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"Unable to insert organisation for museumId $mid"))
  }

}
