package no.uio.musit.healthcheck

import com.google.inject.{Inject, Singleton}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

@Singleton
class HealthCheckService @Inject()(val healthChecks: Set[HealthCheck]) {

  def executeHealthChecks(): Future[Set[HealthCheckStatus]] =
    Future.sequence(healthChecks.map(_.healthCheck()))

}
