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

import java.sql.{Timestamp => JSqlTimestamp}

import no.uio.musit.formatters.DateTimeFormatters.dateTimeFormatter
import no.uio.musit.formatters.StrictFormatters._
import no.uio.musit.formatters.WithDateTimeFormatters
import no.uio.musit.microservice.storagefacility.domain.{ActorId, NamedPathElement, NodePath}
import org.joda.time.DateTime
import play.api.data.validation.ValidationError
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.Writes._
import play.api.libs.json._

/**
 * Represents the base attributes of a StorageNode in the system.
 *
 * TODO: Should the environmentRequirement attribute be defined as required?
 * We have logic that tries to determine if a new EnvRequirement event
 * should be created or not. That is directly depending on its value.
 */
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
  val path: NodePath
  val pathNames: Option[Seq[NamedPathElement]]
  val environmentRequirement: Option[EnvironmentRequirement]
  val storageType: StorageType
  val updatedBy: Option[ActorId]
  val updatedDate: Option[DateTime]
}

object StorageNode {

  private val tpe = "type"

  implicit val reads: Reads[StorageNode] = Reads { jsv =>
    (jsv \ tpe).as[String] match {
      case StorageType.RootType.entryName =>
        Root.formats.reads(jsv)

      case StorageType.OrganisationType.entryName =>
        Organisation.formats.reads(jsv)

      case StorageType.BuildingType.entryName =>
        Building.formats.reads(jsv)

      case StorageType.RoomType.entryName =>
        Room.formats.reads(jsv)

      case StorageType.StorageUnitType.entryName =>
        StorageUnit.formats.reads(jsv)

      case err =>
        JsError(ValidationError("Invalid type for storage node", err))
    }
  }

  implicit val writes: Writes[StorageNode] = Writes {
    case root: Root =>
      Root.formats.writes(root).as[JsObject] ++
        Json.obj(tpe -> root.storageType.entryName)

    case org: Organisation =>
      Organisation.formats.writes(org).as[JsObject] ++
        Json.obj(tpe -> org.storageType.entryName)

    case bld: Building =>
      Building.formats.writes(bld).as[JsObject] ++
        Json.obj(tpe -> bld.storageType.entryName)

    case room: Room =>
      Room.formats.writes(room).as[JsObject] ++
        Json.obj(tpe -> room.storageType.entryName)

    case unit: StorageUnit =>
      StorageUnit.formats.writes(unit).as[JsObject] ++
        Json.obj(tpe -> unit.storageType.entryName)

    case other: GenericStorageNode =>
      GenericStorageNode.formats.writes(other).as[JsObject] ++
        Json.obj(tpe -> other.storageType.entryName)

  }

}

/**
 * Used to represent the common denominator for all storage nodes. Typically
 * used for services where a list of storage nodes need to be returned. A key
 * difference to the other StorageNode types is the lack of a
 * {{{Seq[NamedPathElement]}}}.
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
    path: NodePath,
    environmentRequirement: Option[EnvironmentRequirement],
    storageType: StorageType,
    updatedBy: Option[ActorId],
    updatedDate: Option[DateTime]
) extends StorageNode {
  val pathNames: Option[Seq[NamedPathElement]] = None
}

object GenericStorageNode {

  val formats: Format[GenericStorageNode] = (
    (__ \ "id").formatNullable[StorageNodeId] and
    (__ \ "name").format[String](maxCharsFormat(100)) and
    (__ \ "area").formatNullable[Double] and
    (__ \ "areaTo").formatNullable[Double] and
    (__ \ "isPartOf").formatNullable[StorageNodeId] and
    (__ \ "height").formatNullable[Double] and
    (__ \ "heightTo").formatNullable[Double] and
    (__ \ "groupRead").formatNullable[String] and
    (__ \ "groupWrite").formatNullable[String] and
    (__ \ "path").formatNullable[NodePath].inmap[NodePath](_.getOrElse(NodePath.empty), Option.apply) and
    (__ \ "environmentRequirement").formatNullable[EnvironmentRequirement] and
    (__ \ "type").format[StorageType] and
    (__ \ "updatedBy").formatNullable[ActorId] and
    (__ \ "updatedDate").formatNullable[DateTime]
  )(GenericStorageNode.apply, unlift(GenericStorageNode.unapply))

}

/**
 * A Root node is at the top of the storage node hierarchy. Each museum should
 * have _at least_ one root node.
 */
