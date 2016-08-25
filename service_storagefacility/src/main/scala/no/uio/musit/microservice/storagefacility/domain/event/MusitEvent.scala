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

package no.uio.musit.microservice.storagefacility.domain.event

import no.uio.musit.microservice.storagefacility.domain.event.EventTypeRegistry.{ SubEvent, subEventWithNameOption }
import no.uio.musit.microservice.storagefacility.domain.event.Implicits._
import no.uio.musit.microservice.storagefacility.domain.event.PropTypes.PropValidation
import no.uio.musit.microservices.common.linking.domain.Link
import play.api.data.validation.ValidationError
import play.api.libs.functional.syntax._
import play.api.libs.json.{ Reads, _ }

/**
 * Top level representation of _all_ event types with definitions for the
 * shared attributes they all contain.
 */
trait MusitEvent {
  val baseEvent: MusitEventBase
  val eventType: EventType
}

/**
 * Helps to identify events that are only valid in a "sub-event" context.
 */
trait MusitSubEvent extends MusitEvent {
  val properties: EventProps
}

object MusitSubEvent {

  implicit val format: Format[MusitEventBase] = (
    (__ \ "id").formatNullable[Long] and
    (__ \ "links").formatNullable[Seq[Link]] and
    (__ \ "note").formatNullable[String] and
    (__ \ "partOf").formatNullable[Long]
  )(MusitEventBase.apply, unlift(MusitEventBase.unapply))

  /**
   * Handy function to make JSON parsing of MusitSubEvent sub-types more to the
   * point. It will by default make the shared attributes defined in the parent
   * available for the function that initializes the sub-type.
   * Also, the JSON is passed in to the init function to allow further parsing.
   */
  // TODO: Clean up?
  def fromJson[A <: MusitSubEvent](
    init: (JsValue, MusitEventBase, EventType, EventProps) => JsResult[A]
  ): Reads[A] = Reads { json =>

    (for {
      base <- __.read[MusitEventBase].reads(json)
      evtType <- (json \ "eventType").validate[EventType]
      props <- (json \ "properties").validate[EventProps]
    } yield {
      subEventWithNameOption(evtType.name).map { se =>
        SubEvent.validateProps(se, props) match {
          case PropValidation.ValidProps(validProps) =>
            init(json, base, evtType, validProps)

          case PropValidation.InvalidProps(missing, invalid, badTypes) =>
            val basePath = JsPath() \ "properties"
            val errMissing = missing.toSeq.map { key =>
              val err = ValidationError(s"Missing required key $key")
              basePath \ key -> Seq(err)
            }
            val errInvalid = invalid.toSeq.map { key =>
              val err = ValidationError(s"Invalid key $key")
              basePath \ key -> Seq(err)
            }

            val errBadType = badTypes.toSeq.map { key =>
              val err = ValidationError(s"Incorrect value type for key $key")
              basePath \ key -> Seq(err)
            }

            JsError(errMissing ++ errInvalid ++ errBadType)
        }
      }.getOrElse(JsError(s"Did not recognize ${evtType.name}"))
    }).getOrElse(JsError("Parsing of MusitSubEvent failed"))
  }

}

/**
 * Specifies a "part of" relationship on an implementation of MusitEvent.
 *
 * @tparam A the type of MusitEvent to expect in the "part of" relationship
 */
trait Parts[A <: MusitEvent] {
  val parts: Option[Seq[A]]
}

/**
 * Specifies a "motivates" relationship on an implementation of MusitEvent.
 *
 * @tparam A the type of MusitEvent to expect in the "motivates" relationship.
 */
trait Motivates[A <: MusitEvent] {
  val motivates: Option[A]
}