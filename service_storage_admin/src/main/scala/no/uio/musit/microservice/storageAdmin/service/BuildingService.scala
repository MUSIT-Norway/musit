package no.uio.musit.microservice.storageAdmin.service

import com.google.inject.Inject
import no.uio.musit.microservice.storageAdmin.dao.{BuildingDao, EnvReqDao}
import no.uio.musit.microservice.storageAdmin.domain.dto.{StorageDtoConverter, StorageNodeDTO}
import no.uio.musit.microservice.storageAdmin.domain.{Building, Storage}
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.utils.ServiceHelper

class BuildingService @Inject() (buildingDao: BuildingDao,
                                 envReqDao: EnvReqDao) extends Object with StorageDtoConverter {
  def create(storageBuilding: Building): MusitFuture[Storage] = {
    val buildingDto = buildingToDto(storageBuilding)
    buildingDao.insertBuilding(buildingDto).toMusitFuture
  }

  def updateBuildingByID(id: Long, building: Building) =
    buildingDao.updateBuilding(id, building)

  def getBuildingById(id: Long) =
    buildingDao.getBuildingById(id)
}