case class Root(
    id: Option[StorageNodeId] = None,
    name: String = "root-node",
    environmentRequirement: Option[EnvironmentRequirement] = None,
    path: NodePath = NodePath.empty
) extends StorageNode {
  val area: Option[Double] = None
  val areaTo: Option[Double] = None
  val isPartOf: Option[StorageNodeId] = None
  val height: Option[Double] = None
  val heightTo: Option[Double] = None
  val groupRead: Option[String] = None
  val groupWrite: Option[String] = None
  val pathNames: Option[Seq[NamedPathElement]] = None
  val storageType: StorageType = StorageType.RootType
  val updatedBy: Option[ActorId] = None
  val updatedDate: Option[DateTime] = None
}

object Root {

  val formats: Format[Root] = (
    (__ \ "id").formatNullable[StorageNodeId] and
    (__ \ "name").format[String](maxCharsFormat(100)) and
    (__ \ "environmentRequirement").formatNullable[EnvironmentRequirement] and
    (__ \ "path").formatNullable[NodePath].inmap[NodePath](_.getOrElse(NodePath.empty), Option.apply)
  )(Root.apply, unlift(Root.unapply))

}

/**
 * A StorageUnit is the smallest type of storage node. It typically represents
 * a shelf, box, etc...
 */
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
  path: NodePath,
  pathNames: Option[Seq[NamedPathElement]] = None,
  environmentRequirement: Option[EnvironmentRequirement] = None,
  updatedBy: Option[ActorId],
  updatedDate: Option[DateTime]
)
    extends StorageNode {
  val storageType: StorageType = StorageType.StorageUnitType
}

object StorageUnit extends WithDateTimeFormatters {

  val formats: Format[StorageUnit] = (
    (__ \ "id").formatNullable[StorageNodeId] and
    (__ \ "name").format[String](maxCharsFormat(100)) and
    (__ \ "area").formatNullable[Double] and
    (__ \ "areaTo").formatNullable[Double] and
    (__ \ "isPartOf").formatNullable[StorageNodeId] and
    (__ \ "height").formatNullable[Double] and
    (__ \ "heightTo").formatNullable[Double] and
    (__ \ "groupRead").formatNullable[String] and
    (__ \ "groupWrite").formatNullable[String] and
    (__ \ "path").formatNullable[NodePath].inmap[NodePath](_.getOrElse(NodePath.empty), Option.apply) and
    (__ \ "pathNames").formatNullable[Seq[NamedPathElement]] and
    (__ \ "environmentRequirement").formatNullable[EnvironmentRequirement] and
    (__ \ "updatedBy").formatNullable[ActorId] and
    (__ \ "updatedDate").formatNullable[DateTime]
  )(StorageUnit.apply, unlift(StorageUnit.unapply))

}

/**
 * Represents a Room.
 */
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
    path: NodePath,
    pathNames: Option[Seq[NamedPathElement]] = None,
    environmentRequirement: Option[EnvironmentRequirement] = None,
    securityAssessment: SecurityAssessment,
    environmentAssessment: EnvironmentAssessment,
    updatedBy: Option[ActorId],
    updatedDate: Option[DateTime]
) extends StorageNode {
  val storageType: StorageType = StorageType.RoomType

}

object Room {

