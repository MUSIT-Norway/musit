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

import no.uio.musit.microservice.storagefacility.domain.event.Implicits._
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * Representation of an observation.
 *
 * @param baseEvent MusitEventBase shared attributes for all MusitEvents
 * @param eventType EventType that specifies the which event this is.
 * @param parts Optional collection of ObservationSubEvent parts.
 */
case class Observation(
  baseEvent: MusitEventBase,
  eventType: EventType,
  parts: Option[Seq[ObservationSubEvent]] = None
) extends MusitEvent with Parts[ObservationSubEvent]

object Observation {
  implicit val format: Format[Observation] = (
    __.format[MusitEventBase] and
    (__ \ "eventType").format[EventType] and
    (__ \ "parts").formatNullable[Seq[ObservationSubEvent]]
  )(Observation.apply, unlift(Observation.unapply))
}

/**
 * Representation of an observation sub-event
 *
 * @param baseEvent MusitEventBase shared attributes for all MusitEvents
 * @param eventType EventType that specifies the which event this is.
 * @param properties Additional properties associated with specified eventType.
 */
case class ObservationSubEvent(
  baseEvent: MusitEventBase,
  eventType: EventType,
  properties: Map[String, Any]
) extends MusitSubEvent

object ObservationSubEvent {

  implicit val reads: Reads[ObservationSubEvent] =
    MusitSubEvent.fromJson { (json, base, evtType, validProps) =>
      JsSuccess(ObservationSubEvent(base, evtType, validProps))
    }

  implicit val writes: Writes[ObservationSubEvent] = (
    __.write[MusitEventBase] and
    (__ \ "eventType").write[EventType] and
    (__ \ "properties").write[EventProps]
  )(unlift(ObservationSubEvent.unapply))

}