package no.uio.musit.microservice.storagefacility.domain.storage.dto

import no.uio.musit.microservice.storagefacility.domain.storage._

//import no.uio.musit.microservices.common.linking.domain.Link

sealed trait StorageNodeDto

sealed trait SpecializedStorageNode

case class StorageUnitDto(
  id: Option[StorageNodeId],
  name: String,
  area: Option[Long],
  areaTo: Option[Long],
  isPartOf: Option[StorageNodeId],
  height: Option[Long],
  heightTo: Option[Long],
  groupRead: Option[String],
  groupWrite: Option[String],
  //  links: Option[Seq[Link]],
  isDeleted: Option[Boolean],
  storageType: StorageType
) extends StorageNodeDto

case class ExtendedStorageNode[T <: SpecializedStorageNode](
  storageUnitDto: StorageUnitDto,
  extension: T
) extends StorageNodeDto

case class BuildingDto(
  id: Option[StorageNodeId],
  address: Option[String]
) extends SpecializedStorageNode

case class RoomDto(
  id: Option[StorageNodeId],
  sikringSkallsikring: Option[Boolean],
  sikringTyverisikring: Option[Boolean],
  sikringBrannsikring: Option[Boolean],
  sikringVannskaderisiko: Option[Boolean],
  sikringRutineOgBeredskap: Option[Boolean],
  bevarLuftfuktOgTemp: Option[Boolean],
  bevarLysforhold: Option[Boolean],
  bevarPrevantKons: Option[Boolean]
) extends SpecializedStorageNode

object StorageNodeDto {

  def toStorageNode[T <: StorageNodeDto](dto: T) =
    dto match {
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
        }
    }

  def fromStorageNode[T <: StorageNode](stu: T): StorageNodeDto =
    stu match {
      case su: StorageUnit => fromStorageUnit(su)
      case b: Building => fromBuilding(b)
      case r: Room => fromRoom(r)
    }

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
      groupWrite = su.groupWrite
    //          links = stu.links
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
      //          links = ex.storageUnitDto.links,
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
      //          links = su.links,
      sikringSkallsikring = ext.extension.sikringSkallsikring,
      sikringBrannsikring = ext.extension.sikringBrannsikring,
      sikringTyverisikring = ext.extension.sikringTyverisikring,
      sikringVannskaderisiko = ext.extension.sikringVannskaderisiko,
      sikringRutineOgBeredskap = ext.extension.sikringRutineOgBeredskap,
      bevarLuftfuktOgTemp = ext.extension.bevarLuftfuktOgTemp,
      bevarLysforhold = ext.extension.bevarLysforhold,
      bevarPrevantKons = ext.extension.bevarPrevantKons
    )
  }

  def fromStorageUnit(su: StorageUnit): StorageUnitDto =
    StorageUnitDto(
      id = su.id,
      name = su.name,
      area = su.area,
      areaTo = su.areaTo,
      isPartOf = su.isPartOf,
      height = su.height,
      heightTo = su.heightTo,
      groupRead = su.groupRead,
      groupWrite = su.groupWrite,
      isDeleted = Some(false),
      storageType = su.storageType
    )

  def fromBuilding(b: Building): ExtendedStorageNode[BuildingDto] =
    ExtendedStorageNode(
      storageUnitDto = StorageUnitDto(
        id = b.id,
        name = b.name,
        area = b.area,
        areaTo = b.areaTo,
        isPartOf = b.isPartOf,
        height = b.height,
        heightTo = b.heightTo,
        groupRead = b.groupRead,
        groupWrite = b.groupWrite,
        isDeleted = Some(false),
        storageType = b.storageType
      ),
      extension = BuildingDto(
        id = b.id,
        address = b.address
      )
    )

  def fromRoom(r: Room): ExtendedStorageNode[RoomDto] =
    ExtendedStorageNode(
      storageUnitDto = StorageUnitDto(
        id = r.id,
        name = r.name,
        area = r.area,
        areaTo = r.areaTo,
        isPartOf = r.isPartOf,
        height = r.height,
        heightTo = r.heightTo,
        groupRead = r.groupRead,
        groupWrite = r.groupWrite,
        isDeleted = Some(false),
        storageType = r.storageType
      ),
      extension = RoomDto(
        id = r.id,
        sikringSkallsikring = r.sikringSkallsikring,
        sikringTyverisikring = r.sikringTyverisikring,
        sikringBrannsikring = r.sikringBrannsikring,
        sikringVannskaderisiko = r.sikringVannskaderisiko,
        sikringRutineOgBeredskap = r.sikringRutineOgBeredskap,
        bevarLuftfuktOgTemp = r.bevarLuftfuktOgTemp,
        bevarLysforhold = r.bevarLysforhold,
        bevarPrevantKons = r.bevarPrevantKons
      )
    )
}