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
package no.uio.musit.microservices.common.linking.dao

import no.uio.musit.microservices.common.domain.BaseMusitDomain
import no.uio.musit.microservices.common.linking.domain.Link
import play.api.Play
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfig }
import slick.driver.JdbcProfile
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

object LinkDao extends HasDatabaseConfig[JdbcProfile] {
  import driver.api._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  private val linkTable = TableQuery[LinkTable]

  def insert(ownerTable: BaseMusitDomain, rel: String, href: String): Future[Unit] = db.run(linkTable += Link(None, ownerTable.id, rel, href)).map { _ => () }

  def findByLocalTableId(id: Long): Future[Seq[Link]] = db.run(linkTable.filter(_.localTableId === id).result)

  def findAllLinks(): Future[Seq[Link]] = db.run(linkTable.result)

  /*
   * Every microservice using this functionality need to add the following to their evolution script:
   * CREATE TABLE URI_LINKS (
   *   ID bigint(20) NOT NULL AUTO_INCREMENT,
   *   LOCAL_TABLE_ID bigint(20) NOT NULL,
   *   REL varchar(255) NOT NULL,
   *   HREF varchar(2000) NOT NULL,
   *   PRIMARY KEY (ID)
   * );
   */
  private class LinkTable(tag: Tag) extends Table[Link](tag, "URI_LINKS") {
    def id = column[Option[Long]]("ID", O.PrimaryKey) // This is the primary key column
    def localTableId = column[Option[Long]]("LOCAL_TABLE_ID")
    def rel = column[String]("REL")
    def href = column[String]("HREF")
    def * = (id, localTableId, rel, href) <> (Link.tupled, Link.unapply _)
  }
}
