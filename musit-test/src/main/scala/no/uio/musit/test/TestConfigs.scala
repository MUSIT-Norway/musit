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

trait TestConfigs {

  def slickWithInMemoryH2(
    dbName: String,
    evolve: String = "enabled"
  ): Map[String, Any] = Map.apply(
    "slick.dbs.default.driver" -> "slick.driver.H2Driver$",
    "slick.dbs.default.connectionTimeout" -> "20000",
    "slick.dbs.default.loginTimeout" -> "20000",
    "slick.dbs.default.socketTimeout" -> "20000",
    "slick.dbs.default.db.driver" -> "org.h2.Driver",
    "slick.dbs.default.connectionTestQuery" -> "SELECT 1",
    "slick.dbs.default.db.url" -> s"jdbc:h2:mem:$dbName;MODE=Oracle",
    "slick.dbs.default.leakDetectionThreshold" -> "5000",
    "evolutionplugin" -> evolve
  )

}
