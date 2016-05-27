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

import no.uio.musit.microservices.common.domain.BaseMusitDomain
import no.uio.musit.microservices.common.linking.domain.Link
import play.api.libs.json.{ JsObject, Json }

sealed trait AbstractStorageUnit extends BaseMusitDomain {
  def storageKind: StorageUnitType = {
    val x = StUnit
    x
  }
  /* def id: Long
 def storageType :String
def storageUnitName:Option[String]
def area:Option[Long]
def isStorageUnit:Option[String]
def isPartOf:Option[Long]
def height:Option[Long]
def groupRead:Option[String]
def groupWrite:Option[String]*/
}

case class StorageUnit(id: Long, storageType: String, storageUnitName: String, area: Option[Long],
    isStorageUnit: Option[String], isPartOf: Option[Long], height: Option[Long],
    groupRead: Option[String], groupWrite: Option[String], links: Seq[Link]) extends AbstractStorageUnit {
  def toJson: JsObject = Json.toJson(this).as[JsObject]

  override def storageKind: StorageUnitType = StorageUnitType { storageType }
}

object StorageUnit {
  def tupled = (StorageUnit.apply _).tupled
  implicit val format = Json.format[StorageUnit]
}

case class StorageRoom(id: Long, sikringSkallsikring: Option[String], sikringTyverisikring: Option[String],
    sikringBrannsikring: Option[String], sikringVannskaderisiko: Option[String], sikringRutineOgBeredskap: Option[String],
    bevarLuftfuktOgTemp: Option[String], bevarLysforhold: Option[String], bevarPrevantKons: Option[String],
    links: Seq[Link]) extends AbstractStorageUnit {

  def toJson: JsObject = Json.toJson(this).as[JsObject]
  override def storageKind: StorageUnitType = Room
}

object StorageRoom {
  def tupled = (StorageRoom.apply _).tupled
  implicit val format = Json.format[StorageRoom]
}

case class StorageBuilding(id: Long, address: Option[String], links: Seq[Link]) extends AbstractStorageUnit {

  def toJson: JsObject = Json.toJson(this).as[JsObject]
  override def storageKind: StorageUnitType = Building
}

object StorageBuilding {
  def tupled = (StorageBuilding.apply _).tupled
  implicit val format = Json.format[StorageBuilding]

}

case class STORAGE_UNIT_LINK(id: Long, storage_unit_id: Long, relation: String, links: Seq[Link]) extends BaseMusitDomain

object STORAGE_UNIT_LINK {
  def tupled = (STORAGE_UNIT_LINK.apply _).tupled
  implicit val format = Json.format[STORAGE_UNIT_LINK]
}

