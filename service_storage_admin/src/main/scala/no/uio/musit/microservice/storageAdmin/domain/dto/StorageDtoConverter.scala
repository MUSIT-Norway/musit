package no.uio.musit.microservice.storageAdmin.domain.dto

import no.uio.musit.microservice.storageAdmin.domain._

trait StorageDtoConverter {

  def fromDto[T <: CompleteStorageNodeDto](dto: T) =
    dto match {
      case stu: CompleteStorageUnitDto =>
        storageUnitFromDto(stu)
      case building: CompleteBuildingDto =>
        buildingFromDto(building)
      case room: CompleteRoomDto =>
        roomFromDto(room)
    }


  def toDto[T <: Storage](node: T) = {
    node match {
      case stu: StorageUnit =>
        storageUnitToDto(stu)
      case building: Building =>
        buildingToDto(building)
      case room: Room =>
        roomToDto(room)
    }
  }

  def storageNodeToDto(storage: Storage): StorageNodeDTO = {
    StorageNodeDTO(
      id = storage.id,
      name = storage.name,
      area = storage.area,
      areaTo = storage.areaTo,
      isPartOf = storage.isPartOf,
      height = storage.height,
      heightTo = storage.heightTo,
      groupRead = storage.groupRead,
      groupWrite = storage.groupWrite,
      latestMoveId = storage.latestMoveId,
      latestEnvReqId = storage.latestEnvReqId,
      links = storage.links,
      isDeleted = false, //Todo: Is this correct, can we assume this?
      storageType = StorageType.fromStorage(storage)
    )


  }
  def storageUnitToDto(storageUnit: StorageUnit): CompleteStorageUnitDto = {
    CompleteStorageUnitDto(storageNodeToDto(storageUnit))
  }

  def buildingToDto(building: Building): CompleteBuildingDto = {
    val buildingPart = BuildingDTO(building.id, building.address)
    CompleteBuildingDto(storageNodeToDto(building), buildingPart)
  }


  def roomToDto(room: Room): CompleteRoomDto = {
    val sec = room.securityAssessment
    val env = room.environmentAssessment
    val roomPart = RoomDTO(
      id = room.id,
      perimeterSecurity = sec.perimeterSecurity,
      theftProtection = sec.theftProtection,
      fireProtection = sec.fireProtection,
      waterDamageAssessment = sec.waterDamageAssessment,
      routinesAndContingencyPlan = sec.routinesAndContingencyPlan,
      relativeHumidity = env.relativeHumidity,
      temperatureAssessment = env.temperatureAssessment,
      lightingCondition = env.lightingCondition,
      preventiveConservation = env.preventiveConservation
    )
    CompleteRoomDto(storageNodeToDto(room), roomPart)
  }


  def roomFromDto(room: CompleteRoomDto): Room = {
    val nodePart = room.storageNode
    val roomPart = room.roomDto

    require(nodePart.id == roomPart.id)

    val secAssessment = SecurityAssessment(
      perimeterSecurity = roomPart.perimeterSecurity,
      fireProtection = roomPart.fireProtection,
      theftProtection = roomPart.theftProtection,
      waterDamageAssessment = roomPart.waterDamageAssessment,
      routinesAndContingencyPlan = roomPart.routinesAndContingencyPlan
    )
    val envAssessment = EnvironmentAssessment(
      relativeHumidity = roomPart.relativeHumidity,
      temperatureAssessment = roomPart.temperatureAssessment,
      lightingCondition = roomPart.lightingCondition,
      preventiveConservation = roomPart.preventiveConservation
    )
    Room(
      id = nodePart.id,
      name = nodePart.name,
      area = nodePart.area,
      areaTo = nodePart.areaTo,
      height = nodePart.height,
      heightTo = nodePart.heightTo,
      isPartOf = nodePart.isPartOf,
      groupRead = nodePart.groupRead,
      groupWrite = nodePart.groupWrite,
      latestMoveId = nodePart.latestMoveId,
      latestEnvReqId = nodePart.latestEnvReqId,
      links = nodePart.links,
      securityAssessment = secAssessment,
      environmentAssessment = envAssessment
    )
  }

  def buildingFromDto(building: CompleteBuildingDto) = {
    val nodePart = building.storageNode
    val buildingPart = building.buildingDto
    Building(
      id = nodePart.id,
      name = nodePart.name,
      area = nodePart.area,
      areaTo = nodePart.areaTo,
      height = nodePart.height,
      heightTo = nodePart.heightTo,
      isPartOf = nodePart.isPartOf,
      groupRead = nodePart.groupRead,
      groupWrite = nodePart.groupWrite,
      latestMoveId = nodePart.latestMoveId,
      latestEnvReqId = nodePart.latestEnvReqId,
      links = nodePart.links,
      address = buildingPart.address
    )
  }

  def storageUnitFromDto(completeStorageUnit: CompleteStorageUnitDto) = {
    val nodePart =  completeStorageUnit.storageNode
    StorageUnit(
      id = nodePart.id,
      name = nodePart.name,
      area = nodePart.area,
      areaTo = nodePart.areaTo,
      height = nodePart.height,
      heightTo = nodePart.heightTo,
      isPartOf = nodePart.isPartOf,
      groupRead = nodePart.groupRead,
      groupWrite = nodePart.groupWrite,
      latestMoveId = nodePart.latestMoveId,
      latestEnvReqId = nodePart.latestEnvReqId,
      links = nodePart.links
    )
  }

}
