/*
 *   MUSIT is a cooperation between the university museums of Norway.
 *   Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License,
 *   or any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License along
 *   with this program; if not, write to the Free Software Foundation, Inc.,
 *   51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package no.uio.musit.microservices.common

import org.scalatest.concurrent.PatienceConfiguration
import scala.concurrent.duration._

object PlayTestDefaults {

  val timeout = PatienceConfiguration.Timeout(5 seconds)

  def inMemoryDatabaseConfig(evolve:String = "enabled"):Map[String, String] = Map.apply (
    "slick.dbs.default.driver" -> "slick.driver.H2Driver$",
    "slick.dbs.default.connectionTimeout" -> "20s",
    "slick.dbs.default.loginTimeout" -> "20s",
    "slick.dbs.default.socketTimeout" -> "20s",
    "slick.dbs.default.db.driver" -> "org.h2.Driver",
    "slick.dbs.default.connectionTestQuery" -> "SELECT 1",
    "slick.dbs.default.db.url" -> "jdbc:h2:mem:play-test",
    "slick.dbs.default.leakDetectionThreshold" -> "5000",
    "evolutionplugin" -> evolve
  )
}
