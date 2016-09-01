/*
 * MUSIT is a museum database to archive natural and cultural history data.
 * Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package no.uio.musit.microservice.storagefacility.domain.storage

import julienrf.json.derived
import no.uio.musit.microservice.storagefacility.domain.storage.dto._
import no.uio.musit.microservices.common.linking.LinkService
import no.uio.musit.microservices.common.linking.domain.Link
import play.api.libs.json.{ OFormat, __ }

sealed trait Storage {
  val id: Option[StorageNodeId]
  val name: String
  val area: Option[Long]
  val areaTo: Option[Long]
  val isPartOf: Option[StorageNodeId]
  val height: Option[Long]
  val heightTo: Option[Long]
  val groupRead: Option[String]
  val groupWrite: Option[String]
  val links: Option[Seq[Link]]
  val storageType: StorageType
}

case class StorageUnit(
    id: Option[StorageNodeId],
    name: String,
    area: Option[Long],
    areaTo: Option[Long],
    isPartOf: Option[StorageNodeId],
    height: Option[Long],
    heightTo: Option[Long],
    groupRead: Option[String],
    groupWrite: Option[String],
    links: Option[Seq[Link]]
) extends Storage {
  val storageType = StorageType.StorageUnit
}

object StorageUnit

case class Room(
    id: Option[StorageNodeId],
    name: String,
    area: Option[Long],
    areaTo: Option[Long],
    isPartOf: Option[StorageNodeId],
    height: Option[Long],
    heightTo: Option[Long],
    groupRead: Option[String],
    groupWrite: Option[String],
    links: Option[Seq[Link]],
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
    id: Option[StorageNodeId],
    name: String,
    area: Option[Long],
    areaTo: Option[Long],
    isPartOf: Option[StorageNodeId],
    height: Option[Long],
    heightTo: Option[Long],
    groupRead: Option[String],
    groupWrite: Option[String],
    links: Option[Seq[Link]],
    address: Option[String]
) extends Storage {
  val storageType: StorageType = StorageType.Building
}

object Storage {

  implicit lazy val format: OFormat[Storage] = derived.flat.oformat((__ \ "type").format[String])

  def fromDTO[T <: BaseDto](dto: T) =
    dto match {
      case stu: StorageUnitDto =>
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
          links = stu.links
        )
      case building: BuildingDto =>
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
          links = building.links,
          address = building.address
        )
      case room: RoomDto =>
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

  def getBuilding(unit: BaseDto, building: Building): Building = {
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
      links = unit.links,
      address = building.address
    )
  }

  def getRoom(unit: BaseDto, room: Room): Room = {
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
    StorageUnitDto(
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
      isDeleted = Some(false), // hack, we check isDeleted in slick before update, so ..
      `type` = stu.storageType
    )

  def linkText(id: Option[Long]) =
    Some(Seq(LinkService.self(s"/v1/${id.get}")))
}