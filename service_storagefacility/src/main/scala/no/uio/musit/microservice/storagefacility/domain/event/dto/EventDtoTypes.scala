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

package no.uio.musit.microservice.storagefacility.domain.event.dto

import no.uio.musit.microservice.storagefacility.domain.event.EventTypeId

// TODO: Change id and partOf to EventId

sealed trait Dto {
  val id: Option[Long]
  val eventTypeId: EventTypeId
  val note: Option[String]
  val relatedSubEvents: Seq[RelatedEvents]
  val partOf: Option[Long]
  /*
     TODO: Additional attributes should be handled in a uniform way.
     Being consistent in how additional attributes are handled, allows for
     cleaner and more reusable code. It will also put less strain on the
     cognitive load for a developer, because all data of similar characteristics
     are treated the same way (pattern).
   */
  val valueLong: Option[Long]
  val valueString: Option[String]
  val valueDouble: Option[Double]
}

/**
 * The EventDto contains attributes that are common across _all_ event types.
 */
case class BaseEventDto(
  id: Option[Long],
  eventTypeId: EventTypeId,
  note: Option[String],
  relatedSubEvents: Seq[RelatedEvents],
  partOf: Option[Long],
  valueLong: Option[Long] = None,
  valueString: Option[String] = None,
  valueDouble: Option[Double] = None
) extends Dto

sealed trait DtoExtension

/**
 * Having the ExtendedDto include the base EventDto allows for easier
 * conversions between domain and to.
 */
case class ExtendedDto(
    id: Option[Long],
    eventTypeId: EventTypeId,
    note: Option[String],
    relatedSubEvents: Seq[RelatedEvents],
    partOf: Option[Long],
    valueLong: Option[Long] = None,
    valueString: Option[String] = None,
    valueDouble: Option[Double] = None,
    extension: DtoExtension
) extends Dto {

  def baseEventDto: BaseEventDto = {
    BaseEventDto(
      id = id,
      eventTypeId = eventTypeId,
      note = note,
      relatedSubEvents = relatedSubEvents,
      partOf = partOf,
      valueLong = valueLong,
      valueString = valueString,
      valueDouble = valueDouble
    )
  }

}

object ExtendedDto {

  def apply(bed: BaseEventDto, ext: DtoExtension): ExtendedDto = {
    ExtendedDto(
      id = bed.id,
      eventTypeId = bed.eventTypeId,
      note = bed.note,
      relatedSubEvents = bed.relatedSubEvents,
      partOf = bed.partOf,
      valueLong = bed.valueLong,
      valueDouble = bed.valueDouble,
      valueString = bed.valueString,
      extension = ext
    )
  }

}

/**
 * Dto to handle environment requirements.
 */
case class EnvRequirementDto(
  id: Option[Long],
  temperature: Option[Int],
  tempInterval: Option[Int],
  airHumidity: Option[Int],
  airHumInterval: Option[Int],
  hypoxicAir: Option[Int],
  hypoxicInterval: Option[Int],
  cleaning: Option[String],
  light: Option[String]
) extends DtoExtension

/**
 * Dto that handles observation events with from and to attributes.
 */
case class ObservationFromToDto(
  id: Option[Long],
  from: Option[Double],
  to: Option[Double]
) extends DtoExtension

/**
 * Dto to handle observation events related to pest control.
 */
case class ObservationPestDto(
  lifeCycles: Seq[LifecycleDto]
) extends DtoExtension

/**
 * Note: The eventId is only used during writing to the database. It is set to
 * None when reading from the database.
 *
 * FIXME: The comment above is no longer valid. Id should be set for all DTO's.
 */
case class LifecycleDto(
  eventId: Option[Long],
  stage: Option[String],
  number: Option[Int]
)

