package no.uio.musit.microservice.storagefacility.service

import com.google.inject.Inject
import no.uio.musit.microservice.storagefacility.dao.storage.BuildingDao
import no.uio.musit.microservice.storagefacility.domain.storage.dto.StorageUnitDto
import no.uio.musit.microservice.storagefacility.domain.storage.{ Building, Storage, StorageNodeId }
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.utils.ServiceHelper

/**
 * TODO: Document me!!!
 * @param buildingDao
 */
class BuildingService @Inject() (buildingDao: BuildingDao) {

  /**
   * TODO: Document me!!!
   */
  def create(storageUnit: StorageUnitDto, storageBuilding: Building): MusitFuture[Storage] =
    ServiceHelper.daoInsert(buildingDao.insertBuilding(storageUnit, storageBuilding))

  /**
   * TODO: Document me!!! + id: Long should be id: StorageNodeId
   */
  def updateBuildingByID(id: Long, building: Building) =
    buildingDao.updateBuilding(StorageNodeId(id), building)

  /**
   * TODO: Document me!!! + id: Long should be id: StorageNodeId
   */
  def getBuildingById(id: Long) =
    buildingDao.getBuildingById(StorageNodeId(id))
}
