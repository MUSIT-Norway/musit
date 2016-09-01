package no.uio.musit.microservice.storagefacility.service

import com.google.inject.Inject
import no.uio.musit.microservice.storagefacility.dao.storage.RoomDao
import no.uio.musit.microservice.storagefacility.domain.storage.dto.StorageUnitDto
import no.uio.musit.microservice.storagefacility.domain.storage.{ Room, Storage, StorageNodeId }
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.utils.ServiceHelper

/**
 * TODO: Document me!!!
 */
class RoomService @Inject() (roomDao: RoomDao) {

  /**
   * TODO: Document me!!!
   */
  def create(storageUnit: StorageUnitDto, storageRoom: Room): MusitFuture[Storage] =
    ServiceHelper.daoInsert(roomDao.insertRoom(storageUnit, storageRoom))

  /**
   * TODO: Document me!!! + id: Long should be id: StorageNodeId
   */
  def updateRoomByID(id: Long, room: Room) =
    roomDao.updateRoom(StorageNodeId(id), room)

  /**
   * TODO: Document me!!! + id: Long should be id: StorageNodeId
   */
  def getRoomById(id: Long) =
    roomDao.getRoomById(StorageNodeId(id))
}