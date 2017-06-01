package no.uio.musit.healthcheck

import play.api.libs.json.{Format, Json}

/**
 * A health check status indicating the status with an optional message.
 *
 * @param name The health check
 * @param available Status of the check
 * @param responseTime The time used in millis
 * @param message Optional message
 */
case class HealthCheckStatus(
    name: String,
    available: Boolean,
    responseTime: Long,
    message: Option[String]
)

object HealthCheckStatus {
  implicit val format: Format[HealthCheckStatus] = Json.format[HealthCheckStatus]
}
