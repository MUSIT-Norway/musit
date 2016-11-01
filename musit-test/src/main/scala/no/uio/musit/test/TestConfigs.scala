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

package no.uio.musit.test

import scala.util.Random

trait TestConfigs {

  def slickWithInMemoryH2(
    evolve: String = "enabled"
  ): Map[String, Any] = Map(
    "play.evolutions.db.default.enabled" -> false,
    "play.evolutions.db.default.autoApply" -> false,
    "play.slick.db.default" -> "test",
    "slick.dbs.test.driver" -> "slick.driver.H2Driver$",
    "slick.dbs.test.connectionTimeout" -> "20000",
    "slick.dbs.test.loginTimeout" -> "20000",
    "slick.dbs.test.socketTimeout" -> "20000",
    "slick.dbs.test.db.driver" -> "org.h2.Driver",
    "slick.dbs.test.connectionTestQuery" -> "SELECT 1",
    "slick.dbs.test.db.url" -> s"jdbc:h2:mem:musit-test${Random.nextInt()};MODE=Oracle;DB_CLOSE_DELAY=-1",
    "slick.dbs.test.leakDetectionThreshold" -> "5000",
    "evolutionplugin" -> evolve
  )

}
