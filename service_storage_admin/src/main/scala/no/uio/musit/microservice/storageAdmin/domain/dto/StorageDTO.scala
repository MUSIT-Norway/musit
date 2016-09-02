package no.uio.musit.microservice.storageAdmin.domain.dto

import no.uio.musit.microservices.common.linking.domain.Link

sealed trait StorageDTO {
  val id: Option[Long]
  val name: String
  val area: Option[Long]
  val areaTo: Option[Long]
  val isPartOf: Option[Long]
  val height: Option[Long]
  val heightTo: Option[Long]
  val groupRead: Option[String]
  val groupWrite: Option[String]
  val latestMoveId: Option[Long]
  val latestEnvReqId: Option[Long]
  val links: Option[Seq[Link]]
  val isDeleted: Boolean
  val storageType: StorageType
}

case class BuildingDTO(
  id: Option[Long],
  name: String,
  area: Option[Long],
  areaTo: Option[Long],
  isPartOf: Option[Long],
  height: Option[Long],
  heightTo: Option[Long],
  groupRead: Option[String],
  groupWrite: Option[String],
  latestMoveId: Option[Long],
  latestEnvReqId: Option[Long],
  links: Option[Seq[Link]],
  isDeleted: Boolean,
  storageType: StorageType,
  address: Option[String]
) extends StorageDTO

case class RoomDTO(
                    id: Option[Long],
                    name: String,
                    area: Option[Long],
                    areaTo: Option[Long],
                    isPartOf: Option[Long],
                    height: Option[Long],
                    heightTo: Option[Long],
                    groupRead: Option[String],
                    groupWrite: Option[String],
                    latestMoveId: Option[Long],
                    latestEnvReqId: Option[Long],
                    links: Option[Seq[Link]],
                    isDeleted: Boolean,
                    storageType: StorageType,
                    perimeterSecurity: Option[Boolean],
                    theftProtection: Option[Boolean],
                    fireProtection: Option[Boolean],
                    waterDamageAssessment: Option[Boolean],
                    routinesAndContingencyPlan: Option[Boolean],
                    relativeHumidity: Option[Boolean],
                    temperatureAssessment: Option[Boolean],
                    lightingCondition: Option[Boolean],
                    preventiveConservation: Option[Boolean]
) extends StorageDTO

case class StorageNodeDTO(
  id: Option[Long],
  name: String,
  area: Option[Long],
  areaTo: Option[Long],
  isPartOf: Option[Long],
  height: Option[Long],
  heightTo: Option[Long],
  groupRead: Option[String],
  groupWrite: Option[String],
  latestMoveId: Option[Long],
  latestEnvReqId: Option[Long],
  links: Option[Seq[Link]],
  isDeleted: Boolean,
  storageType: StorageType
) extends StorageDTO