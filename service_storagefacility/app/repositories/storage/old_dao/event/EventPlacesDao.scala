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

package repositories.storage.old_dao.event

import com.google.inject.{Inject, Singleton}
import models.storage.event.dto.EventRolePlace
import no.uio.musit.models.{EventId, MuseumId}
import play.api.db.slick.DatabaseConfigProvider
import repositories.storage.old_dao.EventTables

import scala.concurrent.Future

@Singleton
class EventPlacesDao @Inject()(
    val dbConfigProvider: DatabaseConfigProvider
) extends EventTables {

  import profile.api._

  def insertPlaces(
      eventId: EventId,
      relatedPlaces: Seq[EventRolePlace]
  ): DBIO[Option[Int]] = {
    val relPlaces = relatedPlaces.map(_.copy(eventId = Some(eventId)))
    eventPlacesTable ++= relPlaces
  }

  def getRelatedPlaces(mid: MuseumId, eventId: EventId): Future[Seq[EventRolePlace]] = {
    val query = eventPlacesTable.filter(evt => evt.eventId === eventId)
    db.run(query.result)
  }

}
