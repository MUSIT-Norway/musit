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

import no.uio.musit.microservices.common.extensions.EitherExtensions.{ MusitResult, _ }
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.extensions.OptionExtensions._
import no.uio.musit.microservices.common.linking.domain.Link
import no.uio.musit.microservices.common.utils.Misc._
import no.uio.musit.microservices.common.utils.{ ErrorHelper, ResourceHelper }
import play.api.libs.json.{ JsArray, JsObject, JsResult, JsValue }

/**
 * Created by jstabel on 7/6/16.
 */
object EventHelpers {
  private def fromJsonToBaseEventProps(eventType: EventType, jsObject: JsObject, relatedSubEvents: Seq[RelatedEvents]): JsResult[BaseEventProps] = {
    for {
      id <- (jsObject \ "id").validateOpt[Long]
      links <- (jsObject \ "links").validateOpt[Seq[Link]]
      note <- (jsObject \ "note").validateOpt[String]
    } yield BaseEventProps(id, links, eventType, note, relatedSubEvents)
  }

  def invokeJsonValidator(multipleDtos: MultipleDtosEventType, eventType: EventType, jsResBaseEventProps: JsResult[BaseEventProps], jsObject: JsObject) = {
    for {
      baseProps <- jsResBaseEventProps
      customDto <- multipleDtos.validateCustomDto(jsObject)
    } yield multipleDtos.createEventInMemory(baseProps, customDto)
  }

  def fromJsonToEventResult(eventType: EventType, jsObject: JsObject, relatedSubEvents: Seq[RelatedEvents]): JsResult[Event] = {
    val jsResBaseEventProps = fromJsonToBaseEventProps(eventType, jsObject, relatedSubEvents)
    eventType.singleOrMultipleDtos match {
      case Left(singleDto) =>
        jsResBaseEventProps.map {
          baseEventProps =>
            singleDto.createEventInMemory(baseEventProps)
        }

      case Right(multipleDtos) => invokeJsonValidator(multipleDtos, eventType, jsResBaseEventProps, jsObject)

    }
  }

  def validateSingleEvent(jsObject: JsObject, relatedSubEvents: Seq[RelatedEvents]): MusitResult[Event] = {
    val evtTypeName = (jsObject \ "eventType").as[String]
    val maybeEventTypeResult = EventType.getByName(evtTypeName).toMusitResult(ErrorHelper.badRequest(s"Unknown eventType: $evtTypeName"))

    val maybeEventResult = maybeEventTypeResult.flatMap {
      eventType => fromJsonToEventResult(eventType, jsObject, relatedSubEvents) |> ResourceHelper.jsResultToMusitResult
    }
    maybeEventResult
  }

  /** Handles recursion */
  def validateEvent(jsObject: JsObject): MusitResult[Event] = {
    val subRelatedEvents = validatePotentialSubEvents(jsObject)

    subRelatedEvents.flatMap { relatedEvents =>
      validateSingleEvent(jsObject, relatedEvents)
    }
  }

  def validatePotentialSubEvents(jsObject: JsObject): MusitResult[Seq[RelatedEvents]] = {

    def mapToProperRelation(fieldName: String, jsValue: JsValue): MusitResult[(EventRelation, JsValue)] = {
      assert(fieldName.startsWith(Constants.subEventsPrefix))

      val relName = fieldName.substring(Constants.subEventsPrefix.length)
      EventRelations.getMusitResultByName(relName).map((_, jsValue))
    }

    def validateSingleRelationWithSubEvents(eventRelation: EventRelation, jsValue: JsValue): MusitResult[RelatedEvents] = {
      jsValue match {
        case jsArray: JsArray =>
          val subEvents = jsArray.value.map(jsValue => validateEvent(jsValue.asInstanceOf[JsObject]))
          val concatenatedMusitResults = concatenateMusitResults(subEvents)
          concatenatedMusitResults.map { reallySubEvents =>
            RelatedEvents(eventRelation, reallySubEvents)
          }

        case _ => Left(ErrorHelper.badRequest("expected array of subEvents in subEvent property"))
      }
    }

    val potentialSubEvents: Seq[(String, JsValue)] = jsObject.fields.filter { case (fieldName, value) => fieldName.startsWith(Constants.subEventsPrefix) }

    val withProperRelations = potentialSubEvents.map { case (fieldName, value) => mapToProperRelation(fieldName, value) }

    val result = withProperRelations.map { musitResultOfPair => musitResultOfPair.flatMap(pair => validateSingleRelationWithSubEvents(pair._1, pair._2)) }
    concatenateMusitResults(result)
  }

  def eventFromJson[T <: Event](jsValue: JsValue): MusitResult[T] = {
    validateEvent(jsValue.asInstanceOf[JsObject]).map(res => res.asInstanceOf[T])
  }

  def toJson(event: Event, recursive: Boolean): JsObject = {
    val baseJson = event.baseEventProps.toJson
    val singleEventJson = event.eventType.maybeMultipleDtos.fold(baseJson)(jsonWriter => baseJson ++ (jsonWriter.customDtoToJson(event).asInstanceOf[JsObject]))
    if (recursive && event.hasSubEvents) {
      event.relatedSubEvents.foldLeft(singleEventJson) {
        (resultSoFar, relationWithSubEvents) =>
          val subEventsJsonSeq = relationWithSubEvents.events.map(subEvent => toJson(subEvent, recursive))
          resultSoFar.+((Constants.subEventsPrefix + relationWithSubEvents.relation.name, JsArray(subEventsJsonSeq)))
      }
    } else
      singleEventJson
  }

  def eventDtoToStoreInDatabase(event: Event, parentId: Option[Long]) = {
    event.eventType.maybeSingleTableMultipleDtos match {
      case Some(singleTableMultipleDtos) => singleTableMultipleDtos.customDtoToBaseTable(event, event.baseEventProps.toBaseEventDto(parentId))
      case None => event.baseEventProps.toBaseEventDto(parentId)
    }
  }
}
