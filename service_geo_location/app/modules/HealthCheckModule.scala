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

package modules

import akka.actor.ActorSystem
import com.google.inject.{AbstractModule, Inject, Provider}
import controllers.routes
import net.codingwell.scalaguice.{ScalaModule, ScalaMultibinder}
import no.uio.musit.healthcheck.{HealthCheck, HealthCheckDao, ZabbixExecutor}
import no.uio.musit.service.BuildInfo
import play.api.{Configuration, Environment}

class HealthCheckModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    val healthChecks = ScalaMultibinder.newSetBinder[HealthCheck](binder)
    healthChecks.addBinding.to[HealthCheckDao]

    bind[ZabbixExecutor].toProvider(classOf[ZabbixExecutorProvider]).asEagerSingleton()
  }

}

class ZabbixExecutorProvider @Inject() (
    environment: Environment,
    configuration: Configuration,
    healthChecks: Set[HealthCheck],
    actorSystem: ActorSystem
) extends Provider[ZabbixExecutor] {

  override def get() = ZabbixExecutor(
    BuildInfo.name,
    s"api/barcode/${routes.HealthCheckController.healthCheck().url}",
    healthChecks,
    actorSystem,
    environment.mode,
    configuration
  )

}
