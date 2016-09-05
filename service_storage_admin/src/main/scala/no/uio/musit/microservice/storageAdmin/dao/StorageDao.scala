package no.uio.musit.microservice.storageAdmin.dao

import com.google.inject.Inject
import no.uio.musit.microservice.storageAdmin.domain.Storage
import no.uio.musit.microservice.storageAdmin.domain.dto._
import no.uio.musit.microservice.storageAdmin.service.{BuildingService, RoomService}
import no.uio.musit.microservices.common.extensions.FutureExtensions.{MusitFuture, _}
import no.uio.musit.microservices.common.utils.ErrorHelper

import scala.concurrent.Future

class StorageDao  @Inject() (
                             storageUnitDao: StorageUnitDao,
                             roomDao: RoomDao,
                             buildingDao: BuildingDao
                           ) extends Object with StorageDtoConverter {

  private def getStorageNodeOnly(id: Long) =
    storageUnitDao.getStorageUnitOnlyById(id).toMusitFuture(storageUnitDao.storageUnitNotFoundError(id))


  private def getBuildingById(id: Long): MusitFuture[BuildingDTO] =
    buildingDao.getBuildingById(id).toMusitFuture(ErrorHelper.notFound(s"Unknown storageBuilding with id: $id"))


  private def getRoomById(id: Long): MusitFuture[RoomDTO] =
    roomDao.getRoomById(id).toMusitFuture(ErrorHelper.notFound(s"Unknown storageRoom with id: $id"))


  def getByNode(storageNode: StorageNodeDTO) = {
    val id = storageNode.id.get
    storageNode.storageType match {
      case StorageType.StorageUnit => MusitFuture.successful(fromDto(CompleteStorageUnitDto(storageNode)))
      case StorageType.Building => getBuildingById(id).musitFutureMap(storageBuilding => fromDto(CompleteBuildingDto(storageNode, storageBuilding)))
      case StorageType.Room => getRoomById(id).musitFutureMap(storageRoom => fromDto(CompleteRoomDto(storageNode, storageRoom)))
    }
  }

  def getById(id: Long): MusitFuture[Storage] = {
    val musitFutureStorageNode = getStorageNodeOnly(id)
    musitFutureStorageNode.musitFutureFlatMap { storageNode =>
      getByNode(storageNode)
    }
  }
}
