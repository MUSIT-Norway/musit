package no.uio.musit.microservice.storageAdmin.domain.dto

import no.uio.musit.microservice.storageAdmin.domain.NodePath

/* We have three types of storage nodes:

Room
Building
StorageUnit

The common properties are stored in the StorageNode table. StorageUnit has no extra properties, so no explicit table for StorageUnit.

 */

sealed trait StorageDTO {
  val id: Option[Long]
}

case class BuildingDTO(
  id: Option[Long],
  address: Option[String]
) extends StorageDTO

case class OrganisationDTO(
  id: Option[Long],
  address: Option[String]
) extends StorageDTO

case class RoomDTO(
  id: Option[Long],
  perimeterSecurity: Option[Boolean],
  theftProtection: Option[Boolean],
  fireProtection: Option[Boolean],
  waterDamageAssessment: Option[Boolean],
  routinesAndContingencyPlan: Option[Boolean],
  relativeHumidity: Option[Boolean],
  temperatureAssessment: Option[Boolean],
  lightingCondition: Option[Boolean],
  preventiveConservation: Option[Boolean]
) extends StorageDTO

case class StorageNodeDTO(
  id: Option[Long],
  name: String,
  area: Option[Double],
  areaTo: Option[Double],
  height: Option[Double],
  heightTo: Option[Double],
  isPartOf: Option[Long],
  nodePath: NodePath,
  groupRead: Option[String],
  groupWrite: Option[String],
  latestMoveId: Option[Long],
  latestEnvReqId: Option[Long],
  isDeleted: Boolean,
  storageType: StorageType
) extends StorageDTO

sealed trait CompleteStorageNodeDto {
  val storageNode: StorageNodeDTO
  val envReqDto: Option[EnvReqDto]

  def id = storageNode.id
}

case class CompleteBuildingDto(storageNode: StorageNodeDTO, buildingDto: BuildingDTO, envReqDto: Option[EnvReqDto]) extends CompleteStorageNodeDto
case class CompleteOrganisationDto(storageNode: StorageNodeDTO, organisationDto: OrganisationDTO, envReqDto: Option[EnvReqDto]) extends CompleteStorageNodeDto
case class CompleteRoomDto(storageNode: StorageNodeDTO, roomDto: RoomDTO, envReqDto: Option[EnvReqDto]) extends CompleteStorageNodeDto
case class CompleteStorageUnitDto(storageNode: StorageNodeDTO, envReqDto: Option[EnvReqDto]) extends CompleteStorageNodeDto

