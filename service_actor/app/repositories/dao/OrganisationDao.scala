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
import models.Organisation
import no.uio.musit.models.OrgId
import no.uio.musit.service.MusitResults._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

import scala.concurrent.Future

@Singleton
class OrganisationDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile] with ColumnTypeMappers {

  import driver.api._

  private val orgTable = TableQuery[OrganisationTable]

  def getById(id: OrgId): Future[Option[Organisation]] = {
    db.run(orgTable.filter(_.id === id).result.headOption)
  }

  def getByName(searchString: String): Future[Seq[Organisation]] = {
    val query = orgTable.filter { org =>
      (org.fn like s"%$searchString%") || (org.nickname like s"%$searchString%")
    }
    db.run(query.result)
  }

  def insert(organization: Organisation): Future[Organisation] = {
    val query = orgTable returning
      orgTable.map(_.id) into ((organization, id) => organization.copy(id = id))

    val action = query += organization

    db.run(action)
  }

  def update(org: Organisation): Future[MusitResult[Option[Int]]] = {
    // "Record was updated!"
    val query = orgTable.filter(_.id === org.id).update(org)
    db.run(query).map {
      case upd: Int if upd == 0 => MusitSuccess(None)
      case upd: Int if upd == 1 => MusitSuccess(Some(upd))
      case upd: Int if upd > 1 => MusitDbError("Too many records were updated")
    }
  }

  def delete(id: OrgId): Future[Int] = {
    db.run(orgTable.filter(_.id === id).delete)
  }

  private class OrganisationTable(
      tag: Tag
  ) extends Table[Organisation](tag, Some(SchemaName), OrgTableName) {

    val id = column[Option[OrgId]]("ID", O.PrimaryKey, O.AutoInc)
    val fn = column[String]("FN")
    val nickname = column[String]("NICKNAME")
    val tel = column[String]("TEL")
    val web = column[String]("WEB")

    val create = (
      id: Option[OrgId],
      fn: String,
      nickname: String,
      tel: String,
      web: String
    ) => Organisation(id, fn, nickname, tel, web)

    val destroy = (org: Organisation) =>
      Some((org.id, org.fn, org.nickname, org.tel, org.web))

    // scalastyle:off method.name
    def * = (id, fn, nickname, tel, web) <> (create.tupled, destroy)

    // scalastyle:on method.name
  }

}
