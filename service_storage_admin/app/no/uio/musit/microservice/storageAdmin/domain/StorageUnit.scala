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
import play.api.libs.json._
import play.api.libs.functional.syntax._

object LocalTypes {
  type StorageBuildingOrRoom = Either[StorageBuilding, StorageRoom]
}

sealed trait AbstractStorageUnit {
  def storageKind: StorageUnitType = {
    val x = StUnit
    x
  }

  //def getId: Long
}

case class StorageUnit(
    id: Option[Long],
    storageType: String,
    storageUnitName: String,
    area: Option[Long],
    isPartOf: Option[Long],
    height: Option[Long],
    groupRead: Option[String],
    groupWrite: Option[String],
    links: Option[Seq[Link]]
) extends AbstractStorageUnit {
  def toJson: JsObject = Json.toJson(this).as[JsObject]

  //def getId: Long = id.get //we use it in tests, here we KNOW that it's a values and not Some(value))

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
    sikringSkallsikring: Option[Boolean] = None,
    sikringTyverisikring: Option[Boolean] = None,
    sikringBrannsikring: Option[Boolean] = None,
    sikringVannskaderisiko: Option[Boolean] = None,
    sikringRutineOgBeredskap: Option[Boolean] = None,
    bevarLuftfuktOgTemp: Option[Boolean] = None,
    bevarLysforhold: Option[Boolean] = None,
    bevarPrevantKons: Option[Boolean] = None,
    links: Option[Seq[Link]] = None
) extends AbstractStorageUnit {

  def toJson: JsObject = Json.toJson(this).as[JsObject]

  //def getId: Long = id.get //we use it in tests, here we KNOW that it's a values and not Some(value))

  override def storageKind: StorageUnitType = Room
}

object StorageRoom {
  def tupled = (StorageRoom.apply _).tupled

  implicit val format = Json.format[StorageRoom]

  /*implicit val writes = Json.writes[StorageRoom]

  implicit val reads: Reads[StorageRoom] = (
    (JsPath \ "id").readNullable[Long] and
    (JsPath \ "sikringSkallsikring").readNullable[Boolean] and
    (JsPath \ "sikringTyverisikring").readNullable[Boolean] and
    (JsPath \ "sikringBrannsikring").readNullable[Boolean] and
    (JsPath \ "sikringVannskaderisiko").readNullable[Boolean] and
    (JsPath \ "sikringRutineOgBeredskap").readNullable[Boolean] and
    (JsPath \ "bevarLuftfuktOgTemp").readNullable[Boolean] and
    (JsPath \ "bevarLysforhold").readNullable[Boolean] and
    (JsPath \ "bevarPrevantKons").readNullable[Boolean] and
    (JsPath \ "links").readNullable[Seq[Link]]
  )(StorageRoom.apply _)*/
}

case class StorageBuilding(
    id: Option[Long],
    address: Option[String],
    links: Option[Seq[Link]]
) extends AbstractStorageUnit {

  def toJson: JsObject = Json.toJson(this).as[JsObject]

  //def getId: Long = id.get //here we KNOW that it's a values and not Some(value))

  override def storageKind: StorageUnitType = Building
}

object StorageBuilding {
  def tupled = (StorageBuilding.apply _).tupled

  implicit val format = Json.format[StorageBuilding]

}

///Represents a StorageUnit or a StorageBuilding (together with its corresponding StorageUnit) or a StorageRoom (together with its corresponding StorageUnit)
case class StorageUnitTriple(storageUnit: StorageUnit, buildingOrRoom: Option[StorageBuildingOrRoom]) {
  def storageKind: StorageUnitType = storageUnit.storageKind

  def getBuildingOrRoom = {
    assert(storageKind == Building || storageKind == Room)
    assert(buildingOrRoom.isDefined)

    buildingOrRoom.get
  }
  def getBuilding = {
    assert(storageKind == Building)
    val buildingOrRoom = getBuildingOrRoom
    assert(buildingOrRoom.isLeft)
    buildingOrRoom.left.get
  }

  def getRoom = {
    assert(storageKind == Room)
    val buildingOrRoom = getBuildingOrRoom
    assert(buildingOrRoom.isRight)
    buildingOrRoom.right.get
  }

  def toJson = {
    storageKind match {
      case StUnit => { storageUnit.toJson }
      case Building => { storageUnit.toJson.++(getBuilding.toJson) }
      case Room => { storageUnit.toJson.++(getRoom.toJson) }
    }

  }

  //When id is explicitly defined in the url in the request, we want this id to override possible id in the body, so we have this convenience method to do this transformation
  def copyWithId(id: Long) = {
    val newStorageUnit = storageUnit.copy(id = Some(id))
    storageKind match {
      case StUnit => StorageUnitTriple.createStorageUnit(newStorageUnit)
      case Building => StorageUnitTriple.createBuilding(newStorageUnit, getBuilding.copy(id = Some(id)))
      case Room => StorageUnitTriple.createRoom(newStorageUnit, getRoom.copy(id = Some(id)))
    }
  }
}

object StorageUnitTriple {
  def createStorageUnit(storageUnit: StorageUnit) = {
    assert(storageUnit.storageKind == StUnit)
    StorageUnitTriple(storageUnit, None)
  }

  def createBuilding(storageUnit: StorageUnit, building: StorageBuilding) = {
    assert(storageUnit.storageKind == Building)
    StorageUnitTriple(storageUnit, Some(Left(building)))
  }

  def createRoom(storageUnit: StorageUnit, room: StorageRoom) = {
    assert(storageUnit.storageKind == Room)
    StorageUnitTriple(storageUnit, Some(Right(room)))
  }
}

case class STORAGE_UNIT_LINK(id: Long, storage_unit_id: Long, relation: String, links: Seq[Link]) extends BaseMusitDomain

object STORAGE_UNIT_LINK {
  def tupled = (STORAGE_UNIT_LINK.apply _).tupled

  implicit val format = Json.format[STORAGE_UNIT_LINK]
}

