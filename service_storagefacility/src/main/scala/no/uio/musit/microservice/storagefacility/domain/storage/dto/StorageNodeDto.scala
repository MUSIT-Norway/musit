package no.uio.musit.microservice.storagefacility.domain.storage.dto

import no.uio.musit.microservice.storagefacility.domain.MuseumId
import no.uio.musit.microservice.storagefacility.domain.storage._

sealed trait StorageNodeDto

sealed trait SpecializedStorageNode

case class StorageUnitDto(
  id: Option[StorageNodeId],
  storageType: StorageType,
  name: String,
  area: Option[Double],
  areaTo: Option[Double],
  isPartOf: Option[StorageNodeId],
  height: Option[Double],
  heightTo: Option[Double],
  groupRead: Option[String],
  groupWrite: Option[String],
  isDeleted: Option[Boolean],
  museumId: MuseumId
) extends StorageNodeDto

case class ExtendedStorageNode[T <: SpecializedStorageNode](
  storageUnitDto: StorageUnitDto,
  extension: T
) extends StorageNodeDto

case class RootDto(
    id: Option[StorageNodeId],
    name: String,
    storageType: StorageType,
    museumId: MuseumId
) extends StorageNodeDto {

  /**
   * Hack to convert into a StorageUnitDto for DB inserts
   */
  def asStorageUnit(mid: MuseumId): StorageUnitDto =
    StorageUnitDto(
      id = id,
      name = name,
      area = None,
      areaTo = None,
      isPartOf = None,
      height = None,
      heightTo = None,
      groupRead = None,
      groupWrite = None,
      isDeleted = None,
      storageType = storageType,
      museumId = mid
    )

}

case class BuildingDto(
  id: Option[StorageNodeId],
  address: Option[String]
) extends SpecializedStorageNode

case class OrganisationDto(
  id: Option[StorageNodeId],
  address: Option[String]
) extends SpecializedStorageNode

case class RoomDto(
  id: Option[StorageNodeId],
  perimeterSecurity: Option[Boolean],
  theftProtection: Option[Boolean],
  fireProtection: Option[Boolean],
  waterDamageAssessment: Option[Boolean],
  routinesAndContingencyPlan: Option[Boolean],
  relativeHumidity: Option[Boolean],
  temperatureAssessment: Option[Boolean],
  lightingCondition: Option[Boolean],
  preventiveConservation: Option[Boolean]
) extends SpecializedStorageNode

object StorageNodeDto {

  def toStorageNode[T <: StorageNodeDto](dto: T) =
    dto match {
      case root: RootDto =>
        toRoot(root)

      case stu: StorageUnitDto =>
        toStorageUnit(stu)

      case ext: ExtendedStorageNode[_] =>
        // Need to match on extension type due to type erasure in the above
        // case pattern. There are ways to remedy this. But isn't worth it for
        // this simple use-case
        ext.extension match {
          case buildExt: BuildingDto =>
            toBuilding(
              ExtendedStorageNode[BuildingDto](ext.storageUnitDto, buildExt)
            )

          case roomExt: RoomDto =>
            toRoom(
              ExtendedStorageNode[RoomDto](ext.storageUnitDto, roomExt)
            )

          case orgExt: OrganisationDto =>
            toOrganisation(
              ExtendedStorageNode[OrganisationDto](ext.storageUnitDto, orgExt)
            )
        }
    }

  def fromStorageNode[T <: StorageNode](mid: MuseumId, stu: T): StorageNodeDto =
    stu match {
      case root: Root => fromRoot(mid, root)
      case su: StorageUnit => fromStorageUnit(mid, su)
      case b: Building => fromBuilding(mid, b)
      case r: Room => fromRoom(mid, r)
    }

  def toGenericStorageNode(su: StorageUnitDto): GenericStorageNode =
    GenericStorageNode(
      id = su.id,
      name = su.name,
      area = su.area,
      areaTo = su.areaTo,
      height = su.height,
      heightTo = su.heightTo,
      isPartOf = su.isPartOf,
      groupRead = su.groupRead,
      groupWrite = su.groupWrite,
      environmentRequirement = None, // EnvRequirement is handled elsewhere
      storageType = su.storageType
    )

  def toRoot(r: RootDto): Root =
    Root(
      id = r.id
    )

  def toStorageUnit(su: StorageUnitDto): StorageUnit =
    StorageUnit(
      id = su.id,
      name = su.name,
      area = su.area,
      areaTo = su.areaTo,
      height = su.height,
      heightTo = su.heightTo,
      isPartOf = su.isPartOf,
      groupRead = su.groupRead,
      groupWrite = su.groupWrite,
      environmentRequirement = None // EnvRequirement is handled elsewhere
    )

  def toBuilding(ext: ExtendedStorageNode[BuildingDto]): Building = {
    Building(
      id = ext.extension.id,
      name = ext.storageUnitDto.name,
      area = ext.storageUnitDto.area,
      areaTo = ext.storageUnitDto.areaTo,
      height = ext.storageUnitDto.height,
      heightTo = ext.storageUnitDto.heightTo,
      isPartOf = ext.storageUnitDto.isPartOf,
      groupRead = ext.storageUnitDto.groupRead,
      groupWrite = ext.storageUnitDto.groupWrite,
      environmentRequirement = None, // EnvRequirement is handled elsewhere
      address = ext.extension.address
    )
  }

