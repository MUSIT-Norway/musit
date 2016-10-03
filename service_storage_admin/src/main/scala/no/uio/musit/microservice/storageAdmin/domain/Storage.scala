package no.uio.musit.microservice.storageAdmin.domain

import julienrf.json.derived
import play.api.libs.json._

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
  //val latestMoveId: Option[Long]
  //  val latestEnvReqId: Option[Long]
  val environmentRequirement: Option[EnvironmentRequirement]
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
  environmentRequirement: Option[EnvironmentRequirement]
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
  environmentRequirement: Option[EnvironmentRequirement],
  securityAssessment: SecurityAssessment,
  environmentAssessment: EnvironmentAssessment

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
  environmentRequirement: Option[EnvironmentRequirement],
  address: Option[String]
) extends Storage

case class Organisation(
  id: Option[Long],
  name: String,
  area: Option[Double],
  areaTo: Option[Double],
  isPartOf: Option[Long],
  height: Option[Double],
  heightTo: Option[Double],
  groupRead: Option[String],
  groupWrite: Option[String],
  environmentRequirement: Option[EnvironmentRequirement],
  address: Option[String]
) extends Storage

case class EnvironmentRequirement(
  temperature: Option[Double],
  temperatureTolerance: Option[Double],
  hypoxicAir: Option[Double],
  hypoxicAirTolerance: Option[Double],
  relativeHumidity: Option[Double],
  relativeHumidityTolerance: Option[Double],
  cleaning: Option[String],
  lightingCondition: Option[String],
  comments: Option[String]
)

object EnvironmentRequirement {
  implicit val format = Json.format[EnvironmentRequirement]

  val empty = EnvironmentRequirement(None, None, None, None, None, None, None, None, None)
}

object Storage {

  implicit lazy val format: OFormat[Storage] = derived.flat.oformat((__ \ "type").format[String])
}

/**
 * Not a proper "subtype" of Storage, just some common fields shared by all types of storage nodes.
 * Mainly used for json-writing, for writing out info where we do not need the full nodes.
 */
case class StorageNodeCommonProperties(
  id: Option[Long],
  name: String,
  area: Option[Double],
  areaTo: Option[Double],
  isPartOf: Option[Long],
  height: Option[Double],
  heightTo: Option[Double],
  groupRead: Option[String],
  groupWrite: Option[String],
  storageType: String
)

object StorageNodeCommonProperties {
  implicit val format = Json.format[StorageNodeCommonProperties]
}
