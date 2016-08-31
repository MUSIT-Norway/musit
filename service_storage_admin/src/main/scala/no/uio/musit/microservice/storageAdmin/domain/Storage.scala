package no.uio.musit.microservice.storageAdmin.domain

import julienrf.json.derived
import no.uio.musit.microservice.storageAdmin.domain.dto._
import no.uio.musit.microservices.common.linking.LinkService
import no.uio.musit.microservices.common.linking.domain.Link
import play.api.libs.json.{ OFormat, __ }

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
  val latestMoveId: Option[Long]
  val links: Option[Seq[Link]]
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
  latestMoveId: Option[Long],
  links: Option[Seq[Link]]
) extends Storage

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
  latestMoveId: Option[Long],
  links: Option[Seq[Link]],
  sikringSkallsikring: Option[Boolean],
  sikringTyverisikring: Option[Boolean],
  sikringBrannsikring: Option[Boolean],
  sikringVannskaderisiko: Option[Boolean],
  sikringRutineOgBeredskap: Option[Boolean],
  bevarLuftfuktOgTemp: Option[Boolean],
  bevarLysforhold: Option[Boolean],
  bevarPrevantKons: Option[Boolean]
) extends Storage

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
  latestMoveId: Option[Long],
  links: Option[Seq[Link]],
  address: Option[String]
) extends Storage

object Storage {

  implicit lazy val format: OFormat[Storage] = derived.flat.oformat((__ \ "type").format[String])

  def fromDTO[T <: StorageDTO](dto: T) =
    dto match {
      case stu: StorageNodeDTO =>
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
          latestMoveId = stu.latestMoveId,
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
          latestMoveId = building.latestMoveId,
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
          latestMoveId = room.latestMoveId,
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

  def getBuilding(unit: StorageDTO, building: Building): Building = {
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
      latestMoveId = unit.latestMoveId,
      links = unit.links,
      address = building.address
    )
  }

  def getRoom(unit: StorageDTO, room: Room): Room = {
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
      latestMoveId = unit.latestMoveId,
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
    StorageNodeDTO(
      id = stu.id,
      name = stu.name,
      area = stu.area,
      areaTo = stu.areaTo,
      isPartOf = stu.isPartOf,
      height = stu.height,
      heightTo = stu.heightTo,
      groupRead = stu.groupRead,
      groupWrite = stu.groupWrite,
      latestMoveId = stu.latestMoveId,
      links = stu.links,
      isDeleted = false,
      storageType = StorageType.fromStorage(stu)
    )

  def linkText(id: Option[Long]) =
    Some(Seq(LinkService.self(s"/v1/${id.get}")))
}