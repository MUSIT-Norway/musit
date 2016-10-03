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

import no.uio.musit.microservice.event.domain.{LifeCycleDto, ObservationPestDto, ObservationPestDto$}
import no.uio.musit.microservices.common.utils.DaoHelper
import play.api.Play
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfig}
import slick.driver.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Created by jstabel on 7/8/16.
 */
object ObservationPestDao extends HasDatabaseConfig[JdbcProfile] {

  import driver.api._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  private val LifeCycleTable = TableQuery[LifeCycleTable]

  // The insertAction and getObservation are somewhat more complex than necessary because I don't know how to remove the
  // EventId field from the livssyklus case class and still get it inserted using Slick. Please feel free to remove the
  // need for the EventId in the dto, that would clean this up a bit.

  def insertAction(eventId: Long, obsDto: ObservationPestDto): DBIO[Int] = {
    val lifeCyclesWithEventId = obsDto.lifeCycles.map { lifeCycle => lifeCycle.copy(eventId = Some(eventId)) }
    DaoHelper.mapMultiRowInsertResultIntoOk(
      LifeCycleTable ++= lifeCyclesWithEventId
    )
  }

  def getObservation(id: Long): Future[Option[ObservationPestDto]] =
    db.run(LifeCycleTable.filter(lifeCycle => lifeCycle.eventId === id).result).
      map {
        seqLifeCycle =>
          if (seqLifeCycle.isEmpty) {
            None
          } else {
            val seqLifeCycleWithoutEventIds = seqLifeCycle.map(_.copy(eventId = None)) //We don't want the eventIds in json output.
            Some(ObservationPestDto(seqLifeCycleWithoutEventIds))
          }
      }

  private class LifeCycleTable(tag: Tag) extends Table[LifeCycleDto](tag, Some("MUSARK_EVENT"), "OBSERVATION_PEST_LIFECYCLE") {
    def * = (eventId, stage, number) <> (create.tupled, destroy) // scalastyle:ignore

    val eventId = column[Option[Long]]("EVENT_ID")
    val stage = column[Option[String]]("STAGE")
    val number = column[Option[Int]]("NUMBER")

    def create = (eventId: Option[Long], stage: Option[String], number: Option[Int]) =>
      LifeCycleDto(eventId, stage, number)

    def destroy(lifeCycle: LifeCycleDto) = Some(lifeCycle.eventId, lifeCycle.stage, lifeCycle.number)
  }

}