  val formats: Format[Room] = (
    (__ \ "id").formatNullable[StorageNodeId] and
    (__ \ "name").format[String](maxCharsFormat(100)) and
    (__ \ "area").formatNullable[Double] and
    (__ \ "areaTo").formatNullable[Double] and
    (__ \ "isPartOf").formatNullable[StorageNodeId] and
    (__ \ "height").formatNullable[Double] and
    (__ \ "heightTo").formatNullable[Double] and
    (__ \ "groupRead").formatNullable[String] and
    (__ \ "groupWrite").formatNullable[String] and
    (__ \ "path").formatNullable[NodePath].inmap[NodePath](_.getOrElse(NodePath.empty), Option.apply) and
    (__ \ "pathNames").formatNullable[Seq[NamedPathElement]] and
    (__ \ "environmentRequirement").formatNullable[EnvironmentRequirement] and
    (__ \ "securityAssessment").format[SecurityAssessment] and
    (__ \ "environmentAssessment").format[EnvironmentAssessment] and
    (__ \ "updatedBy").formatNullable[ActorId] and
    (__ \ "updatedDate").formatNullable[DateTime]
  )(Room.apply, unlift(Room.unapply))

}

/**
 * Represents a Building.
 */
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
    path: NodePath,
    pathNames: Option[Seq[NamedPathElement]] = None,
    environmentRequirement: Option[EnvironmentRequirement] = None,
    address: Option[String],
    updatedBy: Option[ActorId],
    updatedDate: Option[DateTime]
) extends StorageNode {
  val storageType: StorageType = StorageType.BuildingType
}

object Building {

  val formats: Format[Building] = (
    (__ \ "id").formatNullable[StorageNodeId] and
    (__ \ "name").format[String](maxCharsFormat(100)) and
    (__ \ "area").formatNullable[Double] and
    (__ \ "areaTo").formatNullable[Double] and
    (__ \ "isPartOf").formatNullable[StorageNodeId] and
    (__ \ "height").formatNullable[Double] and
    (__ \ "heightTo").formatNullable[Double] and
    (__ \ "groupRead").formatNullable[String] and
    (__ \ "groupWrite").formatNullable[String] and
    (__ \ "path").formatNullable[NodePath].inmap[NodePath](_.getOrElse(NodePath.empty), Option.apply) and
    (__ \ "pathNames").formatNullable[Seq[NamedPathElement]] and
    (__ \ "environmentRequirement").formatNullable[EnvironmentRequirement] and
    (__ \ "address").formatNullable[String](Format(maxLength[String](100), StringWrites)) and
    (__ \ "updatedBy").formatNullable[ActorId] and
    (__ \ "updatedDate").formatNullable[DateTime]
  )(Building.apply, unlift(Building.unapply))

}

/**
 * Represents an Organisation. Both internal and external.
 */
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
    path: NodePath,
    pathNames: Option[Seq[NamedPathElement]] = None,
    environmentRequirement: Option[EnvironmentRequirement] = None,
    address: Option[String],
    updatedBy: Option[ActorId],
    updatedDate: Option[DateTime]
) extends StorageNode {
  val storageType: StorageType = StorageType.OrganisationType
}

object Organisation {

  val formats: Format[Organisation] = (
    (__ \ "id").formatNullable[StorageNodeId] and
    (__ \ "name").format[String](maxCharsFormat(100)) and
    (__ \ "area").formatNullable[Double] and
    (__ \ "areaTo").formatNullable[Double] and
    (__ \ "isPartOf").formatNullable[StorageNodeId] and
    (__ \ "height").formatNullable[Double] and
    (__ \ "heightTo").formatNullable[Double] and
    (__ \ "groupRead").formatNullable[String] and
    (__ \ "groupWrite").formatNullable[String] and
    (__ \ "path").formatNullable[NodePath].inmap[NodePath](_.getOrElse(NodePath.empty), Option.apply) and
    (__ \ "pathNames").formatNullable[Seq[NamedPathElement]] and
    (__ \ "environmentRequirement").formatNullable[EnvironmentRequirement] and
    (__ \ "address").formatNullable[String](Format(maxLength[String](100), StringWrites)) and
    (__ \ "updatedBy").formatNullable[ActorId] and
    (__ \ "updatedDate").formatNullable[DateTime]
  )(Organisation.apply, unlift(Organisation.unapply))

}