package no.uio.musit.microservice.storageAdmin.domain

import no.uio.musit.microservices.common.linking.LinkService
import no.uio.musit.microservices.common.linking.domain.Link
import play.api.libs.json.OFormat
import julienrf.json.derived
import no.uio.musit.microservice.storageAdmin.domain.dto.{ BaseDTO, BuildingDTO, RoomDTO, StorageUnitDTO }
import play.api.libs.json.__
import shapeless.syntax.std.tuple._

sealed trait Storage {
  val id: Option[Long]
  val name: String
  val area: Option[Long]
  val areaTo: Option[Long]
  val isPartOf: Option[Long]
  val height: Option[Long]
  val heightTo: Option[Long]
  val groupRead: Option[String]
  val groupWrite: Option[String]
  val links: Option[Seq[Link]]
  val isDeleted: Option[Boolean]
  val storageType: StorageType
}

case class StorageUnit(
  id: Option[Long],
  name: String,
  area: Option[Long],
  areaTo: Option[Long],
  isPartOf: Option[Long],
  height: Option[Long],
  heightTo: Option[Long],
  groupRead: Option[String],
  groupWrite: Option[String],
  links: Option[Seq[Link]],
  isDeleted: Option[Boolean],
  storageType: StorageType = StorageType.StorageUnit
) extends Storage

object StorageUnit

case class Room(
    id: Option[Long],
    name: String,
    area: Option[Long],
    areaTo: Option[Long],
    isPartOf: Option[Long],
    height: Option[Long],
    heightTo: Option[Long],
    groupRead: Option[String],
    groupWrite: Option[String],
    links: Option[Seq[Link]],
    isDeleted: Option[Boolean],
    sikringSkallsikring: Option[Boolean],
    sikringTyverisikring: Option[Boolean],
    sikringBrannsikring: Option[Boolean],
    sikringVannskaderisiko: Option[Boolean],
    sikringRutineOgBeredskap: Option[Boolean],
    bevarLuftfuktOgTemp: Option[Boolean],
    bevarLysforhold: Option[Boolean],
    bevarPrevantKons: Option[Boolean]
) extends Storage {
  val storageType: StorageType = StorageType.Room
}

case class Building(
    id: Option[Long],
    name: String,
    area: Option[Long],
    areaTo: Option[Long],
    isPartOf: Option[Long],
    height: Option[Long],
    heightTo: Option[Long],
    groupRead: Option[String],
    groupWrite: Option[String],
    links: Option[Seq[Link]],
    isDeleted: Option[Boolean],
    address: Option[String]
) extends Storage {
  val storageType: StorageType = StorageType.Building
}

object Storage {

  implicit lazy val format: OFormat[Storage] = derived.flat.oformat((__ \ "type").format[String])

  def fromDTO[T <: BaseDTO](dto: T) =
    dto match {
      case stu: StorageUnitDTO =>
        StorageUnit(
          id = stu.id,
          name = stu.name,
          area = stu.area,
          areaTo = stu.areaTo,
          height = stu.height,
          heightTo = stu.heightTo,
          isPartOf = stu.isPartOf,
          groupRead = stu.groupRead,
          groupWrite = stu.groupWrite,
          isDeleted = stu.isDeleted,
          links = stu.links
        )
      case building: BuildingDTO =>
        Building(
          id = building.id,
          name = building.name,
          area = building.area,
          areaTo = building.areaTo,
          height = building.height,
          heightTo = building.heightTo,
          isPartOf = building.isPartOf,
          groupRead = building.groupRead,
          groupWrite = building.groupWrite,
          isDeleted = building.isDeleted,
          links = building.links,
          address = building.address
        )
      case room: RoomDTO =>
        Room(
          id = room.id,
          name = room.name,
          area = room.area,
          areaTo = room.areaTo,
          height = room.height,
          heightTo = room.heightTo,
          isPartOf = room.isPartOf,
          groupRead = room.groupRead,
          groupWrite = room.groupWrite,
          isDeleted = room.isDeleted,
          links = room.links,
          sikringSkallsikring = room.sikringSkallsikring,
          sikringBrannsikring = room.sikringBrannsikring,
          sikringTyverisikring = room.sikringTyverisikring,
          sikringVannskaderisiko = room.sikringVannskaderisiko,
          sikringRutineOgBeredskap = room.sikringRutineOgBeredskap,
          bevarLuftfuktOgTemp = room.bevarLuftfuktOgTemp,
          bevarLysforhold = room.bevarLysforhold,
          bevarPrevantKons = room.bevarPrevantKons
        )
    }

  def getBuilding(unit: BaseDTO, building: Building): Building = {
    Building(
      id = unit.id,
      name = unit.name,
      area = unit.area,
      areaTo = unit.areaTo,
      height = unit.height,
      heightTo = unit.heightTo,
      isPartOf = unit.isPartOf,
      groupRead = unit.groupRead,
      groupWrite = unit.groupWrite,
      isDeleted = unit.isDeleted,
      links = unit.links,
      address = building.address
    )
  }

  def getRoom(unit: BaseDTO, room: Room): Room = {
    Room(
      id = unit.id,
      name = unit.name,
      area = unit.area,
      areaTo = unit.areaTo,
      height = unit.height,
      heightTo = unit.heightTo,
      isPartOf = unit.isPartOf,
      groupRead = unit.groupRead,
      groupWrite = unit.groupWrite,
      isDeleted = unit.isDeleted,
      links = unit.links,
      sikringSkallsikring = room.sikringSkallsikring,
      sikringBrannsikring = room.sikringBrannsikring,
      sikringTyverisikring = room.sikringTyverisikring,
      sikringVannskaderisiko = room.sikringVannskaderisiko,
      sikringRutineOgBeredskap = room.sikringRutineOgBeredskap,
      bevarLuftfuktOgTemp = room.bevarLuftfuktOgTemp,
      bevarLysforhold = room.bevarLysforhold,
      bevarPrevantKons = room.bevarPrevantKons
    )
  }

  def toDTO[T <: Storage](stu: T) =
    StorageUnitDTO(
      id = stu.id,
      name = stu.name,
      area = stu.area,
      areaTo = stu.areaTo,
      isPartOf = stu.isPartOf,
      height = stu.height,
      heightTo = stu.heightTo,
      groupRead = stu.groupRead,
      groupWrite = stu.groupWrite,
      links = stu.links,
      isDeleted = stu.isDeleted,
      `type` = stu.storageType
    )

  def linkText(id: Option[Long]) =
    Some(Seq(LinkService.self(s"/v1/${id.get}")))
}