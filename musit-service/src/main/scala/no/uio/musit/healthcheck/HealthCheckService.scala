package no.uio.musit.healthcheck

import com.google.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HealthCheckService @Inject()(
    implicit
    val healthChecks: Set[HealthCheck],
    val ec: ExecutionContext
) {

  def executeHealthChecks(): Future[Set[HealthCheckStatus]] =
    Future.sequence(healthChecks.map(_.healthCheck()))

}
