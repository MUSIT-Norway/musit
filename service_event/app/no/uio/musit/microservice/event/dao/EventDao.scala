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

import no.uio.musit.microservice.event.domain.{ AtomLink, CompleteEvent, Event }
import no.uio.musit.microservices.common.extensions.OptionExtensions._
import no.uio.musit.microservices.common.linking.LinkService
import no.uio.musit.microservices.common.linking.dao.LinkDao
import no.uio.musit.microservices.common.utils.Misc._
import play.api.Play
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfig }
import slick.driver.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Created by jstabel on 6/13/16.
 */
object EventDao extends HasDatabaseConfig[JdbcProfile] {

  import driver.api._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  private val EventBaseTable = TableQuery[EventBaseTable]
  //private val EventLinkTable = TableQuery[EventLinkTable]

  def linkText(id: Long) = {
    LinkService.local(Some(id), "self", s"/v1/${id}")
  }

  def insertAction(eventBase: Event): DBIO[Event] = {
    val insertQuery = EventBaseTable returning EventBaseTable.map(_.id) into
      ((eventBase, idNew) => eventBase.copy(id = idNew))
    val action = insertQuery += eventBase
    action
  }

  def insertBaseEventAction(eventBase: Event, links: Seq[AtomLink]): DBIO[CompleteEvent] = {
    def idOfEvent(eventBase: Event) = eventBase.id.getOrThrow("missing eventId in eventDao.insertBaseEvent ")
    def copyEventIdIntoLinks(eventBase: Event) = links.map(l => l.toLink(idOfEvent(eventBase)))
    def selfLink(eventBase: Event) = linkText(idOfEvent(eventBase))
    def selfLinkAsAtomLink(eventBase: Event) = selfLink(eventBase) |> AtomLink.createFromLink

    (for {
      base <- insertAction(eventBase)
      _ <- LinkDao.insertLinksAction(copyEventIdIntoLinks(base))
      _ <- selfLink(base) |> LinkDao.insertLinkAction
    } yield CompleteEvent(base, None, Some(selfLinkAsAtomLink(base) +: links)))
      .transactionally
  }

  def getBaseEvent(id: Long): Future[Option[Event]] = {
    val action = EventBaseTable.filter(event => event.id === id).result.headOption
    db.run(action)
  }

  private class EventBaseTable(tag: Tag) extends Table[Event](tag, Some("MUSARK_EVENT"), "EVENT") {
    def * = (id, eventTypeID, eventNote) <> (create.tupled, destroy) // scalastyle:ignore

    val id = column[Option[Long]]("ID", O.PrimaryKey, O.AutoInc)

    val eventTypeID = column[Int]("EVENT_TYPE_ID")

    val eventNote = column[Option[String]]("NOTE")

    def create = (id: Option[Long], eventTypeId: Int, note: Option[String]) =>
      Event(
        id, eventTypeId,
        note
      )

    def destroy(unit: Event) = Some(unit.id, unit.eventTypeId, unit.note)

  }

}
