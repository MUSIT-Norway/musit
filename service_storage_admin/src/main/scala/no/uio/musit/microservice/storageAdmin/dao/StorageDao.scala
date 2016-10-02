package no.uio.musit.microservice.storageAdmin.dao

import com.google.inject.Inject
import no.uio.musit.microservice.storageAdmin.domain.{NodePath, Storage}
import no.uio.musit.microservice.storageAdmin.domain.dto._
import no.uio.musit.microservice.storageAdmin.service.{BuildingService, RoomService}
import no.uio.musit.microservices.common.extensions.FutureExtensions.{MusitFuture, _}
import no.uio.musit.microservices.common.utils.ErrorHelper

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class StorageDao @Inject() (
    storageUnitDao: StorageUnitDao,
    roomDao: RoomDao,
    buildingDao: BuildingDao,
    organisationDao: OrganisationDao,
    envReqDao: EnvReqDao
) extends Object with StorageDtoConverter {

  def getStorageNodeDtoById(id: Long) =
    storageUnitDao.getStorageNodeDtoByIdAsMusitFuture(id)

  private def getBuildingById(id: Long): MusitFuture[BuildingDTO] =
    buildingDao.getBuildingById(id).toMusitFuture(ErrorHelper.notFound(s"Unknown storageBuilding with id: $id"))

  private def getOrganisationById(id: Long): MusitFuture[OrganisationDTO] =
    organisationDao.getOrganisationById(id).toMusitFuture(ErrorHelper.notFound(s"Unknown storageOrganisation with id: $id"))

  private def getRoomById(id: Long): MusitFuture[RoomDTO] =
    roomDao.getRoomById(id).toMusitFuture(ErrorHelper.notFound(s"Unknown storageRoom with id: $id"))

  def getByNode(storageNode: StorageNodeDTO): MusitFuture[Storage] = {
    val id = storageNode.id.get
    val optFutOptEnvReq = storageNode.latestEnvReqId.map {
      envReqId => envReqDao.getById(envReqId)
    }
    val noneEnvReq: Option[EnvReqDto] = None
    val futOptEnvReq = optFutOptEnvReq.fold {
      Future.successful(noneEnvReq)
    } { identity }

    val musFutOptEnvReq: MusitFuture[Option[EnvReqDto]] = futOptEnvReq.map(Right(_))
    musFutOptEnvReq.musitFutureFlatMap { optEnvReqDto =>

      storageNode.storageType match {
        case StorageType.StorageUnit => MusitFuture.successful(fromDto(CompleteStorageUnitDto(storageNode, optEnvReqDto)))
        case StorageType.Building => getBuildingById(id).musitFutureMap(storageBuilding =>
          fromDto(CompleteBuildingDto(storageNode, storageBuilding, optEnvReqDto)))
        case StorageType.Organisation => getOrganisationById(id).musitFutureMap(storageOrganisation =>
          fromDto(CompleteOrganisationDto(storageNode, storageOrganisation, optEnvReqDto)))
        case StorageType.Room => getRoomById(id).musitFutureMap(storageRoom =>
          fromDto(CompleteRoomDto(storageNode, storageRoom, optEnvReqDto)))
      }
    }
  }

  def getById(id: Long): MusitFuture[Storage] = {
    val musitFutureStorageNode = getStorageNodeDtoById(id)
    musitFutureStorageNode.musitFutureFlatMap { storageNode =>
      getByNode(storageNode)
    }
  }
}
