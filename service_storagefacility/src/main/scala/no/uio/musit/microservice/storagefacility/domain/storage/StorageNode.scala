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
//import no.uio.musit.microservices.common.linking.LinkService
//import no.uio.musit.microservices.common.linking.domain.Link
import play.api.libs.json.{ OFormat, __ }

sealed trait StorageNode {
  val id: Option[StorageNodeId]
  val name: String
  val area: Option[Long]
  val areaTo: Option[Long]
  val isPartOf: Option[StorageNodeId]
  val height: Option[Long]
  val heightTo: Option[Long]
  val groupRead: Option[String]
  val groupWrite: Option[String]
  //  val links: Option[Seq[Link]]
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
    groupWrite: Option[String]
//    links: Option[Seq[Link]]
) extends StorageNode {
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
    //    links: Option[Seq[Link]],
    sikringSkallsikring: Option[Boolean],
    sikringTyverisikring: Option[Boolean],
    sikringBrannsikring: Option[Boolean],
    sikringVannskaderisiko: Option[Boolean],
    sikringRutineOgBeredskap: Option[Boolean],
    bevarLuftfuktOgTemp: Option[Boolean],
    bevarLysforhold: Option[Boolean],
    bevarPrevantKons: Option[Boolean]
) extends StorageNode {
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
    //    links: Option[Seq[Link]],
    address: Option[String]
) extends StorageNode {
  val storageType: StorageType = StorageType.Building
}

object StorageNode {

  implicit lazy val format: OFormat[StorageNode] = derived.flat.oformat((__ \ "type").format[String])
  //  def linkText(id: Option[Long]) =
  //    Some(Seq(LinkService.self(s"/v1/${id.get}")))
}