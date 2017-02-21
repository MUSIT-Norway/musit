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

import java.io.File
import java.nio.file.Files

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, IOResult}
import akka.stream.scaladsl.{FileIO, Flow, Keep, Source}
import akka.util.ByteString
import org.joda.time.DateTime
import play.api.Mode.Mode
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.{Configuration, Logger, Mode}

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class ZabbixExecutor(
    zabbixMeta: ZabbixMeta,
    healthChecks: Set[HealthCheck],
    zabbaxFile: ZabbixFile,
    actorSystem: ActorSystem
) {

  val logger = Logger(classOf[ZabbixExecutor])

  logger.info("Setting up health check in interval")

  implicit val system = actorSystem
  implicit val materializer = ActorMaterializer()

  val scheduler = actorSystem.scheduler.schedule(
    initialDelay = 10 seconds,
    interval = 4 minutes
  ) {
    executeHealthChecks().onFailure {
      case t => logger.warn("Failed to execute health check", t)
    }
  }

  def close(): Unit = {
    scheduler.cancel()
  }

  def executeHealthChecks(): Future[IOResult] = {
    Future.sequence(healthChecks.map(_.healthCheck()))
      .map(hc => Zabbix(
        meta = zabbixMeta,
        updated = DateTime.now(),
        healthChecks = hc
      ))
      .flatMap(z => writeToFile(z))
  }

  private def writeToFile(z: Zabbix): Future[IOResult] = {
    val sink = Flow[String]
      .map(s => ByteString(s))
      .toMat(FileIO.toPath(zabbaxFile.ensureWritableFile().toPath))(Keep.right)

    Source.fromFuture(Future.successful(Json.prettyPrint(z.toJson)))
      .runWith(sink)
  }

}

object ZabbixExecutor {

  def apply(
    buildInfoName: String,
    healthCheckEndpoint: String,
    healthChecks: Set[HealthCheck],
    actorSystem: ActorSystem,
    environmentMode: Mode,
    configuration: Configuration
  ): ZabbixExecutor = {
    val zabbixFilePath = environmentMode match {
      case Mode.Dev => "./target/"
      case _ => "/opt/docker/zabbix/"
    }
    Files.createDirectories(new File(zabbixFilePath).toPath)

    def resolveStringConfiguration(key: String) =
      configuration.getString(key).toRight(key).right

    val meta = for {
      env <- resolveStringConfiguration("musit.env")
      baseUrl <- resolveStringConfiguration("musit.baseUrl")
      hostname <- resolveStringConfiguration("musit.docker.hostname")
    } yield (
      ZabbixFile(zabbixFilePath, s"musit-$buildInfoName-$env-health.json"),
      ZabbixMeta(
        s"musit-$buildInfoName-$env",
        s"$hostname-$buildInfoName",
        s"$baseUrl/$healthCheckEndpoint",
        "musit-developer"
      )
    )

    meta match {
      case Right((p, m)) =>
        new ZabbixExecutor(
          zabbixMeta = m,
          healthChecks = healthChecks,
          zabbaxFile = p,
          actorSystem = actorSystem
        )
      case Left(key) =>
        throw new IllegalStateException(s"Missing configuration with key '$key'")
    }
  }

}