  def toOrganisation(ext: ExtendedStorageNode[OrganisationDto]): Organisation = {
    Organisation(
      id = ext.extension.id,
      name = ext.storageUnitDto.name,
      area = ext.storageUnitDto.area,
      areaTo = ext.storageUnitDto.areaTo,
      height = ext.storageUnitDto.height,
      heightTo = ext.storageUnitDto.heightTo,
      isPartOf = ext.storageUnitDto.isPartOf,
      groupRead = ext.storageUnitDto.groupRead,
      groupWrite = ext.storageUnitDto.groupWrite,
      environmentRequirement = None, // EnvRequirement is handled elsewhere
      address = ext.extension.address
    )
  }

  def toRoom(ext: ExtendedStorageNode[RoomDto]): Room = {
    Room(
      id = ext.extension.id,
      name = ext.storageUnitDto.name,
      area = ext.storageUnitDto.area,
      areaTo = ext.storageUnitDto.areaTo,
      height = ext.storageUnitDto.height,
      heightTo = ext.storageUnitDto.heightTo,
      isPartOf = ext.storageUnitDto.isPartOf,
      groupRead = ext.storageUnitDto.groupRead,
      groupWrite = ext.storageUnitDto.groupWrite,
      environmentRequirement = None, // EnvRequirement is handled elsewhere
      securityAssessment = SecurityAssessment(
        perimeter = ext.extension.perimeterSecurity,
        theftProtection = ext.extension.theftProtection,
        fireProtection = ext.extension.fireProtection,
        waterDamage = ext.extension.waterDamageAssessment,
        routinesAndContingencyPlan = ext.extension.routinesAndContingencyPlan
      ),
      environmentAssessment = EnvironmentAssessment(
        relativeHumidity = ext.extension.relativeHumidity,
        temperature = ext.extension.temperatureAssessment,
        lightingCondition = ext.extension.lightingCondition,
        preventiveConservation = ext.extension.preventiveConservation
      )
    )
  }

  def fromRoot(mid: MuseumId, r: Root): RootDto =
    RootDto(
      id = r.id,
      name = r.name,
      storageType = r.storageType,
      museumId = mid.underlying
    )

  def fromStorageUnit(
    mid: MuseumId,
    su: StorageUnit,
    id: Option[StorageNodeId] = None
  ): StorageUnitDto =
    StorageUnitDto(
      id = id.orElse(su.id),
      name = su.name,
      area = su.area,
      areaTo = su.areaTo,
      isPartOf = su.isPartOf,
      height = su.height,
      heightTo = su.heightTo,
      groupRead = su.groupRead,
      groupWrite = su.groupWrite,
      isDeleted = Some(false),
      storageType = su.storageType,
      museumId = mid.underlying
    )

  def fromBuilding(
    mid: MuseumId,
    b: Building,
    id: Option[StorageNodeId] = None
  ): ExtendedStorageNode[BuildingDto] =
    ExtendedStorageNode(
      storageUnitDto = StorageUnitDto(
        id = id.orElse(b.id),
        name = b.name,
        area = b.area,
        areaTo = b.areaTo,
        isPartOf = b.isPartOf,
        height = b.height,
        heightTo = b.heightTo,
        groupRead = b.groupRead,
        groupWrite = b.groupWrite,
        isDeleted = Some(false),
        storageType = b.storageType,
        museumId = mid.underlying
      ),
      extension = BuildingDto(
        id = id.orElse(b.id),
        address = b.address
      )
    )

  def fromOrganisation(
    mid: MuseumId,
    o: Organisation,
    id: Option[StorageNodeId] = None
  ): ExtendedStorageNode[OrganisationDto] =
    ExtendedStorageNode(
      storageUnitDto = StorageUnitDto(
        id = id.orElse(o.id),
        name = o.name,
        area = o.area,
        areaTo = o.areaTo,
        isPartOf = o.isPartOf,
        height = o.height,
        heightTo = o.heightTo,
        groupRead = o.groupRead,
        groupWrite = o.groupWrite,
        isDeleted = Some(false),
        storageType = o.storageType,
        museumId = mid.underlying
      ),
      extension = OrganisationDto(
        id = id.orElse(o.id),
        address = o.address
      )
    )

  def fromRoom(
    mid: MuseumId,
    r: Room,
    id: Option[StorageNodeId] = None
  ): ExtendedStorageNode[RoomDto] =
    ExtendedStorageNode(
      storageUnitDto = StorageUnitDto(
        id = id.orElse(r.id),
        name = r.name,
        area = r.area,
        areaTo = r.areaTo,
        isPartOf = r.isPartOf,
        height = r.height,
        heightTo = r.heightTo,
        groupRead = r.groupRead,
        groupWrite = r.groupWrite,
        isDeleted = Some(false),
        storageType = r.storageType,
        museumId = mid.underlying
      ),
      extension = RoomDto(
        id = id.orElse(r.id),
        perimeterSecurity = r.securityAssessment.perimeter,
        theftProtection = r.securityAssessment.theftProtection,
        fireProtection = r.securityAssessment.fireProtection,
        waterDamageAssessment = r.securityAssessment.waterDamage,
        routinesAndContingencyPlan = r.securityAssessment.routinesAndContingencyPlan,
        relativeHumidity = r.environmentAssessment.relativeHumidity,
        temperatureAssessment = r.environmentAssessment.temperature,
        lightingCondition = r.environmentAssessment.lightingCondition,
        preventiveConservation = r.environmentAssessment.preventiveConservation
      )
    )
}