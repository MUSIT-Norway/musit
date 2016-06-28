/*
 *   MUSIT is a cooperation between the university museums of Norway.
 *   Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License,
 *   or any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License along
 *   with this program; if not, write to the Free Software Foundation, Inc.,
 *   51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package no.uio.musit.microservice.event.dao

import play.api.Play
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfig }
import slick.driver.JdbcProfile

import scala.concurrent.Future

case class ControlDTO(id: Option[Long], controlType: Option[String])

object ControlDao extends HasDatabaseConfig[JdbcProfile] {

  import driver.api._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  private val ControlTable = TableQuery[ControlTable]

  def insertAction(event: ControlDTO): DBIO[Int] =
    ControlTable += event

  def getControl(id: Long): Future[Option[ControlDTO]] = {
    val action = ControlTable.filter(event => event.id === id).result.headOption
    db.run(action)
  }

  private class ControlTable(tag: Tag) extends Table[ControlDTO](tag, Some("MUSARK_EVENT"), "CONTROL") {
    def * = (id.?, controlType) <> (create.tupled, destroy) // scalastyle:ignore

    val id = column[Long]("ID", O.PrimaryKey)

    val controlType = column[Option[String]]("CONTROLTYPE")

    def create = (id: Option[Long], controlType: Option[String]) =>
      ControlDTO(id, controlType)

    def destroy(event: ControlDTO) = Some(event.id, event.controlType)
  }

}