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

import java.io.FileInputStream
import java.nio.file.{Files, Paths}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import no.uio.musit.test.MusitSpec
import org.joda.time.{DateTime, DateTimeUtils}
import org.scalatest.BeforeAndAfterEach
import play.api.libs.json.{JsObject, JsValue, Json}

import scala.concurrent.Future

class ZabbixExecutorSpec extends MusitSpec with BeforeAndAfterEach {

  override def afterEach() = {
    DateTimeUtils.setCurrentMillisSystem()
  }

  implicit val actorSystem  = ActorSystem("ZabbixExecutorSpec")
  implicit val materializer = ActorMaterializer()

  val meta = ZabbixMeta(
    name = "musit-dev",
    instance = "localhost",
    url = "http://localhost/buildinfo",
    hostGroup = "musit-developer"
  )

  "ZabbixExecutor" when {
    "write results to file" in {
      val folder     = createTempFolder()
      val zibbixFile = ZabbixFile(folder, "musit-health.json")

      val executor: ZabbixExecutor = createExecutor(zibbixFile)
      val result                   = executor.executeHealthChecks().futureValue

      val res = Json.parse(new FileInputStream(zibbixFile.ensureWritableFile()))
      (res \ "noop").as[Boolean] mustBe true
    }

    "write result over old file" in {
      val folder                   = createTempFolder()
      val zibbixFile               = ZabbixFile(folder, "musit-health.json")
      val executor: ZabbixExecutor = createExecutor(zibbixFile)
      val currentTime              = DateTime.now()
      val latestUpdated            = currentTime.plusHours(1).getMillis

      DateTimeUtils.setCurrentMillisFixed(currentTime.getMillis)
      executor.executeHealthChecks().futureValue

      DateTimeUtils.setCurrentMillisFixed(latestUpdated)
      executor.executeHealthChecks().futureValue

      val res = Json.parse(new FileInputStream(zibbixFile.ensureWritableFile()))
      (res \ "updated").as[Long] mustBe latestUpdated
    }

    "write result over old file even when the state changes" in {
      val folder     = createTempFolder()
      val zibbixFile = ZabbixFile(folder, "musit-health.json")
      val executor: ZabbixExecutor =
        createExecutor(zibbixFile, new FlippingAvailableHealthCheck)

      (1 to 3).foreach { n =>
        executor.executeHealthChecks().futureValue
        val res = Json.parse(new FileInputStream(zibbixFile.ensureWritableFile()))
        res mustBe a[JsObject]
      }
    }
  }

  def createExecutor(
      zabbixFile: ZabbixFile,
      healthCheck: HealthCheck = new NoopHealthCheck
  ) = {
    val executor = new ZabbixExecutor(
      zabbixMeta = meta,
      healthChecks = Set(healthCheck),
      zabbaxFile = zabbixFile,
      actorSystem = actorSystem,
      materializer = materializer
    )
    executor.close()
    executor
  }

  def createTempFolder(): String = {
    val tempFolderName = s"scalatest-${System.currentTimeMillis()}"
    val tmpDir         = Paths.get(System.getProperty("java.io.tmpdir"))
    val directory      = Files.createTempDirectory(tmpDir, tempFolderName)

    directory.toString
  }
}

class NoopHealthCheck() extends HealthCheck {
  override def healthCheck() = Future.successful(
    HealthCheckStatus(
      name = "noop",
      available = true,
      responseTime = 1,
      message = None
    )
  )
}

class FlippingAvailableHealthCheck() extends HealthCheck {
  var available = true

  override def healthCheck() = {
    available = !available
    Future.successful(
      HealthCheckStatus(
        name = "flipping",
        available = available,
        responseTime = 1,
        message = None
      )
    )
  }
}
