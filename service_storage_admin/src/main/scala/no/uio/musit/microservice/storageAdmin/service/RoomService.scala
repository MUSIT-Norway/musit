package no.uio.musit.microservice.storageAdmin.service

import com.google.inject.Inject
import no.uio.musit.microservice.storageAdmin.dao.{ EnvReqDao, RoomDao }
import no.uio.musit.microservice.storageAdmin.domain.dto.{ StorageDtoConverter, StorageNodeDTO }
import no.uio.musit.microservice.storageAdmin.domain.{ Room, Storage }
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.utils.ServiceHelper

class RoomService @Inject() (
    roomDao: RoomDao,
    envReqDao: EnvReqDao
) extends Object with StorageDtoConverter {
  def create(storageRoom: Room): MusitFuture[Storage] = {
    roomDao.insertRoom(storageRoom).toMusitFuture
  }

  def updateRoomByID(id: Long, room: Room) =
    roomDao.updateRoom(id, room)

  def getRoomById(id: Long) =
    roomDao.getRoomById(id)
}