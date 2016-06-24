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

package no.uio.musit.microservice.event.domain

import no.uio.musit.microservices.common.extensions.FutureExtensions._
import play.api.Play
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfig }
import play.api.libs.json.{ JsObject, JsResult, JsValue, Json }
import slick.dbio._
import slick.driver.JdbcProfile

import scala.concurrent.Future

case class ControlDTO(blablabla: Option[String])

object ControlDTO {
  implicit val format = Json.format[ControlDTO]
}

class Control(eventType: EventType, baseDTO: BaseEventDTO, val controlDTO: ControlDTO) extends Event(eventType, baseDTO) {
}

object Control extends EventFactory {
  override def fromJson(eventType: EventType, baseResult: JsResult[BaseEventDTO], jsObject: JsObject): JsResult[Control] = {
    for {
      baseDto <- baseResult
      controlEventDto <- jsObject.validate[ControlDTO]
    } yield new Control(eventType, baseDto, controlEventDto)
  }

  def toJson(event: Event): JsValue = Json.toJson(event.asInstanceOf[Control].controlDTO)

  def fromDatabase(eventType: EventType, id: Long, baseEventDto: BaseEventDTO): MusitFuture[Event] = ???

  def createDatabaseInsertAction(id: Long, event: Event): DBIO[Int] = ???
}

/*TODO!
object ControlDAO extends HasDatabaseConfig[JdbcProfile] {

  import driver.api._


  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  private val ControlTable = TableQuery[ControlTable]


  def insertAction(newId: Long, event: Control): DBIO[Int] = {
    val dtoToInsert = event.ControlDTO.copy(id = Some(newId))
    val action = ControlTable += dtoToInsert
    action
  }

  def getControl(id: Long): Future[Option[ControlDTO]] = {
    val action = ControlTable.filter(event => event.id === id).result.headOption
    db.run(action)
  }


  private class ControlTable(tag: Tag) extends Table[ControlDTO](tag, Some("MUSARK_EVENT"), "Control") {
    def * = (id, temperature) <>(create.tupled, destroy) // scalastyle:ignore

    val id = column[Option[Long]]("ID", O.PrimaryKey)

    val temperature = column[Option[Double]]("TEMPERATURE")

    def create = (id: Option[Long], temperature: Option[Double]) =>
      ControlDTO(
        id, temperature
      )

    def destroy(event: ControlDTO) = Some(event.id, event.temperature)
  }

}
*/ 