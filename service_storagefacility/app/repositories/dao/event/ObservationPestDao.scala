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

package repositories.dao.event

import com.google.inject.{Inject, Singleton}
import models.event.dto.ObservationPestDto
import no.uio.musit.models.EventId
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.dao.EventTables

import scala.concurrent.Future

@Singleton
class ObservationPestDao @Inject()(
    val dbConfigProvider: DatabaseConfigProvider
) extends EventTables {

  import profile.api._

  /**
   * The insertAction and getObservation are somewhat more complex than
   * necessary because I don't know how to remove the EventId field from the
   * lifecycle case class and still get it inserted using Slick. Please feel
   * free to remove the need for the EventId in the dto, that would clean this
   * up a bit.
   */
  def insertAction(parentId: EventId, obsDto: ObservationPestDto): DBIO[Int] = {
    // Need to enrich the lifecycles with the parentId
    val lifeCyclesWithEventId = obsDto.lifeCycles.map { lifeCycle =>
      lifeCycle.copy(eventId = Some(parentId))
    }
    (lifeCycleTable ++= lifeCyclesWithEventId).map { maybeInt =>
      maybeInt.fold(1)(identity)
    }
  }

  def getObservation(eventId: EventId): Future[Option[ObservationPestDto]] =
    db.run(lifeCycleTable.filter(lifeCycle => lifeCycle.eventId === eventId).result)
      .map {
        case Nil        => None
        case lifeCycles => Some(ObservationPestDto(lifeCycles))
      }

}
