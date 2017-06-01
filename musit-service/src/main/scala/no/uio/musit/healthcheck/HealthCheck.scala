package no.uio.musit.healthcheck

import scala.concurrent.Future

trait HealthCheck {

  def healthCheck(): Future[HealthCheckStatus]

}
