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

package models.storage.dto

import java.sql.{Timestamp => JSqlTimestamp}

import models.datetime.Implicits._
import models.storage._
import no.uio.musit.models._

sealed trait StorageNodeDto

sealed trait SpecializedStorageNode

case class StorageUnitDto(
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
  oldBarcode: Option[Int],
  path: NodePath,
  isDeleted: Option[Boolean],
  storageType: StorageType,
  museumId: MuseumId,
  updatedBy: Option[ActorId],
  updatedDate: Option[JSqlTimestamp]
) extends StorageNodeDto

case class ExtendedStorageNode[T <: SpecializedStorageNode](
  storageUnitDto: StorageUnitDto,
  extension: T
) extends StorageNodeDto

case class RootDto(
    id: Option[StorageNodeDatabaseId],
    nodeId: Option[StorageNodeId],
    name: String,
    storageType: StorageType,
    museumId: MuseumId,
    path: NodePath = NodePath.empty,
    updatedBy: Option[ActorId],
    updatedDate: Option[JSqlTimestamp]
) extends StorageNodeDto {

  /**
   * Hack to convert into a StorageUnitDto for DB inserts
   */
  def asStorageUnitDto(mid: MuseumId): StorageUnitDto =
    StorageUnitDto(
      id = id,
      nodeId = nodeId,
      name = name,
      area = None,
      areaTo = None,
      isPartOf = None,
      height = None,
      heightTo = None,
      groupRead = None,
      groupWrite = None,
      oldBarcode = None,
      path = path,
      isDeleted = None,
      storageType = storageType,
      museumId = mid,
      updatedBy = updatedBy,
      updatedDate = updatedDate
    )

}

case class BuildingDto(
  id: Option[StorageNodeDatabaseId],
  address: Option[String]
) extends SpecializedStorageNode

case class OrganisationDto(
  id: Option[StorageNodeDatabaseId],
  address: Option[String]
) extends SpecializedStorageNode

case class RoomDto(
  id: Option[StorageNodeDatabaseId],
  perimeterSecurity: Option[Boolean],
  theftProtection: Option[Boolean],
  fireProtection: Option[Boolean],
  waterDamageAssessment: Option[Boolean],
  routinesAndContingencyPlan: Option[Boolean],
  relativeHumidity: Option[Boolean],
  temperatureAssessment: Option[Boolean],
  lightingCondition: Option[Boolean],
  preventiveConservation: Option[Boolean]
) extends SpecializedStorageNode

object StorageNodeDto {

  def toStorageNode[T <: StorageNodeDto](dto: T) =
    dto match {
      case root: RootDto =>
        toRootNode(root)

      case stu: StorageUnitDto =>
        toStorageUnit(stu)

      case ext: ExtendedStorageNode[_] =>
        // Need to match on extension type due to type erasure in the above
        // case pattern. There are ways to remedy this. But isn't worth it for
        // this simple use-case
        ext.extension match {
          case buildExt: BuildingDto =>
            toBuilding(
              ExtendedStorageNode[BuildingDto](ext.storageUnitDto, buildExt)
            )

          case roomExt: RoomDto =>
            toRoom(
              ExtendedStorageNode[RoomDto](ext.storageUnitDto, roomExt)
            )

          case orgExt: OrganisationDto =>
            toOrganisation(
              ExtendedStorageNode[OrganisationDto](ext.storageUnitDto, orgExt)
            )
        }
    }

  def fromStorageNode[T <: StorageNode](mid: MuseumId, stu: T): StorageNodeDto =
    stu match {
      case root: Root => fromRootNode(mid, root)
      case su: StorageUnit => fromStorageUnit(mid, su)
      case b: Building => fromBuilding(mid, b)
      case r: Room => fromRoom(mid, r)
    }

  def toGenericStorageNode(su: StorageUnitDto): GenericStorageNode =
    GenericStorageNode(
      id = su.id,
      nodeId = su.nodeId,
      name = su.name,
      area = su.area,
      areaTo = su.areaTo,
      height = su.height,
      heightTo = su.heightTo,
      isPartOf = su.isPartOf,
      groupRead = su.groupRead,
      groupWrite = su.groupWrite,
      path = su.path,
      environmentRequirement = None, // EnvRequirement is handled elsewhere
      storageType = su.storageType,
      updatedBy = su.updatedBy,
      updatedDate = su.updatedDate
    )

