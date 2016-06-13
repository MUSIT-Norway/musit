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

package no.uio.musit.microservice.event.service

import no.uio.musit.microservice.event.dao.EventDao
import no.uio.musit.microservices.common.domain.MusitError
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.utils.Misc._
import no.uio.musit.microservices.common.utils.{ ErrorHelper, ServiceHelper }
import no.uio.musit.microservice.event.domain._
import play.api.libs.json.{ JsObject, Json }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Created by jstabel on 6/10/16.
 */
trait EventService {

  type MusitResult[T] = Either[MusitError, T]
  type MusitFuture[T] = Future[MusitResult[T]]

  //A separate function for this message because we want to verify we get this error message in some of the integration tests
  def unknownEventMsg(id: Long) = s"Unknown event with id: $id"

  private def eventNotFoundError(id: Long): MusitError = {
    ErrorHelper.notFound(unknownEventMsg(id))
  }

  def eventInfoToComplexEvent(eventInfo: EventInfo): ComplexEvent = {
    val eventType = EventType(eventInfo.eventType)

    def getNote = {
      eventInfo.eventData.flatMap(jsObject => (jsObject \ "note").toOption.map(_.toString))
    }
    def interpretEventBase = {
      Event(None, eventType.eventTypeId, getNote, None, None)
    }

    eventType match {
      case MoveEventType => ComplexEvent(interpretEventBase, None)
      case ControlEventType => ComplexEvent(interpretEventBase, None)
      case ObservationEventType => ComplexEvent(interpretEventBase, None)
    }
  }

  def baseEventDataToJson(baseEvent: Event): Option[JsObject] = {
    val hasData = baseEvent.note.isDefined // or blablabla
    if (hasData) {
      Some(Json.obj("note" -> Json.toJson(baseEvent.note)))
    } else None
  }

  def complexEventToEventInfo(complexEvent: ComplexEvent): EventInfo = {
    val baseEvent = complexEvent.baseEvent
    val eventTypeName = baseEvent.eventType.typename
    val jsObject = baseEventDataToJson(baseEvent) //Todo: Include more attributes (including from the eventExtension object)
    EventInfo(baseEvent.id, eventTypeName, complexEvent.allAtomLinks, jsObject)
  }

  def eventToEventInfo(baseEvent: Event): EventInfo = {
    ComplexEvent(baseEvent, None) |> complexEventToEventInfo
  }

  def createEvent(eventInfo: EventInfo): MusitFuture[EventInfo] = {

    val complexEvent = eventInfoToComplexEvent(eventInfo)

    ServiceHelper.daoInsert(EventDao.insertBaseEvent(complexEvent.baseEvent).map(eventToEventInfo))
  }

  private def getBaseEvent(id: Long): MusitFuture[Event] = EventDao.getBaseEvent(id).toFutureEither(eventNotFoundError(id))

  def getById(id: Long): MusitFuture[ComplexEvent] = {
    val musitFutureBaseEvent = getBaseEvent(id)

    musitFutureBaseEvent.futureEitherMap(baseEvent => ComplexEvent(baseEvent, None)) //TEMP!!! Fjernes når nedenstående kommer inn
    //Todo, for de andre event-typene
    /*
    musitFutureBaseEvent.futureEitherFlatMap { baseEvent =>
      storageUnit.storageKind match {
        case StUnit => Future.successful(Right(StorageUnitTriple.createStorageUnit(storageUnit)))
        case Building => getBuildingById(id).futureEitherMap(storageBuilding => StorageUnitTriple.createBuilding(storageUnit, storageBuilding))
        case Room => getRoomById(id).futureEitherMap(storageRoom => StorageUnitTriple.createRoom(storageUnit, storageRoom))
      }
    }*/
  }

}

object EventService extends EventService
