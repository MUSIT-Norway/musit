package no.uio.musit.microservice.storagefacility.domain.storage.dto

import no.uio.musit.microservice.storagefacility.domain.storage.{ StorageNodeId, StorageType }
import no.uio.musit.microservices.common.linking.domain.Link

sealed trait BaseDto {
  val id: Option[StorageNodeId]
  val name: String
  val area: Option[Long]
  val areaTo: Option[Long]
  val isPartOf: Option[StorageNodeId]
  val height: Option[Long]
  val heightTo: Option[Long]
  val groupRead: Option[String]
  val groupWrite: Option[String]
  val links: Option[Seq[Link]]
  val isDeleted: Option[Boolean]
  val `type`: StorageType
}

case class BuildingDto(
  id: Option[StorageNodeId],
  name: String,
  area: Option[Long],
  areaTo: Option[Long],
  isPartOf: Option[StorageNodeId],
  height: Option[Long],
  heightTo: Option[Long],
  groupRead: Option[String],
  groupWrite: Option[String],
  links: Option[Seq[Link]],
  isDeleted: Option[Boolean],
  `type`: StorageType,
  address: Option[String]
) extends BaseDto

case class RoomDto(
  id: Option[StorageNodeId],
  name: String,
  area: Option[Long],
  areaTo: Option[Long],
  isPartOf: Option[StorageNodeId],
  height: Option[Long],
  heightTo: Option[Long],
  groupRead: Option[String],
  groupWrite: Option[String],
  links: Option[Seq[Link]],
  isDeleted: Option[Boolean],
  `type`: StorageType,
  sikringSkallsikring: Option[Boolean],
  sikringTyverisikring: Option[Boolean],
  sikringBrannsikring: Option[Boolean],
  sikringVannskaderisiko: Option[Boolean],
  sikringRutineOgBeredskap: Option[Boolean],
  bevarLuftfuktOgTemp: Option[Boolean],
  bevarLysforhold: Option[Boolean],
  bevarPrevantKons: Option[Boolean]
) extends BaseDto

case class StorageUnitDto(
  id: Option[StorageNodeId],
  name: String,
  area: Option[Long],
  areaTo: Option[Long],
  isPartOf: Option[StorageNodeId],
  height: Option[Long],
  heightTo: Option[Long],
  groupRead: Option[String],
  groupWrite: Option[String],
  links: Option[Seq[Link]],
  isDeleted: Option[Boolean],
  `type`: StorageType
) extends BaseDto