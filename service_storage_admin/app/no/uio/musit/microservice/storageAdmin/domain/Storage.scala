package no.uio.musit.microservice.storageAdmin.domain

import no.uio.musit.microservices.common.linking.LinkService
import no.uio.musit.microservices.common.linking.domain.Link
import no.uio.musit.microservice.storageAdmin.utils.TupleImplicits._
import play.api.libs.json.OFormat
import julienrf.json.derived
import play.api.libs.json.__

sealed trait Storage {
  def storageType: StorageType
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
) extends Storage with TypeFields {
  val storageType: StorageType = StorageType.StorageUnit
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
) extends Storage with SubType {
  val storageType: StorageType = StorageType.Building
}

object Storage {

  implicit lazy val format: OFormat[Storage] = derived.flat.oformat((__ \ "type").format[String])

  def linkText(id: Option[Long]) =
    Some(Seq(LinkService.self(s"/v1/${id.get}")))

  def getBuilding(storageUnit: StorageUnit, building: Building) =
    Building.tupled(StorageUnit.unapply(storageUnit).get
      -> building.address)

  def getBuilding(id: Option[Long], address: Option[String]) =
    Building(id, null, None, None, None, None, None, None, None, None, None,
      address)

  def getRoom(storageUnit: StorageUnit, room: Room) =
    Room.tupled(StorageUnit.unapply(storageUnit).get
      -> (
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
    Room(id, null, None, None, None, None, None, None, None, None, None,
      sikringSkallsikring,
      sikringTyverisikring,
      sikringBrannsikring,
      sikringVannskaderisiko,
      sikringRutineOgBeredskap,
      bevarLuftfuktOgTemp,
      bevarLysforhold,
      bevarPrevantKons)
}