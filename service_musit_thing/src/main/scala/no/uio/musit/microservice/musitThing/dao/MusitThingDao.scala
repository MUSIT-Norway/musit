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
package no.uio.musit.microservice.musitThing.dao

import com.google.inject.{ Inject, Singleton }
import no.uio.musit.microservice.musitThing.domain.MusitThing
import no.uio.musit.microservices.common.linking.LinkService
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfig }
import slick.driver.JdbcProfile

import scala.concurrent.Future

@Singleton
class MusitThingDao @Inject() (
    databaseConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfig[JdbcProfile] {

  import driver.api._

  protected val dbConfig = databaseConfigProvider.get[JdbcProfile]

  private val ThingTable = TableQuery[MusitThingTable] // scalastyle:ignore

  def all: Future[Seq[MusitThing]] = db.run(ThingTable.result)

  def insert(musitThing: MusitThing): Future[MusitThing] = {
    db.run(
      ThingTable returning ThingTable.map(_.id) into (
        (musitThing, id) => musitThing.copy(id = id, links = Some(Seq(LinkService.self(s"/v1/${id.getOrElse("")}"))))
      ) += musitThing
    )
  }

  def getDisplayName(id: Long): Future[Option[String]] =
    db.run(ThingTable.filter(_.id === id).map(_.displayName).result.headOption)

  def getDisplayId(id: Long): Future[Option[String]] =
    db.run(ThingTable.filter(_.id === id).map(_.displayId).result.headOption)

  def getById(id: Long): Future[Option[MusitThing]] =
    db.run(ThingTable.filter(_.id === id).result.headOption)

  private class MusitThingTable(tag: Tag) extends Table[MusitThing](tag, Some("MUSIT_MAPPING"), "VIEW_MUSITTHING") {
    val id = column[Option[Long]]("NY_ID", O.PrimaryKey, O.AutoInc)
    val displayId = column[String]("DISPLAYID")
    val displayName = column[String]("DISPLAYNAME")
    val create = (id: Option[Long], displayid: String, displayname: String) =>
      MusitThing(
        id,
        displayid,
        displayname,
        Some(Seq(LinkService.self(s"/v1/${id.getOrElse("")}")))
      )
    val destroy = (thing: MusitThing) => Some(thing.id, thing.displayid, thing.displayname)

    def * = (id, displayId, displayName) <> (create.tupled, destroy) // scalastyle:ignore
  }

}

