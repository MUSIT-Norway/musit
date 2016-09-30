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
import no.uio.musit.microservice.storagefacility.domain.NodePath
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

sealed trait StorageNode {
  val id: Option[StorageNodeId]
  val name: String
  val area: Option[Double]
  val areaTo: Option[Double]
  val isPartOf: Option[StorageNodeId]
  val height: Option[Double]
  val heightTo: Option[Double]
  val groupRead: Option[String]
  val groupWrite: Option[String]
  val path: Option[NodePath] // TODO: I think path should be required. But that doesn't make sense on creation :-/
  // TODO: Need to provide a readable path, might possibly be part of NodePath?
  // val readablePath: Option[String]
  /*
    TODO: Should this attribute be defined as required? We have logic that tries
    to determine if a new EnvRequirement event should be created or not. That is
    directly depending on what value this has.
   */
  val environmentRequirement: Option[EnvironmentRequirement]
  val storageType: StorageType
}

case class Root(
    id: Option[StorageNodeId] = None,
    name: String = "root-node",
    environmentRequirement: Option[EnvironmentRequirement] = None,
    path: Option[NodePath] = Some(NodePath.empty)
) extends StorageNode {
  val area: Option[Double] = None
  val areaTo: Option[Double] = None
  val isPartOf: Option[StorageNodeId] = None
  val height: Option[Double] = None
  val heightTo: Option[Double] = None
  val groupRead: Option[String] = None
  val groupWrite: Option[String] = None
  val storageType: StorageType = StorageType.RootType
}

case class StorageUnit(
    id: Option[StorageNodeId],
    name: String,
    area: Option[Double],
    areaTo: Option[Double],
    isPartOf: Option[StorageNodeId],
    height: Option[Double],
    heightTo: Option[Double],
    groupRead: Option[String],
    groupWrite: Option[String],
    path: Option[NodePath],
    environmentRequirement: Option[EnvironmentRequirement]
) extends StorageNode {
  val storageType: StorageType = StorageType.StorageUnitType
}

case class Room(
    id: Option[StorageNodeId],
    name: String,
    area: Option[Double],
    areaTo: Option[Double],
    isPartOf: Option[StorageNodeId],
    height: Option[Double],
    heightTo: Option[Double],
    groupRead: Option[String],
    groupWrite: Option[String],
    path: Option[NodePath],
    environmentRequirement: Option[EnvironmentRequirement],
    securityAssessment: SecurityAssessment,
    environmentAssessment: EnvironmentAssessment
) extends StorageNode {
  val storageType: StorageType = StorageType.RoomType
}

case class Building(
    id: Option[StorageNodeId],
    name: String,
    area: Option[Double],
    areaTo: Option[Double],
    isPartOf: Option[StorageNodeId],
    height: Option[Double],
    heightTo: Option[Double],
    groupRead: Option[String],
    groupWrite: Option[String],
    path: Option[NodePath],
    environmentRequirement: Option[EnvironmentRequirement],
    address: Option[String]
) extends StorageNode {
  val storageType: StorageType = StorageType.BuildingType
}

case class Organisation(
    id: Option[StorageNodeId],
    name: String,
    area: Option[Double],
    areaTo: Option[Double],
    isPartOf: Option[StorageNodeId],
    height: Option[Double],
    heightTo: Option[Double],
    groupRead: Option[String],
    groupWrite: Option[String],
    path: Option[NodePath],
    environmentRequirement: Option[EnvironmentRequirement],
    address: Option[String]
) extends StorageNode {
  val storageType: StorageType = StorageType.OrganisationType
}

object StorageNode {

  /*
    ============================================================================
    ¡¡¡¡¡IMPORTANT!!!!!:
    ============================================================================
    There's a significant caveat with using the derived JSON codec. And it is
    that refactoring the name of _any_ of the types in the ADT will change the
    API of any services that use them. In other words; it is a non-backwards
    compatible change. Because the _value_ of the "type" attribute needs to be
    constant for each type.

    FIXME: The derived codecs JSON formatters have horrible error messages.
    We should probably change the formatters to use plain reads/writes with
    manual disambiguation as shown here:
    http://scalytica.net/#posts/2015-03-29/playframework-reads-writes-of-parent-child-class-structure
    ============================================================================
  */

  private[this] val constrainedNameRead: Reads[String] =
    (__ \ "name").read[String](maxLength[String](500))

  private[this] val constrainedAddressRead: Reads[Option[String]] =
    (__ \ "address").readNullable(maxLength[String](500))

  implicit lazy val reads: Reads[StorageNode] = {
    constrainedNameRead andKeep constrainedAddressRead andKeep
      derived.flat.reads[StorageNode]((__ \ "type").read[String])
  }

  implicit lazy val writes: OWrites[StorageNode] =
    derived.flat.owrites[StorageNode]((__ \ "type").write[String])
}

/**
 * Used to represent the common denominator for all storage nodes.
 */
case class GenericStorageNode(
  id: Option[StorageNodeId],
  name: String,
  area: Option[Double],
  areaTo: Option[Double],
  isPartOf: Option[StorageNodeId],
  height: Option[Double],
  heightTo: Option[Double],
  groupRead: Option[String],
  groupWrite: Option[String],
  path: Option[NodePath],
  environmentRequirement: Option[EnvironmentRequirement],
  storageType: StorageType
) extends StorageNode

object GenericStorageNode {
  implicit val format: Format[GenericStorageNode] = (
    (__ \ "id").formatNullable[StorageNodeId] and
    (__ \ "name").format[String] and
    (__ \ "area").formatNullable[Double] and
    (__ \ "areaTo").formatNullable[Double] and
    (__ \ "isPartOf").formatNullable[StorageNodeId] and
    (__ \ "height").formatNullable[Double] and
    (__ \ "heightTo").formatNullable[Double] and
    (__ \ "groupRead").formatNullable[String] and
    (__ \ "groupWrite").formatNullable[String] and
    (__ \ "path").formatNullable[NodePath] and
    (__ \ "environmentRequirement").formatNullable[EnvironmentRequirement] and
    (__ \ "type").format[StorageType]
  )(GenericStorageNode.apply, unlift(GenericStorageNode.unapply))
}