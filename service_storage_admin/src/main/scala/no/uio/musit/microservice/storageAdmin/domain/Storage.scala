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
  val latestMoveId: Option[Long]
  val latestEnvReqId: Option[Long]
  val links: Option[Seq[Link]]

  //TODO  val environmentRequirement: Option[EnvironmentRequirement] //TODO: Optional or required?
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
  securityAssessment: SecurityAssessment, //TODO: Optional or required?
  environmentAssessment: EnvironmentAssessment //TODO: Optional or required?
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

case class EnvironmentRequirement(
  temperature: Option[Double],
  temperatureTolerance: Option[Double],
  hypoxicAir: Option[Double],
  hypoxicTolerance: Option[Double],
  relativeHumidity: Option[Double],
  relativeHumidityTolerance: Option[Double],
  cleaning: Option[String],
  comments: Option[String]
)

object EnvironmentRequirement {
  implicit val format = Json.format[EnvironmentRequirement]
}

object Storage {

  implicit lazy val format: OFormat[Storage] = derived.flat.oformat((__ \ "type").format[String])

  def linkText(id: Option[Long]) =
    Some(Seq(LinkService.self(s"/v1/${id.get}")))
}