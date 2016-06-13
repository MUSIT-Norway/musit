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

import no.uio.musit.microservice.event.domain.{ Event, EventType }
import no.uio.musit.microservices.common.domain.MusitNotImplementedYetException
import no.uio.musit.microservices.common.linking.LinkService
import play.api.Play
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfig }
import slick.driver.JdbcProfile

import scala.concurrent.Future

/**
 * Created by jstabel on 6/13/16.
 */
object EventDao extends HasDatabaseConfig[JdbcProfile] {
  import driver.api._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  private val EventBaseTable = TableQuery[EventBaseTable]
  // private val RoomTable = TableQuery[RoomTable]
  // private val BuildingTable = TableQuery[BuildingTable]

  def linkText(id: Option[Long]) = {
    assert(id.isDefined)
    Some(Seq(LinkService.self(s"/v1/${id.get}")))
  }

  def insertAction(eventBase: Event): DBIO[Event] = {
    val insertQuery = EventBaseTable returning EventBaseTable.map(_.id) into
      ((eventBase, idNew) => eventBase.copy(id = idNew, links = linkText(idNew)))
    val action = insertQuery += eventBase
    action
  }

  def insertBaseEvent(eventBase: Event): Future[Event] =
    db.run(insertAction(eventBase))

  def getBaseEvent(id: Long): Future[Option[Event]] = {
    throw new MusitNotImplementedYetException("EventDao.getBaseEvent")
  }

  private class EventBaseTable(tag: Tag) extends Table[Event](tag, Some("MUSARK_EVENT"), "EVENT") {
    def * = (id, eventTypeID, eventNote) <> (create.tupled, destroy)

    val id = column[Option[Long]]("ID", O.PrimaryKey, O.AutoInc)

    val eventTypeID = column[Int]("EVENT_TYPE_ID")

    val eventNote = column[Option[String]]("NOTE")

    def create = (id: Option[Long], eventTypeId: Int, note: Option[String]) =>
      Event(
        id, eventTypeId,
        note, None,
        linkText(id)
      )

    def destroy(unit: Event) = Some(unit.id, unit.eventTypeId, unit.note)

  }

}
