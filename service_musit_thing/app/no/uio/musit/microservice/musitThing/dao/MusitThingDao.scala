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

import no.uio.musit.microservice.musitThing.domain.MusitThing
import no.uio.musit.microservices.common.linking.LinkService
import play.api.{Logger, Play}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfig}
import slick.driver.JdbcProfile

import scala.concurrent.Future

object MusitThingDao extends HasDatabaseConfig[JdbcProfile] {
  import driver.api._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  private val MusitThingTable = TableQuery[MusitThingTable]


  def all() : Future[Seq[MusitThing]] = db.run(MusitThingTable.result)

  def insert(musitThing: MusitThing): Future[MusitThing] = {
    val insertQuery = (MusitThingTable returning MusitThingTable.map(_.id) into ((musitThing, id) => (musitThing.copy(id=id, links=Seq(LinkService.self(s"/v1/$id"))))))
    val action = insertQuery += musitThing

    db.run(action)
  }

  def getDisplayName(id:Long) :Future[Option[String]] ={
    val action = MusitThingTable.filter( _.id === id).map(_.displayname).result.headOption
    db.run(action)
  }

  def getDisplayID(id:Long) :Future[Option[String]] ={
    val action = MusitThingTable.filter( _.id === id).map(_.displayid).result.headOption
    db.run(action)
  }

  def getById(id:Long) :Future[Option[MusitThing]] ={
    val action = MusitThingTable.filter( _.id === id).result.headOption
    db.run(action)
  }

  private class MusitThingTable(tag: Tag) extends Table[MusitThing](tag, Some("MUSIT_MAPPING"),"VIEW_MUSITTHING") {
    def id = column[Long]("NY_ID", O.PrimaryKey, O.AutoInc)// This is the primary key column
    def displayid = column[String]("DISPLAYID")
    def displayname = column[String]("DISPLAYNAME")

    def create = (id: Long , displayid:String, displayname:String) => MusitThing(id, displayid, displayname, Seq(LinkService.self(s"/v1/$id")))
    def destroy(thing:MusitThing) = Some(thing.id, thing.displayid, thing.displayname)

    def * = (id, displayid, displayname) <>(create.tupled, destroy)
  }
}


