package no.uio.musit.microservice.storageAdmin.service

import com.google.inject.Inject
import no.uio.musit.microservice.storageAdmin.dao.RoomDao
import no.uio.musit.microservice.storageAdmin.domain.dto.StorageUnitDTO
import no.uio.musit.microservice.storageAdmin.domain.{ Room, Storage }
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.utils.ServiceHelper

class RoomService @Inject() (roomDao: RoomDao) {
  def create(storageUnit: StorageUnitDTO, storageRoom: Room): MusitFuture[Storage] =
    ServiceHelper.daoInsert(roomDao.insertRoom(storageUnit, storageRoom))

  def updateRoomByID(id: Long, room: Room) =
    roomDao.updateRoom(id, room)

  def getRoomById(id: Long) =
    roomDao.getRoomById(id)
}