package no.uio.musit.microservice.storagefacility.service

import com.google.inject.Inject
import no.uio.musit.microservice.storagefacility.dao.storage.BuildingDao
import no.uio.musit.microservice.storagefacility.domain.storage.dto.StorageUnitDTO
import no.uio.musit.microservice.storagefacility.domain.storage.{ Building, Storage }
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.utils.ServiceHelper

class BuildingService @Inject() (buildingDao: BuildingDao) {
  def create(storageUnit: StorageUnitDTO, storageBuilding: Building): MusitFuture[Storage] =
    ServiceHelper.daoInsert(buildingDao.insertBuilding(storageUnit, storageBuilding))

  def updateBuildingByID(id: Long, building: Building) =
    buildingDao.updateBuilding(id, building)

  def getBuildingById(id: Long) =
    buildingDao.getBuildingById(id)
}
