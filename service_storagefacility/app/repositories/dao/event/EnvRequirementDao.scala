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
import models.event.dto.EnvRequirementDto
import no.uio.musit.models.EventId
import play.api.db.slick.DatabaseConfigProvider
import repositories.dao.EventTables

import scala.concurrent.Future

/**
 * TODO: Document me!
 */
@Singleton
class EnvRequirementDao @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends EventTables {

  import driver.api._

  /**
   * TODO: Document me!
   */
  def insertAction(event: EnvRequirementDto): DBIO[Int] =
    envReqTable += event

  /**
   * TODO: Document me!
   */
  def getEnvRequirement(id: EventId): Future[Option[EnvRequirementDto]] =
    db.run(envReqTable.filter(event => event.id === id).result.headOption)

}

