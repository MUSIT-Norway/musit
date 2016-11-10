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

package repositories.dao

import java.util.UUID

import com.google.inject.{Inject, Singleton}
import models.Person
import no.uio.musit.models.{ActorId, DatabaseId}
import no.uio.musit.security.AuthenticatedUser
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile

import scala.concurrent.Future

@Singleton
class ActorDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile] with ColumnTypeMappers {

  import driver.api._

  private val actorTable = TableQuery[ActorTable]

  def getByDbId(id: DatabaseId): Future[Option[Person]] = {
    db.run(actorTable.filter(_.id === id).result.headOption)
  }

  def getByActorId(uuid: ActorId): Future[Option[Person]] = {
    val query = actorTable.filter { a =>
      a.applicationId === uuid || a.dpId === uuid
    }
    db.run(query.result.headOption)
  }

  def getByName(searchString: String): Future[Seq[Person]] = {
    val likeArg = searchString.toUpperCase
    db.run(actorTable.filter(_.fn.toUpperCase like s"%$likeArg%").result)
  }

  def getByDataportenId(dataportenId: ActorId): Future[Option[Person]] = {
    db.run(actorTable.filter(_.dpId === dataportenId).result.headOption)
  }

  def listBy(ids: Set[ActorId]): Future[Seq[Person]] = {
    db.run(actorTable.filter { a =>
      (a.applicationId inSet ids) || (a.dpId inSet ids)
    }.result)
  }

  def insertAuthUser(user: AuthenticatedUser): Future[Person] = {
    val person = Person.fromAuthUser(user)
    insert(person)
  }

  def insert(actor: Person): Future[Person] = {
    val insAct = actor.copy(applicationId = Option(ActorId.generate()))
    val insQuery = actorTable returning
      actorTable.map(_.id) into ((insAct, id) => insAct.copy(id = id))

    val action = insQuery += insAct

    db.run(action)
  }

  /* TABLE DEF using fieldnames from w3c vcard standard */
  private class ActorTable(
      tag: Tag
  ) extends Table[Person](tag, Some(MappingSchemaName), ActorTableName) {

    val id = column[Option[DatabaseId]]("ACTOR_ID", O.PrimaryKey, O.AutoInc)
    val fn = column[String]("ACTORNAME")
    val dpId = column[Option[ActorId]]("DATAPORTEN_UUID")
    val dpUsername = column[Option[String]]("DATAPORTEN_USERNAME")
    val applicationId = column[Option[ActorId]]("APPLICATION_UUID")

    val create = (
      id: Option[DatabaseId],
      fn: String,
      dataportenId: Option[ActorId],
      dataportenUsername: Option[String],
      applicationId: Option[ActorId]
    ) => Person(
      id = id,
      fn = fn,
      dataportenId = dataportenId,
      dataportenUser = dataportenUsername,
      applicationId = applicationId
    )

    val destroy = (actor: Person) =>
      Some((
        actor.id,
        actor.fn,
        actor.dataportenId,
        actor.dataportenUser,
        actor.applicationId
      ))

    // scalastyle:off method.name
    def * = (id, fn, dpId, dpUsername, applicationId) <> (create.tupled, destroy)

    // scalastyle:on method.name
  }

}

