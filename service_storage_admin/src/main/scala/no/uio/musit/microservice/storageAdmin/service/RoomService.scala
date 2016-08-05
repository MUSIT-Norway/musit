package no.uio.musit.microservice.storageAdmin.service

import no.uio.musit.microservice.storageAdmin.dao.RoomDao
import no.uio.musit.microservice.storageAdmin.domain.dto.StorageUnitDTO
import no.uio.musit.microservice.storageAdmin.domain.{ Room, Storage, StorageUnit }
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.utils.ServiceHelper

trait RoomService {
  def create(storageUnit: StorageUnitDTO, storageRoom: Room): MusitFuture[Storage] =
    ServiceHelper.daoInsert(RoomDao.insertRoom(storageUnit, storageRoom))

  def updateRoomByID(id: Long, room: Room) =
    RoomDao.updateRoom(id, room)
}

object RoomService extends RoomService
