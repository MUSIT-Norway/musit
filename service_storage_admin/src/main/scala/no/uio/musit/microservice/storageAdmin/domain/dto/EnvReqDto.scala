package no.uio.musit.microservice.storageAdmin.domain.dto

/**
 * Created by ellenjo on 08.09.16.
 */
case class EnvReqDto(
  id: Option[Long],
  temperature: Option[Double],
  temperatureTolerance: Option[Double],
  hypoxicAir: Option[Double],
  hypoxicAirTolerance: Option[Double],
  relativeHumidity: Option[Double],
  relativeHumidityTolerance: Option[Double],
  lightingCond: Option[String],
  cleaning: Option[String],
  note: Option[String]
)