  @throws(classOf[IllegalArgumentException]) // scalastyle:ignore
  def toRootNode(su: StorageUnitDto): RootNode = {
    su.storageType match {
      case StorageType.RootType =>
        Root(
          id = su.id,
          nodeId = su.nodeId,
          name = su.name,
          path = su.path,
          updatedBy = su.updatedBy,
          updatedDate = su.updatedDate
        )

      case StorageType.RootLoanType =>
        RootLoan(
          id = su.id,
          nodeId = su.nodeId,
          name = su.name,
          path = su.path,
          updatedBy = su.updatedBy,
          updatedDate = su.updatedDate
        )

      case tpe =>
        throw new IllegalArgumentException(s"Cannot instantiate $tpe as RootNode") // scalastyle:ignore
    }
  }

  def toRootNode(r: RootDto): RootNode = toRootNode(r.asStorageUnitDto(r.museumId))

  def toStorageUnit(su: StorageUnitDto): StorageUnit =
    StorageUnit(
      id = su.id,
      nodeId = su.nodeId,
      name = su.name,
      area = su.area,
      areaTo = su.areaTo,
      height = su.height,
      heightTo = su.heightTo,
      isPartOf = su.isPartOf,
      groupRead = su.groupRead,
      groupWrite = su.groupWrite,
      path = su.path,
      environmentRequirement = None, // EnvRequirement is handled elsewhere
      updatedBy = su.updatedBy,
      updatedDate = su.updatedDate
    )

  def toBuilding(ext: ExtendedStorageNode[BuildingDto]): Building = {
    Building(
      id = ext.extension.id,
      nodeId = ext.storageUnitDto.nodeId,
      name = ext.storageUnitDto.name,
      area = ext.storageUnitDto.area,
      areaTo = ext.storageUnitDto.areaTo,
      height = ext.storageUnitDto.height,
      heightTo = ext.storageUnitDto.heightTo,
      isPartOf = ext.storageUnitDto.isPartOf,
      groupRead = ext.storageUnitDto.groupRead,
      groupWrite = ext.storageUnitDto.groupWrite,
      path = ext.storageUnitDto.path,
      environmentRequirement = None, // EnvRequirement is handled elsewhere
      address = ext.extension.address,
      updatedBy = ext.storageUnitDto.updatedBy,
      updatedDate = ext.storageUnitDto.updatedDate
    )
  }

  def toOrganisation(ext: ExtendedStorageNode[OrganisationDto]): Organisation = {
    Organisation(
      id = ext.extension.id,
      nodeId = ext.storageUnitDto.nodeId,
      name = ext.storageUnitDto.name,
      area = ext.storageUnitDto.area,
      areaTo = ext.storageUnitDto.areaTo,
      height = ext.storageUnitDto.height,
      heightTo = ext.storageUnitDto.heightTo,
      isPartOf = ext.storageUnitDto.isPartOf,
      groupRead = ext.storageUnitDto.groupRead,
      groupWrite = ext.storageUnitDto.groupWrite,
      path = ext.storageUnitDto.path,
      environmentRequirement = None, // EnvRequirement is handled elsewhere
      address = ext.extension.address,
      updatedBy = ext.storageUnitDto.updatedBy,
      updatedDate = ext.storageUnitDto.updatedDate
    )
  }

  def toRoom(ext: ExtendedStorageNode[RoomDto]): Room = {
    Room(
      id = ext.extension.id,
      nodeId = ext.storageUnitDto.nodeId,
      name = ext.storageUnitDto.name,
      area = ext.storageUnitDto.area,
      areaTo = ext.storageUnitDto.areaTo,
      height = ext.storageUnitDto.height,
      heightTo = ext.storageUnitDto.heightTo,
      isPartOf = ext.storageUnitDto.isPartOf,
      groupRead = ext.storageUnitDto.groupRead,
      groupWrite = ext.storageUnitDto.groupWrite,
      path = ext.storageUnitDto.path,
      environmentRequirement = None, // EnvRequirement is handled elsewhere
      securityAssessment = SecurityAssessment(
        perimeter = ext.extension.perimeterSecurity,
        theftProtection = ext.extension.theftProtection,
        fireProtection = ext.extension.fireProtection,
        waterDamage = ext.extension.waterDamageAssessment,
        routinesAndContingencyPlan = ext.extension.routinesAndContingencyPlan
      ),
      environmentAssessment = EnvironmentAssessment(
        relativeHumidity = ext.extension.relativeHumidity,
        temperature = ext.extension.temperatureAssessment,
        lightingCondition = ext.extension.lightingCondition,
        preventiveConservation = ext.extension.preventiveConservation
      ),
      updatedBy = ext.storageUnitDto.updatedBy,
      updatedDate = ext.storageUnitDto.updatedDate
    )
  }

