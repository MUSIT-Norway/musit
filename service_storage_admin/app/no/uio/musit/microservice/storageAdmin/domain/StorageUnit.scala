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

package no.uio.musit.microservice.storageAdmin.domain

import no.uio.musit.microservice.storageAdmin.domain.LocalTypes.StorageBuildingOrRoom
import no.uio.musit.microservices.common.domain.BaseMusitDomain
import no.uio.musit.microservices.common.linking.domain.Link
import play.api.libs.json.{JsObject, JsValue, Json}

object LocalTypes {
  type StorageBuildingOrRoom = Either[StorageBuilding, StorageRoom]
}

sealed trait AbstractStorageUnit {
  def storageKind: StorageUnitType = {
    val x = StUnit
    x
  }

  def getId: Long
}

case class StorageUnit(
    id: Option[Long],
    storageType: String,
    storageUnitName: String,
    area: Option[Long],
    isStorageUnit: Option[String],
    isPartOf: Option[Long],
    height: Option[Long],
    groupRead: Option[String],
    groupWrite: Option[String],
    links: Option[Seq[Link]]
) extends AbstractStorageUnit {
  def toJson: JsObject = Json.toJson(this).as[JsObject]

  def getId: Long = id.get

  override def storageKind: StorageUnitType = StorageUnitType {
    storageType
  }
}

object StorageUnit {
  def tupled = (StorageUnit.apply _).tupled

  implicit val format = Json.format[StorageUnit]
}

case class StorageRoom(
    id: Option[Long] = None,
    sikringSkallsikring: Option[String] = None,
    sikringTyverisikring: Option[String] = None,
    sikringBrannsikring: Option[String] = None,
    sikringVannskaderisiko: Option[String] = None,
    sikringRutineOgBeredskap: Option[String] = None,
    bevarLuftfuktOgTemp: Option[String] = None,
    bevarLysforhold: Option[String] = None,
    bevarPrevantKons: Option[String] = None,
    links: Option[Seq[Link]] = None
) extends AbstractStorageUnit {

  def toJson: JsObject = Json.toJson(this).as[JsObject]

  def getId: Long = id.get

  override def storageKind: StorageUnitType = Room
}

object StorageRoom {
  def tupled = (StorageRoom.apply _).tupled

  implicit val format = Json.format[StorageRoom]
}

case class StorageBuilding(
    id: Option[Long],
    address: Option[String],
    links: Option[Seq[Link]]
) extends AbstractStorageUnit {

  def toJson: JsObject = Json.toJson(this).as[JsObject]

  def getId: Long = id.get

  override def storageKind: StorageUnitType = Building
}

object StorageBuilding {
  def tupled = (StorageBuilding.apply _).tupled

  implicit val format = Json.format[StorageBuilding]

}


case class StorageUnitTriple(storageUnit: StorageUnit, buildingOrRoom: Option[StorageBuildingOrRoom]) {
  def storageKind: StorageUnitType = storageUnit.storageKind

  def getBuildingOrRoom = {
    assert(storageKind==Building || storageKind==Room)
    assert(buildingOrRoom.isDefined)

    buildingOrRoom.get
  }
  def getBuilding = {
    assert(storageKind==Building)
    val buildingOrRoom = getBuildingOrRoom
    assert(buildingOrRoom.isLeft)
    buildingOrRoom.left.get
  }

  def getRoom = {
    assert(storageKind==Room)
    val buildingOrRoom = getBuildingOrRoom
    assert(buildingOrRoom.isRight)
    buildingOrRoom.right.get
  }

  def toJson = {
    storageKind match {
      case StUnit => {storageUnit.toJson}
      case Building => {storageUnit.toJson.++ (getRoom.toJson)}
      case Room =>{storageUnit.toJson.++ (getBuilding.toJson)}
    }
  }

/*
  def createJson(triple: (StorageUnit, Option[StorageRoom], Option[StorageBuilding])) = {
    val storageUnitJs = triple._1.toJson
    val storageUnitJs2 = triple._2.fold(storageUnitJs)(storageRoom => storageUnitJs.++(storageRoom.toJson))
    val res = triple._3.fold(storageUnitJs2)(storageBuilding => storageUnitJs2.++(storageBuilding.toJson))
    res.as[JsValue]
  }*/
}

object StorageUnitTriple {
  def fromStorageUnit(storageUnit: StorageUnit) = {
    assert(storageUnit.storageKind==StUnit)
    StorageUnitTriple(storageUnit, None)
  }

  def fromBuilding(storageUnit: StorageUnit, building: StorageBuilding) = {
    assert(storageUnit.storageKind==Building)
    StorageUnitTriple(storageUnit, Some(Left(building)))
  }

  def fromRoom(storageUnit: StorageUnit, room: StorageRoom) = {
    assert(storageUnit.storageKind==Room)
    StorageUnitTriple(storageUnit, Some(Right(room)))
  }
}

case class STORAGE_UNIT_LINK(id: Long, storage_unit_id: Long, relation: String, links: Seq[Link]) extends BaseMusitDomain

object STORAGE_UNIT_LINK {
  def tupled = (STORAGE_UNIT_LINK.apply _).tupled

  implicit val format = Json.format[STORAGE_UNIT_LINK]
}

