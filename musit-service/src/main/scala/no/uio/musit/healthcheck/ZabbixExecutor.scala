/*
 *  MUSIT is a museum database to archive natural and cultural history data.
 *  Copyright (C) 2017  MUSIT Norway, part of www.uio.no (University of Oslo)
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

import java.io.{Closeable, File, FileWriter}

import akka.actor.ActorSystem
import no.uio.musit.healthcheck.ZabbixExecutor.using
import org.joda.time.DateTime
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class ZabbixExecutor(
    zabbixMeta: ZabbixMeta,
    healthChecks: Set[HealthCheck],
    zabbaxFilePath: String,
    actorSystem: ActorSystem
) {

  val scheduler = actorSystem.scheduler.schedule(
    initialDelay = 10 seconds,
    interval = 4 minutes
  ) { executeHealthChecks() }

  def close(): Unit = {
    scheduler.cancel()
  }

  def executeHealthChecks(): Future[Unit] = {
    Future.sequence(healthChecks.map(_.healthCheck()))
      .map(hc => Zabbix(
        meta = zabbixMeta,
        updated = DateTime.now(),
        healthChecks = hc
      ))
      .map(z => writeToFile(z))
  }

  private def writeToFile(z: Zabbix): Unit = {
    val f = new File(zabbaxFilePath + "/musit-health.json")
    f.createNewFile()
    using(new FileWriter(f, false)) {
      w => w.write(Json.prettyPrint(z.toJson))
    }
  }

}

object ZabbixExecutor {

  def using[A <: Closeable, B](resource: A)(f: A => B): B =
    try f(resource) finally resource.close()

}
