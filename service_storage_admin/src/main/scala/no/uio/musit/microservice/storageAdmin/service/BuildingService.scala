package no.uio.musit.microservice.storageAdmin.service

import com.google.inject.Inject
import no.uio.musit.microservice.storageAdmin.dao.BuildingDao
import no.uio.musit.microservice.storageAdmin.domain.dto.StorageUnitDTO
import no.uio.musit.microservice.storageAdmin.domain.{Building, Storage}
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.utils.ServiceHelper

class BuildingService @Inject()(buildingDao: BuildingDao) {
  def create(storageUnit: StorageUnitDTO, storageBuilding: Building): MusitFuture[Storage] =
    ServiceHelper.daoInsert(buildingDao.insertBuilding(storageUnit, storageBuilding))

  def updateBuildingByID(id: Long, building: Building) =
    buildingDao.updateBuilding(id, building)

  def getBuildingById(id: Long) =
    buildingDao.getBuildingById(id)
}
