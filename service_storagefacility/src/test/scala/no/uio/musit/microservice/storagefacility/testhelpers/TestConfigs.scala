/*
 * MUSIT is a museum database to archive natural and cultural history data.
 * Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package no.uio.musit.microservice.storagefacility.testhelpers

import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{ Millis, Seconds, Span }

import scala.concurrent.Future

object TestConfigs {

  val timeout = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(500, Millis)
  )

  trait WaitLonger {
    implicit val defaultPatience = timeout
  }

  def inMemoryDatabaseConfig(evolve: String = "enabled"): Map[String, Any] = Map.apply(
    "slick.dbs.default.driver" -> "slick.driver.H2Driver$",
    "slick.dbs.default.connectionTimeout" -> "20000",
    "slick.dbs.default.loginTimeout" -> "20000",
    "slick.dbs.default.socketTimeout" -> "20000",
    "slick.dbs.default.db.driver" -> "org.h2.Driver",
    "slick.dbs.default.connectionTestQuery" -> "SELECT 1",
    "slick.dbs.default.db.url" -> "jdbc:h2:mem:play-test",
    "slick.dbs.default.leakDetectionThreshold" -> "5000",
    "evolutionplugin" -> evolve
  )

  def waitFutureValue[T](fut: Future[T]) = {
    fut.futureValue(timeout)
  }

}
