package no.uio.musit.microservice.storageAdmin.domain

import no.uio.musit.microservices.common.linking.LinkService
import no.uio.musit.microservices.common.linking.domain.Link
import play.api.libs.json.OFormat
import julienrf.json.derived
import no.uio.musit.microservice.storageAdmin.dao.StorageUnitDTO
import play.api.libs.json.__
import shapeless.syntax.std.tuple._

sealed trait Storage extends TypeFields {
  var storageType: StorageType
}

trait TypeFields {
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
}

trait SubType extends TypeFields {
  def toStorageUnit =
    StorageUnit(
      id = this.id,
      name = this.name,
      area = this.area,
      areaTo = this.areaTo,
      isPartOf = this.isPartOf,
      height = this.height,
      heightTo = this.heightTo,
      groupRead = this.groupRead,
      groupWrite = this.groupWrite,
      links = this.links,
      isDeleted = this.isDeleted
    )
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
    isDeleted: Option[Boolean]
) extends Storage {
  var storageType: StorageType = StorageType.StorageUnit
  def setType(st: StorageType) = {
    println("Overriding existing storagetype " + storageType + " with " + st)
    storageType = st
    this
  }
}

object StorageUnit {
  def fromDTO(dto: StorageUnitDTO) =
    StorageUnit(
      dto.id,
      dto.name,
      dto.area,
      dto.areaTo,
      dto.isPartOf,
      dto.height,
      dto.heightTo,
      dto.groupRead,
      dto.groupWrite,
      dto.links,
      dto.isDeleted
    )
}

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
) extends Storage with SubType {
  var storageType: StorageType = StorageType.Room
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
) extends Storage with SubType {
  var storageType: StorageType = StorageType.Building
}

object Storage {

  implicit lazy val format: OFormat[Storage] = derived.flat.oformat((__ \ "type").format[String])

  def defaultValues(id: Option[Long]) = (
    id,
    null,
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    None
  )

  def defaultValues(unit: StorageUnitDTO) = (
    unit.id,
    unit.name,
    unit.area,
    unit.areaTo,
    unit.isPartOf,
    unit.height,
    unit.heightTo,
    unit.groupRead,
    unit.groupWrite,
    unit.links,
    unit.isDeleted
  )

  def linkText(id: Option[Long]) =
    Some(Seq(LinkService.self(s"/v1/${id.get}")))

  def getBuilding(storageUnit: StorageUnitDTO, building: Building) =
    Building.tupled(defaultValues(storageUnit) ++ Tuple1(building.address))

  def getBuilding(id: Option[Long], address: Option[String]) =
    Building.tupled(defaultValues(id) ++ Tuple1(address))

  def getRoom(storageUnit: StorageUnitDTO, room: Room) =
    Room.tupled(defaultValues(storageUnit) ++ (
      room.sikringSkallsikring,
      room.sikringTyverisikring,
      room.sikringBrannsikring,
      room.sikringVannskaderisiko,
      room.sikringRutineOgBeredskap,
      room.bevarLuftfuktOgTemp,
      room.bevarLysforhold,
      room.bevarPrevantKons
    ))

  def getRoom(id: Option[Long], sikringSkallsikring: Option[Boolean], sikringTyverisikring: Option[Boolean], sikringBrannsikring: Option[Boolean], sikringVannskaderisiko: Option[Boolean], sikringRutineOgBeredskap: Option[Boolean], bevarLuftfuktOgTemp: Option[Boolean], bevarLysforhold: Option[Boolean], bevarPrevantKons: Option[Boolean]) =
    Room.tupled(defaultValues(id) ++ (
      sikringSkallsikring,
      sikringTyverisikring,
      sikringBrannsikring,
      sikringVannskaderisiko,
      sikringRutineOgBeredskap,
      bevarLuftfuktOgTemp,
      bevarLysforhold,
      bevarPrevantKons
    ))
}