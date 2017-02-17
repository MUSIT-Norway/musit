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

package models.storage

import no.uio.musit.formatters.DateTimeFormatters.dateTimeFormatter
import no.uio.musit.formatters.StrictFormatters._
import no.uio.musit.models._
import org.joda.time.DateTime
import play.api.Logger
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
  val id: Option[StorageNodeDatabaseId]
  val nodeId: Option[StorageNodeId]
  val name: String
  val area: Option[Double]
  val areaTo: Option[Double]
  val isPartOf: Option[StorageNodeDatabaseId]
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

  def logExists(destId: StorageNodeDatabaseId, exists: Boolean): String = {
    s"Destination $destId ${if (exists) "exists" else "doesn't exist"} in " +
      "expected position"
  }

  private val tpe = "type"

  implicit val reads: Reads[StorageNode] = Reads { jsv =>
    (jsv \ tpe).as[String] match {
      case StorageType.RootType.entryName =>
        Root.formats.reads(jsv)

      case StorageType.RootLoanType.entryName =>
        RootLoan.formats.reads(jsv)

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

    case rootLoan: RootLoan =>
      RootLoan.formats.writes(rootLoan).as[JsObject] ++
        Json.obj(tpe -> rootLoan.storageType.entryName)

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
 * used for services where a list of storage nodes need to be returned.
 */
case class GenericStorageNode(
    id: Option[StorageNodeDatabaseId],
    nodeId: Option[StorageNodeId],
    name: String,
    area: Option[Double],
    areaTo: Option[Double],
    isPartOf: Option[StorageNodeDatabaseId],
    height: Option[Double],
    heightTo: Option[Double],
    groupRead: Option[String],
    groupWrite: Option[String],
    path: NodePath,
    environmentRequirement: Option[EnvironmentRequirement],
    storageType: StorageType,
    updatedBy: Option[ActorId],
    updatedDate: Option[DateTime],
    pathNames: Option[Seq[NamedPathElement]] = None
) extends StorageNode

object GenericStorageNode {

  implicit val formats: Format[GenericStorageNode] = (
    (__ \ "id").formatNullable[StorageNodeDatabaseId] and
    (__ \ "nodeId").formatNullable[StorageNodeId] and
    (__ \ "name").format[String](maxCharsFormat(100)) and
    (__ \ "area").formatNullable[Double] and
    (__ \ "areaTo").formatNullable[Double] and
    (__ \ "isPartOf").formatNullable[StorageNodeDatabaseId] and
    (__ \ "height").formatNullable[Double] and
    (__ \ "heightTo").formatNullable[Double] and
    (__ \ "groupRead").formatNullable[String] and
    (__ \ "groupWrite").formatNullable[String] and
    (__ \ "path").formatNullable[NodePath].inmap[NodePath](_.getOrElse(NodePath.empty), Option.apply) and // scalastyle:ignore
    (__ \ "environmentRequirement").formatNullable[EnvironmentRequirement] and
    (__ \ "type").format[StorageType] and
    (__ \ "updatedBy").formatNullable[ActorId] and
    (__ \ "updatedDate").formatNullable[DateTime] and
    (__ \ "pathNames").formatNullable[Seq[NamedPathElement]]
  )(GenericStorageNode.apply, unlift(GenericStorageNode.unapply))

}

/**
 * A Root node is at the top of the storage node hierarchy. Each museum should
 * have _at least_ one root node.
 */
sealed trait RootNode extends StorageNode {
  val area: Option[Double] = None
  val areaTo: Option[Double] = None
  val isPartOf: Option[StorageNodeDatabaseId] = None
  val height: Option[Double] = None
  val heightTo: Option[Double] = None
  val groupRead: Option[String] = None
  val groupWrite: Option[String] = None
  val pathNames: Option[Seq[NamedPathElement]] = None

  def setUpdated(
    by: Option[ActorId] = None,
    date: Option[DateTime] = None
  ): RootNode
}

object RootNode {
  private val tpe = "type"

  implicit val reads: Reads[RootNode] = Reads { jsv =>
    (jsv \ tpe).as[String] match {
      case StorageType.RootType.entryName =>
        Root.formats.reads(jsv)

      case StorageType.RootLoanType.entryName =>
        RootLoan.formats.reads(jsv)

      case err =>
        JsError(ValidationError("Invalid type for root node", err))
    }
  }

  implicit val writes: Writes[RootNode] = Writes {
    case root: Root =>
      Root.formats.writes(root).as[JsObject] ++
        Json.obj(tpe -> root.storageType.entryName)

    case rootLoan: RootLoan =>
      RootLoan.formats.writes(rootLoan).as[JsObject] ++
        Json.obj(tpe -> rootLoan.storageType.entryName)
  }

  /**
   * A Root node can only be placed at the very top of the hierarchy.
   */
  def isValidLocation(destPath: NodePath): Boolean = destPath == NodePath.empty
}

/**
 * Root always represents the beginning of the hierarchy for nodes inside the
 * museum storage facility.
 */
case class Root(
    id: Option[StorageNodeDatabaseId] = None,
    nodeId: Option[StorageNodeId],
    name: String = "root-node",
    environmentRequirement: Option[EnvironmentRequirement] = None,
    path: NodePath = NodePath.empty,
    updatedBy: Option[ActorId] = None,
    updatedDate: Option[DateTime] = None
) extends RootNode {
  val storageType: StorageType = StorageType.RootType

  def setUpdated(
    by: Option[ActorId] = None,
    date: Option[DateTime] = None
  ): RootNode = this.copy(updatedBy = by, updatedDate = date)
}

object Root {

  val logger = Logger(classOf[Root])

  implicit val formats: Format[Root] = (
    (__ \ "id").formatNullable[StorageNodeDatabaseId] and
    (__ \ "nodeId").formatNullable[StorageNodeId] and
    (__ \ "name").format[String](maxCharsFormat(100)) and
    (__ \ "environmentRequirement").formatNullable[EnvironmentRequirement] and
    (__ \ "path").formatNullable[NodePath].inmap[NodePath](_.getOrElse(NodePath.empty), Option.apply) and // scalastyle:ignore
    (__ \ "updatedBy").formatNullable[ActorId] and
    (__ \ "updatedDate").formatNullable[DateTime]
  )(Root.apply, unlift(Root.unapply))

}

/**
 * RootLoan nodes represent the beginning of the hierarchy for nodes that are
 * outside the museum. Typically used to keep track of objects that have been
 * lent to other organisations and actors.
 */
case class RootLoan(
    id: Option[StorageNodeDatabaseId] = None,
    nodeId: Option[StorageNodeId],
    name: String = "root-loan-node",
    environmentRequirement: Option[EnvironmentRequirement] = None,
    path: NodePath = NodePath.empty,
    updatedBy: Option[ActorId] = None,
    updatedDate: Option[DateTime] = None
) extends RootNode {
  val storageType: StorageType = StorageType.RootLoanType

  def setUpdated(
    by: Option[ActorId] = None,
    date: Option[DateTime] = None
  ): RootNode = this.copy(updatedBy = by, updatedDate = date)
}

object RootLoan {

  val logger = Logger(classOf[RootLoan])

  implicit val formats: Format[RootLoan] = (
    (__ \ "id").formatNullable[StorageNodeDatabaseId] and
    (__ \ "nodeId").formatNullable[StorageNodeId] and
    (__ \ "name").format[String](maxCharsFormat(100)) and
    (__ \ "environmentRequirement").formatNullable[EnvironmentRequirement] and
    (__ \ "path").formatNullable[NodePath].inmap[NodePath](_.getOrElse(NodePath.empty), Option.apply) and // scalastyle:ignore
    (__ \ "updatedBy").formatNullable[ActorId] and
    (__ \ "updatedDate").formatNullable[DateTime]
  )(RootLoan.apply, unlift(RootLoan.unapply))

}

/**
 * A StorageUnit is the smallest type of storage node. It typically represents
 * a shelf, box, etc...
 */
case class StorageUnit(
    id: Option[StorageNodeDatabaseId],
    nodeId: Option[StorageNodeId],
    name: String,
    area: Option[Double],
    areaTo: Option[Double],
    isPartOf: Option[StorageNodeDatabaseId],
    height: Option[Double],
    heightTo: Option[Double],
    groupRead: Option[String],
    groupWrite: Option[String],
    path: NodePath,
    pathNames: Option[Seq[NamedPathElement]] = None,
    environmentRequirement: Option[EnvironmentRequirement] = None,
    updatedBy: Option[ActorId],
    updatedDate: Option[DateTime]
) extends StorageNode {

  val storageType: StorageType = StorageType.StorageUnitType

}

object StorageUnit {

  val logger = Logger(classOf[StorageUnit])

  implicit val formats: Format[StorageUnit] = (
    (__ \ "id").formatNullable[StorageNodeDatabaseId] and
    (__ \ "nodeId").formatNullable[StorageNodeId] and
    (__ \ "name").format[String](maxCharsFormat(100)) and
    (__ \ "area").formatNullable[Double] and
    (__ \ "areaTo").formatNullable[Double] and
    (__ \ "isPartOf").formatNullable[StorageNodeDatabaseId] and
    (__ \ "height").formatNullable[Double] and
    (__ \ "heightTo").formatNullable[Double] and
    (__ \ "groupRead").formatNullable[String] and
    (__ \ "groupWrite").formatNullable[String] and
    (__ \ "path").formatNullable[NodePath].inmap[NodePath](_.getOrElse(NodePath.empty), Option.apply) and // scalastyle:ignore
    (__ \ "pathNames").formatNullable[Seq[NamedPathElement]] and
    (__ \ "environmentRequirement").formatNullable[EnvironmentRequirement] and
    (__ \ "updatedBy").formatNullable[ActorId] and
    (__ \ "updatedDate").formatNullable[DateTime]
  )(StorageUnit.apply, unlift(StorageUnit.unapply))

  /**
   * A StorageUnit node can only be placed _after_ the 3 required top-nodes.
   */
  def isValidLocation(
    maybeDestId: Option[StorageNodeDatabaseId],
    pathTypes: Seq[(StorageNodeDatabaseId, StorageType)]
  ): Boolean = {
    maybeDestId.exists { destId =>
      pathTypes.toList match {
        case Nil => false
        case root :: Nil => false
        case root :: org :: Nil => false
        case root :: org :: tail =>
          val exists = tail.exists(_._1 == destId)
          StorageNode.logExists(destId, exists)
          exists
      }
    }
  }

}

/**
 * Represents a Room.
 */
case class Room(
    id: Option[StorageNodeDatabaseId],
    nodeId: Option[StorageNodeId],
    name: String,
    area: Option[Double],
    areaTo: Option[Double],
    isPartOf: Option[StorageNodeDatabaseId],
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

  val logger = Logger(classOf[Room])

  implicit val formats: Format[Room] = (
    (__ \ "id").formatNullable[StorageNodeDatabaseId] and
    (__ \ "nodeId").formatNullable[StorageNodeId] and
    (__ \ "name").format[String](maxCharsFormat(100)) and
    (__ \ "area").formatNullable[Double] and
    (__ \ "areaTo").formatNullable[Double] and
    (__ \ "isPartOf").formatNullable[StorageNodeDatabaseId] and
    (__ \ "height").formatNullable[Double] and
    (__ \ "heightTo").formatNullable[Double] and
    (__ \ "groupRead").formatNullable[String] and
    (__ \ "groupWrite").formatNullable[String] and
    (__ \ "path").formatNullable[NodePath].inmap[NodePath](_.getOrElse(NodePath.empty), Option.apply) and // scalastyle:ignore
    (__ \ "pathNames").formatNullable[Seq[NamedPathElement]] and
    (__ \ "environmentRequirement").formatNullable[EnvironmentRequirement] and
    (__ \ "securityAssessment").format[SecurityAssessment] and
    (__ \ "environmentAssessment").format[EnvironmentAssessment] and
    (__ \ "updatedBy").formatNullable[ActorId] and
    (__ \ "updatedDate").formatNullable[DateTime]
  )(Room.apply, unlift(Room.unapply))

  /**
   * A Room node can only be placed _after_ the 3 required top-nodes.
   */
  def isValidLocation(
    maybeDestId: Option[StorageNodeDatabaseId],
    pathTypes: Seq[(StorageNodeDatabaseId, StorageType)]
  ): Boolean = {
    maybeDestId.exists { destId =>
      pathTypes.toList match {
        case Nil => false
        case root :: Nil => false
        case root :: org :: Nil => false
        case root :: org :: tail =>
          val exists = tail.exists(_._1 == destId)
          StorageNode.logExists(destId, exists)
          exists
      }
    }
  }

}

/**
 * Represents a Building.
 */
case class Building(
    id: Option[StorageNodeDatabaseId],
    nodeId: Option[StorageNodeId],
    name: String,
    area: Option[Double],
    areaTo: Option[Double],
    isPartOf: Option[StorageNodeDatabaseId],
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

  val logger = Logger(classOf[Building])

  implicit val formats: Format[Building] = (
    (__ \ "id").formatNullable[StorageNodeDatabaseId] and
    (__ \ "nodeId").formatNullable[StorageNodeId] and
    (__ \ "name").format[String](maxCharsFormat(100)) and
    (__ \ "area").formatNullable[Double] and
    (__ \ "areaTo").formatNullable[Double] and
    (__ \ "isPartOf").formatNullable[StorageNodeDatabaseId] and
    (__ \ "height").formatNullable[Double] and
    (__ \ "heightTo").formatNullable[Double] and
    (__ \ "groupRead").formatNullable[String] and
    (__ \ "groupWrite").formatNullable[String] and
    (__ \ "path").formatNullable[NodePath].inmap[NodePath](_.getOrElse(NodePath.empty), Option.apply) and // scalastyle:ignore
    (__ \ "pathNames").formatNullable[Seq[NamedPathElement]] and
    (__ \ "environmentRequirement").formatNullable[EnvironmentRequirement] and
    (__ \ "address").formatNullable[String](Format(maxLength[String](100), StringWrites)) and // scalastyle:ignore
    (__ \ "updatedBy").formatNullable[ActorId] and
    (__ \ "updatedDate").formatNullable[DateTime]
  )(Building.apply, unlift(Building.unapply))

  /**
   * A Building node can only be placed _after_ the top organisation node or
   * after the first required building node.
   */
  def isValidLocation(
    maybeDestId: Option[StorageNodeDatabaseId],
    pathTypes: Seq[(StorageNodeDatabaseId, StorageType)]
  ): Boolean = {
    maybeDestId.exists { destId =>
      pathTypes.toList match {
        case Nil => false
        case root :: Nil => false
        case root :: tail =>
          val exists = tail.exists(_._1 == destId)
          StorageNode.logExists(destId, exists)
          exists
      }
    }
  }
}

/**
 * Represents an Organisation. Both internal and external.
 */
case class Organisation(
    id: Option[StorageNodeDatabaseId],
    nodeId: Option[StorageNodeId],
    name: String,
    area: Option[Double],
    areaTo: Option[Double],
    isPartOf: Option[StorageNodeDatabaseId],
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

  val logger = Logger(classOf[Organisation])

  implicit val formats: Format[Organisation] = (
    (__ \ "id").formatNullable[StorageNodeDatabaseId] and
    (__ \ "nodeId").formatNullable[StorageNodeId] and
    (__ \ "name").format[String](maxCharsFormat(100)) and
    (__ \ "area").formatNullable[Double] and
    (__ \ "areaTo").formatNullable[Double] and
    (__ \ "isPartOf").formatNullable[StorageNodeDatabaseId] and
    (__ \ "height").formatNullable[Double] and
    (__ \ "heightTo").formatNullable[Double] and
    (__ \ "groupRead").formatNullable[String] and
    (__ \ "groupWrite").formatNullable[String] and
    (__ \ "path").formatNullable[NodePath].inmap[NodePath](_.getOrElse(NodePath.empty), Option.apply) and // scalastyle:ignore
    (__ \ "pathNames").formatNullable[Seq[NamedPathElement]] and
    (__ \ "environmentRequirement").formatNullable[EnvironmentRequirement] and
    (__ \ "address").formatNullable[String](Format(maxLength[String](100), StringWrites)) and // scalastyle:ignore
    (__ \ "updatedBy").formatNullable[ActorId] and
    (__ \ "updatedDate").formatNullable[DateTime]
  )(Organisation.apply, unlift(Organisation.unapply))

  /**
   * An StorageUnit node can only be placed _after_ the 3 required top-nodes.
   */
  def isValidLocation(
    maybeDestId: Option[StorageNodeDatabaseId],
    pathTypes: Seq[(StorageNodeDatabaseId, StorageType)]
  ): Boolean = {
    maybeDestId.exists { destId =>
      pathTypes.toList match {
        case Nil => false
        case root :: Nil => destId == root._1
        case root :: org :: Nil => false
        case root :: org :: tail =>
          val exists = tail.exists(_._1 == destId)
          StorageNode.logExists(destId, exists)
          exists
      }
    }
  }

}