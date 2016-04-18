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
package no.uio.musit.microservice.service_musit_thing.dao

import no.uio.musit.microservice.service_musit_thing.domain.MusitThing
import play.api.Play
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfig}
import slick.driver.JdbcProfile

import scala.concurrent.Future

class MusitThingDao extends HasDatabaseConfig[JdbcProfile] {
  import driver.api._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  private val MusitThingTable = TableQuery[MusitThingTable]

  def all() : Future[Seq[MusitThing]] = db.run(MusitThingTable.result)

  def insert(musitThing: MusitThing): Future[Unit] = db.run(MusitThingTable += musitThing).map { _ => () }

  private class MusitThingTable(tag: Tag) extends Table[MusitThing](tag, "VIEW_MUSITTHING") {
    def id = column[Long]("NY_ID", O.PrimaryKey) // This is the primary key column
    def displayid = column[String]("DISPLAYID")
    def displayname = column[String]("DISPLAYNAME")
    def * = (id, displayid, displayname) <> (MusitThing.tupled, MusitThing.unapply _)
  }
}


