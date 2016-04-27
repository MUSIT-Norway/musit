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
package no.uio.musit.microservice.actor.dao

import no.uio.musit.microservice.actor.domain.Actor
import no.uio.musit.microservices.common.linking.LinkService
import play.api.{Logger, Play}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfig}
import slick.driver.JdbcProfile

import scala.concurrent.Future

object ActorDao extends HasDatabaseConfig[JdbcProfile] {
  import driver.api._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  private val ActorTable = TableQuery[MusitThingTable]


  def all() : Future[Seq[Actor]] = db.run(ActorTable.result)

  def insert(musitThing: Actor): Future[Actor] = {
    val insertQuery = (ActorTable returning ActorTable.map(_.id) into ((musitThing, id) => (musitThing.copy(id=id, links=Seq(LinkService.self(s"/v1/$id"))))))
    val action = insertQuery += musitThing

    db.run(action)
  }

  def getById(id:Long) :Future[Option[Actor]] ={
    val action = ActorTable.filter( _.id === id).result.headOption
    db.run(action)
  }

  private class MusitThingTable(tag: Tag) extends Table[Actor](tag, "VIEW_MUSITTHING") {
    def id = column[Long]("NY_ID", O.PrimaryKey, O.AutoInc)// This is the primary key column
    def displayid = column[String]("DISPLAYID")
    def displayname = column[String]("DISPLAYNAME")

    def create = (id: Long , displayid:String, displayname:String) => Actor(id, displayid, displayname, Seq(LinkService.self(s"/v1/$id")))
    def destroy(thing:Actor) = Some(thing.id, thing.displayid, thing.displayname)

    def * = (id, displayid, displayname) <>(create.tupled, destroy)
  }
}


