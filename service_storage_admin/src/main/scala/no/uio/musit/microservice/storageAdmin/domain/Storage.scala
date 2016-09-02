package no.uio.musit.microservice.storageAdmin.domain

import julienrf.json.derived
import no.uio.musit.microservice.storageAdmin.domain.dto._
import no.uio.musit.microservices.common.linking.LinkService
import no.uio.musit.microservices.common.linking.domain.Link
import play.api.libs.json.{ OFormat, __ }

sealed trait Storage {
  val id: Option[Long]
  val name: String
  val area: Option[Double]
  val areaTo: Option[Double]
  val isPartOf: Option[Long]
  val height: Option[Double]
  val heightTo: Option[Double]
  val groupRead: Option[String]
  val groupWrite: Option[String]
  val latestMoveId: Option[Long]
  val latestEnvReqId: Option[Long]
  val links: Option[Seq[Link]]
}

case class StorageUnit(
  id: Option[Long],
  name: String,
  area: Option[Double],
  areaTo: Option[Double],
  isPartOf: Option[Long],
  height: Option[Double],
  heightTo: Option[Double],
  groupRead: Option[String],
  groupWrite: Option[String],
  latestMoveId: Option[Long],
  latestEnvReqId: Option[Long],
  links: Option[Seq[Link]]
) extends Storage

case class Room(
  id: Option[Long],
  name: String,
  area: Option[Double],
  areaTo: Option[Double],
  isPartOf: Option[Long],
  height: Option[Double],
  heightTo: Option[Double],
  groupRead: Option[String],
  groupWrite: Option[String],
  latestMoveId: Option[Long],
  latestEnvReqId: Option[Long],
  links: Option[Seq[Link]],
  perimeterSecurity: Option[Boolean],
  theftProtection: Option[Boolean],
  fireProtection: Option[Boolean],
  waterDamageAssessment: Option[Boolean],
  routinesAndContingencyPlan: Option[Boolean],
  relativeHumidity: Option[Boolean],
  temperatureAssessment: Option[Boolean],
  lightingCondition: Option[Boolean],
  preventiveConservation: Option[Boolean]
) extends Storage

case class Building(
  id: Option[Long],
  name: String,
  area: Option[Double],
  areaTo: Option[Double],
  isPartOf: Option[Long],
  height: Option[Double],
  heightTo: Option[Double],
  groupRead: Option[String],
  groupWrite: Option[String],
  latestMoveId: Option[Long],
  latestEnvReqId: Option[Long],
  links: Option[Seq[Link]],
  address: Option[String]
) extends Storage

object Storage {

  implicit lazy val format: OFormat[Storage] = derived.flat.oformat((__ \ "type").format[String])

  def fromDTO[T <: StorageDTO](dto: T) =
    dto match {
      case stu: StorageNodeDTO =>
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
          latestMoveId = stu.latestMoveId,
          latestEnvReqId = stu.latestEnvReqId,
          links = stu.links
        )
      case building: BuildingDTO =>
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
          latestMoveId = building.latestMoveId,
          latestEnvReqId = building.latestEnvReqId,
          links = building.links,
          address = building.address
        )
      case room: RoomDTO =>
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
          latestMoveId = room.latestMoveId,
          latestEnvReqId = room.latestEnvReqId,
          links = room.links,
          perimeterSecurity = room.perimeterSecurity,
          fireProtection = room.fireProtection,
          theftProtection = room.theftProtection,
          waterDamageAssessment = room.waterDamageAssessment,
          routinesAndContingencyPlan = room.routinesAndContingencyPlan,
          relativeHumidity = room.relativeHumidity,
          temperatureAssessment = room.temperature,
          lightingCondition = room.lightingCondition,
          preventiveConservation = room.preventiveConservation
        )
    }

  def getBuilding(unit: StorageDTO, building: Building): Building = {
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
      latestMoveId = unit.latestMoveId,
      latestEnvReqId = unit.latestEnvReqId,
      links = unit.links,
      address = building.address
    )
  }

  def getRoom(unit: StorageDTO, room: Room): Room = {
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
      latestMoveId = unit.latestMoveId,
      latestEnvReqId = unit.latestEnvReqId,
      links = unit.links,
      perimeterSecurity = room.perimeterSecurity,
      fireProtection = room.fireProtection,
      theftProtection = room.theftProtection,
      waterDamageAssessment = room.waterDamageAssessment,
      routinesAndContingencyPlan = room.routinesAndContingencyPlan,
      relativeHumidity = room.relativeHumidity,
      temperatureAssessment = room.temperatureAssessment,
      lightingCondition = room.lightingCondition,
      preventiveConservation = room.preventiveConservation
    )
  }

  def toDTO[T <: Storage](stu: T) =
    StorageNodeDTO(
      id = stu.id,
      name = stu.name,
      area = stu.area,
      areaTo = stu.areaTo,
      isPartOf = stu.isPartOf,
      height = stu.height,
      heightTo = stu.heightTo,
      groupRead = stu.groupRead,
      groupWrite = stu.groupWrite,
      latestMoveId = stu.latestMoveId,
      latestEnvReqId = stu.latestEnvReqId,
      links = stu.links,
      isDeleted = false,
      storageType = StorageType.fromStorage(stu)
    )

  def linkText(id: Option[Long]) =
    Some(Seq(LinkService.self(s"/v1/${id.get}")))
}