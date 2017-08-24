package repositories.storage.dao.nodes

import com.google.inject.{Inject, Singleton}
import models.storage.nodes.Building
import models.storage.nodes.dto.{BuildingDto, ExtendedStorageNode, StorageNodeDto}
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
class BuildingDao @Inject()(
    implicit
    val dbConfigProvider: DatabaseConfigProvider,
    val ec: ExecutionContext
) extends StorageTables {

  import profile.api._

  val logger = Logger(classOf[BuildingDao])

  private def updateAction(
      id: StorageNodeDatabaseId,
      building: BuildingDto
  ): DBIO[Int] = buildingTable.filter(_.id === id).update(building)

  private def insertAction(buildingDto: BuildingDto): DBIO[Int] = {
    buildingTable += buildingDto
  }

  /**
   * TODO: Document me!!!
   */
  def getById(
      mid: MuseumId,
      id: StorageNodeDatabaseId
  ): Future[MusitResult[Option[Building]]] = {
    val action = for {
      maybeUnitDto     <- getNonRootByDatabaseIdAction(mid, id)
      maybeBuildingDto <- buildingTable.filter(_.id === id).result.headOption
    } yield {
      // Map the results into an ExtendedStorageNode type
      maybeUnitDto.flatMap(u => maybeBuildingDto.map(b => ExtendedStorageNode(u, b)))
    }
    // Execute the query
    db.run(action)
      .map(res => MusitSuccess(res.map(StorageNodeDto.toBuilding)))
      .recover(nonFatal(s"Unable to query by id museumID $mid and storageNodeId $id"))
  }

  /**
   * TODO: Document me!!!
   */
  def update(
      mid: MuseumId,
      id: StorageNodeId,
      building: Building
  ): Future[MusitResult[Option[Int]]] = {
    val extDto = StorageNodeDto.fromBuilding(mid, building, uuid = Some(id))
    val action = for {
      unitsUpdated <- updateNodeAction(mid, id, extDto.storageUnitDto)
      buildingsUpdated <- {
        if (unitsUpdated > 0) {
          building.id.map(bid => updateAction(bid, extDto.extension)).getOrElse {
            DBIO.successful[Int](0)
          }
        } else {
          DBIO.successful[Int](0)
        }
      }
    } yield buildingsUpdated

    db.run(action.transactionally)
      .map {
        case res: Int if res == 1 => MusitSuccess(Some(res))
        case res: Int if res == 0 => MusitSuccess(None)
        case res: Int =>
          val msg = wrongNumUpdatedRows(id, res)
          logger.warn(msg)
          MusitDbError(msg)

      }
      .recover(nonFatal(s"There was an error updating building $id"))
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
      case res: Int if res == 1 =>
        MusitSuccess(())

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
      building: Building
  ): Future[MusitResult[StorageNodeDatabaseId]] = {
    val extendedDto = StorageNodeDto.fromBuilding(mid, building)
    val query = for {
      nodeId    <- insertNodeAction(extendedDto.storageUnitDto)
      extWithId <- DBIO.successful(extendedDto.extension.copy(id = Some(nodeId)))
      n         <- insertAction(extWithId)
    } yield {
      nodeId
    }

    db.run(query.transactionally)
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"Unable to insert building with museumId $mid"))
  }

}
