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

import no.uio.musit.microservice.event.domain.{ BaseEventDto, Dto, Event }
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.utils.ErrorHelper
import play.api.libs.json.{ JsObject, JsResult }
import slick.dbio._

import scala.concurrent.Future

/**
 * Created by jstabel on 7/7/16.
 */
/**
 * We split event implementations into four kinds:
 * 1) Those which store all their data in the base event table and doesn't use the custom generic fields (valueAsInteger etc).
 * 2) Those which store all their data in the base event table, but also use the custom generic fields.
 * 3) Those which needs a separate table. But not using the custom generic fields.
 * 4) Those which needs a separate table. *AND* also uses the custom generic fields.
 */

sealed trait EventImplementation

/**
 * For event types which don't need to store extra properties than what is in the base event table and doesn't use the custom generic fields.
 */
trait SingleTableEventType {
  def createEventInMemory(baseEventProps: BaseEventDto): Event
}

trait UsingCustomFieldsInBaseEventTable {
  def getCustomFieldsSpec: CustomFieldsSpec
}

/**
 * Abstract base trait for event types which has their own extra properties table.
 */

trait MultipleTablesEventType {
  def createEventInMemory(baseProps: BaseEventDto, customDto: Dto): Event

  //Json-stuff, consider moving this to a separate trait.
  def validateCustomDto(jsObject: JsObject): JsResult[Dto]

  def customDtoToJson(event: Event): JsObject

  /** creates an action which inserts the extended/specific properties into the database */
  def createInsertCustomDtoAction(id: Long, event: Event): DBIO[Int]

  /** reads the extended/specific properties from the database. Won't typically need the baseEventDto parameter, remove this? */
  def getCustomDtoFromDatabase(id: Long, baseEventProps: BaseEventDto): Future[Option[Dto]] //? MusitFuture[Dto]

  def getEventFromDatabase(id: Long, baseEventProps: BaseEventDto) = {
    getCustomDtoFromDatabase(id, baseEventProps)
      .toMusitFuture(ErrorHelper.badRequest(s"Unable to find ${baseEventProps.eventType.name} with id: $id"))
      .musitFutureMap(customDto => createEventInMemory(baseEventProps, customDto))
  }
}

//----------------------------------------------------------------------
// "Concrete" event traits (mixing in the above traits)
//----------------------------------------------------------------------

/**
 * For event types which don't need to store extra properties than what is in the base event table and doesn't use the custom generic fields.
 */

trait SingleTableNotUsingCustomFields extends EventImplementation with SingleTableEventType {
}

/**
 * For event types which don't need to store extra properties than what is in the base event table, but does use custom generic fields in the base event table.
 *
 * Implement this event type if you need to store anything in valueInteger or valueString.
 *
 * Remember to call super if you implement further subtypes of this event implementation type
 */
trait SingleTableUsingCustomFields extends EventImplementation with SingleTableEventType with UsingCustomFieldsInBaseEventTable {
  // with MultipleDtosEventType {

  /*#OLD
  /**
    * Interprets/reads the custom fields it needs (and copies them into the Dto).
    */
  def baseTableToCustomDto(baseEventDto: BaseEventDto): Dto

  /**
    * Stores the custom values into a BaseEventDto instance.
    * Use this if you need to store anything in valueInteger or valueString, override this method to provide this data. Gets called before the data is written to the database
    */
  def customDtoToBaseTable(event: Event, baseEventDto: BaseEventDto): BaseEventDto
*/
}

/**
 * For event types which has their own extra properties table. Does *not* use any of the custom generic fields in the base event table.
 */
trait MultipleTablesNotUsingCustomFields extends EventImplementation with MultipleTablesEventType {
}

/**
 * For event types which has their own extra properties table. Does *also* use custom generic fields in the base event table.
 */
trait MultipleTablesAndUsingCustomFields extends EventImplementation with MultipleTablesEventType with UsingCustomFieldsInBaseEventTable {
}