  def fromRootNode(mid: MuseumId, r: RootNode): RootDto =
    RootDto(
      id = r.id,
      nodeId = r.nodeId,
      name = r.name,
      storageType = r.storageType,
      museumId = mid,
      updatedBy = r.updatedBy,
      updatedDate = r.updatedDate
    )

  def fromStorageUnit(
    mid: MuseumId,
    su: StorageUnit,
    id: Option[StorageNodeDatabaseId] = None
  ): StorageUnitDto =
    StorageUnitDto(
      id = id.orElse(su.id),
      nodeId = su.nodeId,
      name = su.name,
      area = su.area,
      areaTo = su.areaTo,
      isPartOf = su.isPartOf,
      height = su.height,
      heightTo = su.heightTo,
      groupRead = su.groupRead,
      groupWrite = su.groupWrite,
      oldBarcode = None,
      path = su.path,
      isDeleted = Some(false),
      storageType = su.storageType,
      museumId = mid,
      updatedBy = su.updatedBy,
      updatedDate = su.updatedDate
    )

  def fromBuilding(
    mid: MuseumId,
    b: Building,
    id: Option[StorageNodeDatabaseId] = None
  ): ExtendedStorageNode[BuildingDto] =
    ExtendedStorageNode(
      storageUnitDto = StorageUnitDto(
        id = id.orElse(b.id),
        nodeId = b.nodeId,
        name = b.name,
        area = b.area,
        areaTo = b.areaTo,
        isPartOf = b.isPartOf,
        height = b.height,
        heightTo = b.heightTo,
        groupRead = b.groupRead,
        groupWrite = b.groupWrite,
        oldBarcode = None,
        path = b.path,
        isDeleted = Some(false),
        storageType = b.storageType,
        museumId = mid,
        updatedBy = b.updatedBy,
        updatedDate = b.updatedDate
      ),
      extension = BuildingDto(
        id = id.orElse(b.id),
        address = b.address
      )
    )

  def fromOrganisation(
    mid: MuseumId,
    o: Organisation,
    id: Option[StorageNodeDatabaseId] = None
  ): ExtendedStorageNode[OrganisationDto] =
    ExtendedStorageNode(
      storageUnitDto = StorageUnitDto(
        id = id.orElse(o.id),
        nodeId = o.nodeId,
        name = o.name,
        area = o.area,
        areaTo = o.areaTo,
        isPartOf = o.isPartOf,
        height = o.height,
        heightTo = o.heightTo,
        groupRead = o.groupRead,
        groupWrite = o.groupWrite,
        oldBarcode = None,
        path = o.path,
        isDeleted = Some(false),
        storageType = o.storageType,
        museumId = mid,
        updatedBy = o.updatedBy,
        updatedDate = o.updatedDate
      ),
      extension = OrganisationDto(
        id = id.orElse(o.id),
        address = o.address
      )
    )

  def fromRoom(
    mid: MuseumId,
    r: Room,
    id: Option[StorageNodeDatabaseId] = None
  ): ExtendedStorageNode[RoomDto] =
    ExtendedStorageNode(
      storageUnitDto = StorageUnitDto(
        id = id.orElse(r.id),
        nodeId = r.nodeId,
        name = r.name,
        area = r.area,
        areaTo = r.areaTo,
        isPartOf = r.isPartOf,
        height = r.height,
        heightTo = r.heightTo,
        groupRead = r.groupRead,
        groupWrite = r.groupWrite,
        oldBarcode = None,
        path = r.path,
        isDeleted = Some(false),
        storageType = r.storageType,
        museumId = mid,
        updatedBy = r.updatedBy,
        updatedDate = r.updatedDate
      ),
      extension = RoomDto(
        id = id.orElse(r.id),
        perimeterSecurity = r.securityAssessment.perimeter,
        theftProtection = r.securityAssessment.theftProtection,
        fireProtection = r.securityAssessment.fireProtection,
        waterDamageAssessment = r.securityAssessment.waterDamage,
        routinesAndContingencyPlan = r.securityAssessment.routinesAndContingencyPlan,
        relativeHumidity = r.environmentAssessment.relativeHumidity,
        temperatureAssessment = r.environmentAssessment.temperature,
        lightingCondition = r.environmentAssessment.lightingCondition,
        preventiveConservation = r.environmentAssessment.preventiveConservation
      )
    )
}