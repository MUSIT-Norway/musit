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

import java.sql.{ Date, Timestamp }

import no.uio.musit.microservice.event.service.CustomFieldsHandler
import no.uio.musit.microservices.common.extensions.OptionExtensions._
import no.uio.musit.microservices.common.linking.domain.Link
import org.joda.time.{ DateTimeZone, Instant }
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json._

/**
 * Created by jstabel on 7/7/16.
 */

object BaseEventDto {

  val isoFormat = ISODateTimeFormat.dateTime.withZone(DateTimeZone.getDefault)
  def timeStampToIsoFormat(timestamp: Timestamp) = {
    val instant = new Instant(timestamp.getTime())
    isoFormat.print(instant)
  }


  
  def optTimeStampToIsoFormat(timestamp: Option[Timestamp]) = { timestamp.map(timeStampToIsoFormat) }

  implicit object baseEventPropsWrites extends Writes[BaseEventDto] {

    def writes(baseEventDto: BaseEventDto): JsValue = {

      require(baseEventDto.relatedActors.length <= 1, "This code must be changed when we get multiple related actors in the future!")
      require(baseEventDto.relatedObjects.length <= 1, "This code must be changed when we get multiple related objects in the future!")

      var jsObj = Json.obj(
        "id" -> baseEventDto.id,
        "links" -> baseEventDto.links,
        "eventType" -> baseEventDto.eventType,
        "registeredBy" -> baseEventDto.registeredBy,
        "registeredDate" -> optTimeStampToIsoFormat(baseEventDto.registeredDate)
      )

      baseEventDto.note.foreach { note =>
        jsObj = jsObj + ("note", JsString(note))
      }

      baseEventDto.eventDate.foreach { eventDate =>
        jsObj = jsObj + ("doneDate", JsString(eventDate.toString))
      }

      baseEventDto.relatedActors.foreach { relatedActor =>
        jsObj = jsObj + ("doneBy", JsNumber(relatedActor.actorId))
        //Currently we only have one actor, this code must be changed when we get multiple related actors for events.
      }

      baseEventDto.relatedObjects.foreach { relatedObject =>
        jsObj = jsObj + ("doneWith", JsNumber(relatedObject.objectId))
        //Currently we only have one related object, this code must be changed when we get multiple related objects for events.
      }

      baseEventDto.links match {
        case Some(links) if (links.nonEmpty) =>
          val linksAsJson = Json.toJson(links)
          jsObj = jsObj + ("links", linksAsJson)
        case _ => ()
      }

      CustomFieldsHandler.writeCustomFieldsToJsonIfAny(baseEventDto, jsObj)
    }
  }
}

case class ActorWithRole(roleId: Int, actorId: Int) {
  def toEventRoleActor(eventId: Long) = EventRoleActor(eventId, roleId, actorId)
}

case class EventRoleActor(eventId: Long, roleId: Int, actorId: Int) {
  def toActorWithRole = ActorWithRole(roleId, actorId)
}

case class ObjectWithRole(roleId: Int, objectId: Long) {
  def toEventRoleObject(eventId: Long) = EventRoleObject(eventId, roleId, objectId)
}

case class EventRoleObject(eventId: Long, roleId: Int, objectId: Long) {
  def toObjectWithRole = ObjectWithRole(roleId, objectId)
}

case class LocalObject(objectId: Long, latestMoveId: Option[Long], currentLocationId: Option[Int]) {

}

//RegisteredBy and registeredDate are options even though they are required in the database, because they will be None in input-json
case class BaseEventDto(id: Option[Long], links: Option[Seq[Link]], eventType: EventType, eventDate: Option[Date],
    relatedActors: Seq[ActorWithRole], relatedObjects: Seq[ObjectWithRole],
    note: Option[String],
    relatedSubEvents: Seq[RelatedEvents], partOf: Option[Long], valueLong: Option[Long],
    valueString: Option[String], valueDouble: Option[Double], registeredBy: Option[String],
    registeredDate: Option[Timestamp]) {
  def toJson: JsObject = Json.toJson(this).asInstanceOf[JsObject]

  def getOptBool = valueLong match {
    case Some(1) => Some(true)
    case Some(0) => Some(false)
    case None => None
    case n => throw new Exception(s"Boolean value encoded as an opt integer should be either None, 0 or 1, not $n.")
    //If this happens, we have a bug in our code!
  }

  def getBool = getOptBool.getOrFail("Missing required custom boolean value")

  def setBool(value: Boolean) = this.copy(valueLong = Some(if (value) 1 else 0))

  def getOptString: Option[String] = valueString

  def getString: String = getOptString.getOrFail("Missing required custom string value")

  def setString(value: String) = this.copy(valueString = Some(value))

  def setOptString(value: Option[String]) = {
    value match {
      case Some(s) => setString(s)
      case None => this.copy(valueString = None)
    }
  }

  def getOptDouble: Option[Double] = valueDouble

  def getDouble: Double = getOptDouble.getOrFail("Missing required custom double value")

  def setDouble(value: Double) = this.copy(valueDouble = Some(value))

  def setOptDouble(value: Option[Double]) = {
    value match {
      case Some(s) => setDouble(s)
      case None => this.copy(valueDouble = None)
    }
  }

}
