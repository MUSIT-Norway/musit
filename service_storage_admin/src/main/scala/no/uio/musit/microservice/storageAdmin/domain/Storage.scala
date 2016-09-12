package no.uio.musit.microservice.storageAdmin.domain

import julienrf.json.derived
import no.uio.musit.microservice.storageAdmin.domain.dto._
import no.uio.musit.microservices.common.linking.LinkService
import no.uio.musit.microservices.common.linking.domain.Link
import play.api.libs.json.{ Json, OFormat, __ }

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
  val links: Option[Seq[Link]]
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
  //latestMoveId: Option[Long],
  //  latestEnvReqId: Option[Long],
  links: Option[Seq[Link]],
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
  //  latestMoveId: Option[Long],
  //  latestEnvReqId: Option[Long],
  links: Option[Seq[Link]],
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
  //  latestMoveId: Option[Long],4
  //  latestEnvReqId: Option[Long],
  links: Option[Seq[Link]],
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

  def linkText(id: Option[Long]) =
    Some(Seq(LinkService.self(s"/v1/${id.get}")))
}