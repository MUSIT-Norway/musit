package no.uio.musit.healthcheck

import com.google.inject.{Inject, Singleton}
import no.uio.musit.healthcheck.HealthCheckDao.HealthCheckName
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class HealthCheckDao @Inject()(
    implicit
    val dbConfigProvider: DatabaseConfigProvider,
    val executionContext: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile]
    with HealthCheck {

  import profile.api._

  override def healthCheck(): Future[HealthCheckStatus] = {
    val checkConnectionQuery = sql"""select 1 from dual""".as[Int]
    val stopWatch            = StopWatch()

    db.run(checkConnectionQuery)
      .map {
        case Vector(1) =>
          HealthCheckStatus(
            name = HealthCheckName,
            available = true,
            responseTime = stopWatch.elapsed(),
            message = None
          )
        case r =>
          HealthCheckStatus(
            name = HealthCheckName,
            available = false,
            responseTime = stopWatch.elapsed(),
            message = Some(s"Unexpected result from health check query. Result: $r")
          )
      }
      .recover {
        case NonFatal(e) =>
          HealthCheckStatus(
            name = HealthCheckName,
            available = false,
            responseTime = stopWatch.elapsed(),
            message = Some(
              "Unexpected result from health check query. " +
                s"Reason: ${e.getMessage}"
            )
          )
      }
  }

}

object HealthCheckDao {
  val HealthCheckName = "database.connectivity"
}
