package repositories.storage.dao.nodes

import com.google.inject.{Inject, Singleton}
import models.storage.nodes.Room
import models.storage.nodes.dto.{ExtendedStorageNode, RoomDto, StorageNodeDto}
import no.uio.musit.MusitResults.{MusitDbError, MusitResult, MusitSuccess}
import no.uio.musit.models.{MuseumId, NodePath, StorageNodeDatabaseId, StorageNodeId}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import repositories.storage.dao.StorageTables

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RoomDao @Inject()(
    implicit
    val dbConfigProvider: DatabaseConfigProvider,
    val ec: ExecutionContext
) extends StorageTables {

  import profile.api._

  val logger = Logger(classOf[RoomDao])

  private def updateAction(id: StorageNodeDatabaseId, room: RoomDto): DBIO[Int] = {
    roomTable.filter(_.id === id).update(room)
  }

  private def insertAction(roomDto: RoomDto): DBIO[Int] = roomTable += roomDto

  /**
   * TODO: Document me!!!
   */
  def getById(
      mid: MuseumId,
      id: StorageNodeDatabaseId
  ): Future[MusitResult[Option[Room]]] = {
    val action = for {
      maybeUnitDto <- getNonRootByDatabaseIdAction(mid, id)
      maybeRoomDto <- roomTable.filter(_.id === id).result.headOption
    } yield {
      maybeUnitDto.flatMap(u => maybeRoomDto.map(r => ExtendedStorageNode(u, r)))
    }
    db.run(action)
      .map(res => MusitSuccess(res.map(StorageNodeDto.toRoom)))
      .recover(nonFatal(s"Unable to get room for museumId $mid and storageId $id"))

  }

  /**
   * TODO: Document me!!!
   */
  def update(
      mid: MuseumId,
      id: StorageNodeId,
      room: Room
  ): Future[MusitResult[Option[Int]]] = {
    val roomDto = StorageNodeDto.fromRoom(mid, room, uuid = Some(id))
    val action = for {
      unitsUpdated <- updateNodeAction(mid, id, roomDto.storageUnitDto)
      roomsUpdated <- {
        if (unitsUpdated > 0) {
          room.id.map(rid => updateAction(rid, roomDto.extension)).getOrElse {
            DBIO.successful[Int](0)
          }
        } else {
          DBIO.successful[Int](0)
        }
      }
    } yield roomsUpdated

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
   * Set the path for the given StoragNodeId
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
  def insert(mid: MuseumId, room: Room): Future[MusitResult[StorageNodeDatabaseId]] = {
    val extendedDto = StorageNodeDto.fromRoom(mid, room)
    val action = for {
      nodeId    <- insertNodeAction(extendedDto.storageUnitDto)
      extWithId <- DBIO.successful(extendedDto.extension.copy(id = Some(nodeId)))
      inserted  <- insertAction(extWithId)
    } yield nodeId

    db.run(action.transactionally)
      .map(MusitSuccess.apply)
      .recover(nonFatal(s"Unable to insert room with museumId $mid"))
  }

}
