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

import com.google.inject.{Inject, Singleton}
import models.Person
import no.uio.musit.security.AuthenticatedUser
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile

import scala.concurrent.Future

@Singleton
class ActorDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  private val actorTable = TableQuery[ActorTable]

  def getById(id: Long): Future[Option[Person]] = {
    db.run(actorTable.filter(_.id === id).result.headOption)
  }

  def getByName(searchString: String): Future[Seq[Person]] = {
    val likeArg = searchString.toUpperCase
    db.run(actorTable.filter(_.fn.toUpperCase like s"%$likeArg%").result)
  }

  def getByDataportenId(dataportenId: String): Future[Option[Person]] = {
    db.run(actorTable.filter(_.dataportenId === dataportenId).result.headOption)
  }

  def listByIds(ids: Set[Long]): Future[Seq[Person]] = {
    db.run(actorTable.filter(_.id inSet ids).result)
  }

  def insertAuthUser(user: AuthenticatedUser): Future[Person] = {
    val person = Person.fromAuthUser(user)
    insert(person)
  }

  def insert(actor: Person): Future[Person] = {
    val insQuery = actorTable returning
      actorTable.map(_.id) into ((actor, id) => actor.copy(id = id))

    val action = insQuery += actor

    db.run(action)
  }

  /* TABLE DEF using fieldnames from w3c vcard standard */
  private class ActorTable(
      tag: Tag
  ) extends Table[Person](tag, Some(MappingSchemaName), ActorTableName) {

    val id = column[Option[Long]]("NY_ID", O.PrimaryKey, O.AutoInc)
    val fn = column[String]("ACTORNAME")
    val dataportenId = column[Option[String]]("DATAPORTEN_ID")

    val create = (id: Option[Long], fn: String, dataportenId: Option[String]) =>
      Person(
        id = id,
        fn = fn,
        dataportenId = dataportenId
      )

    val destroy = (actor: Person) =>
      Some((
        actor.id,
        actor.fn,
        actor.dataportenId
      ))

    // scalastyle:off method.name
    def * = (id, fn, dataportenId) <> (create.tupled, destroy)

    // scalastyle:on method.name
  }

}

