/*
 *  MUSIT is a museum database to archive natural and cultural history data.
 *  Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License,
 *  or any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package no.uio.musit.healthcheck

import com.google.inject.{Inject, Singleton}
import no.uio.musit.healthcheck.HealthCheckDao.HealthCheckName
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

import scala.concurrent.Future
import scala.util.control.NonFatal

@Singleton
class HealthCheckDao @Inject() (val dbConfigProvider: DatabaseConfigProvider)
    extends HasDatabaseConfigProvider[JdbcProfile] with HealthCheck {

  import driver.api._

  override def healthCheck(): Future[HealthCheckStatus] = {
    val checkConnectionQuery = sql"""select 1 from dual""".as[Int]
    val stopWatch = StopWatch()

    db.run(checkConnectionQuery).map {
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
    }.recover {
      case NonFatal(e) =>
        HealthCheckStatus(
          name = HealthCheckName,
          available = false,
          responseTime = stopWatch.elapsed(),
          message = Some("Unexpected result from health check query. " +
            s"Reason: ${e.getMessage}")
        )
    }
  }

}

object HealthCheckDao {
  val HealthCheckName = "database.connectivity"
}
