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
import no.uio.musit.microservices.common.linking.domain.Link

trait Dto

/**
 * The EventDto contains attributes that are common across _all_ event types.
 */
case class EventDto(
  id: Option[Long],
  links: Option[Seq[Link]],
  eventType: EventTypeId,
  note: Option[String],
  relatedSubEvents: Seq[RelatedEvents],
  partOf: Option[Long],
  // TODO: It would be preferable to handle custom attributes in a uniform way.
  valueLong: Option[Long] = None,
  valueString: Option[String] = None,
  valueDouble: Option[Double] = None
) extends Dto

trait DtoExtension extends Dto

/**
 * Having the ExtendedDto include the base EventDto allows for easier
 * conversions between domain and to.
 *
 * @param baseEvent EventDto general attributes.
 * @param extension An instance of a DtoExtension.
 */
case class ExtendedDto(
  baseEvent: EventDto,
  extension: DtoExtension
) extends DtoExtension

/**
 * Dto to handle environment requirements.
 */
case class EnvRequirementDto(
  baseEvent: EventDto,
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
case class ObservationPestDto(lifeCycles: Seq[LifecycleDto]) extends DtoExtension

/**
 * Note: The eventId is only used during writing to the database. It is set to
 * None when reading from the database.
 *
 * FIXME: The comment above is no longer valid. The eventId should be set now.
 */
case class LifecycleDto(
  eventId: Option[Long],
  stage: Option[String],
  number: Option[Int]
)

