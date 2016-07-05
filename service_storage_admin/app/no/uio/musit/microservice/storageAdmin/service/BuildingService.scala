package no.uio.musit.microservice.storageAdmin.service

import no.uio.musit.microservice.storageAdmin.dao.BuildingDao
import no.uio.musit.microservice.storageAdmin.domain.dto.StorageUnitDTO
import no.uio.musit.microservice.storageAdmin.domain.{ Building, Storage, StorageUnit }
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.utils.ServiceHelper

trait BuildingService {
  def create(storageUnit: StorageUnitDTO, storageBuilding: Building): MusitFuture[Storage] =
    ServiceHelper.daoInsert(BuildingDao.insertBuilding(storageUnit, storageBuilding))

  def updateBuildingByID(id: Long, building: Building) =
    BuildingDao.updateBuilding(id, building)
}

object BuildingService extends BuildingService
