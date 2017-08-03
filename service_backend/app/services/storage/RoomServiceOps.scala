package services.storage

import models.storage.nodes.Room
import no.uio.musit.MusitResults.{
  MusitError,
  MusitResult,
  MusitSuccess,
  MusitValidationError
}
import no.uio.musit.models.{MuseumId, StorageNodeDatabaseId, StorageNodeId}
import no.uio.musit.security.AuthenticatedUser
import no.uio.musit.time.dateTimeNow
import repositories.storage.dao.nodes.RoomDao

import scala.concurrent.{ExecutionContext, Future}

trait RoomServiceOps { self: NodeService =>

  val roomDao: RoomDao

  def getRoomByDatabaseId(
      mid: MuseumId,
      id: StorageNodeDatabaseId
  )(
      implicit currUsr: AuthenticatedUser,
      ec: ExecutionContext
  ): Future[MusitResult[Option[Room]]] = {
    val eventuallyRoom = roomDao.getById(mid, id)
    getNode(mid, eventuallyRoom) { (n, maybeReq, maybeNames) =>
      n.copy(
        environmentRequirement = maybeReq,
        pathNames = maybeNames
      )
    }
  }

  def addRoom(
      mid: MuseumId,
      room: Room
  )(
      implicit currUsr: AuthenticatedUser,
      ec: ExecutionContext
  ): Future[MusitResult[Option[Room]]] = {
    addNode[Room](
      mid = mid,
      node = room.copy(
        updatedBy = Some(currUsr.id),
        updatedDate = Some(dateTimeNow)
      ),
      insert = roomDao.insert,
      setEnvReq = (node, mer) => node.copy(environmentRequirement = mer),
      updateWithPath = (id, path) => roomDao.setPath(id, path),
      getNode = getRoomByDatabaseId
    )
  }

  def updateRoom(
      mid: MuseumId,
      id: StorageNodeId,
      room: Room
  )(
      implicit currUsr: AuthenticatedUser,
      ec: ExecutionContext
  ): Future[MusitResult[Option[Room]]] = {
    val updateRoom = room.copy(
      updatedBy = Some(currUsr.id),
      updatedDate = Some(dateTimeNow)
    )

    room.id.map { dbId =>
      roomDao.update(mid, id, updateRoom).flatMap {
        case MusitSuccess(maybeRes) =>
          maybeRes.map { _ =>
            for {
              _ <- updateRoom.environmentRequirement
                    .map(er => saveEnvReq(mid, dbId, er))
                    .getOrElse(Future.successful(None))
              node <- getRoomByDatabaseId(mid, dbId)
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
