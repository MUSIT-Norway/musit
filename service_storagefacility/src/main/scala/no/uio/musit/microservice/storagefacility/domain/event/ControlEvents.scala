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
 * Representation of a control.
 *
 * @param baseEvent MusitEventBase shared attributes for all MusitEvents
 * @param eventType EventType that specifies the which event this is.
 * @param parts Optional collection of ControlSubEvent parts.
 */
case class Control(
  baseEvent: MusitEventBase,
  eventType: EventType,
  parts: Option[Seq[ControlSubEvent]] = None
) extends MusitEvent with Parts[ControlSubEvent]

object Control {
  implicit val format: Format[Control] = (
    __.format[MusitEventBase] and
    (__ \ "eventType").format[EventType] and
    (__ \ "parts").formatNullable[Seq[ControlSubEvent]]
  )(Control.apply, unlift(Control.unapply))

}

/**
 * Representation of a control sub-event
 *
 * @param baseEvent MusitEventBase shared attributes for all MusitEvents
 * @param eventType EventType that specifies the which event this is.
 * @param properties Additional properties associated with specified eventType.
 * @param motivates In case of control not OK, an ObservationSubEvent can exist.
 */
case class ControlSubEvent(
  baseEvent: MusitEventBase,
  eventType: EventType,
  properties: EventProps,
  motivates: Option[ObservationSubEvent]
) extends MusitSubEvent with Motivates[ObservationSubEvent]

object ControlSubEvent {

  implicit val reads: Reads[ControlSubEvent] =
    MusitSubEvent.fromJson { (json, base, evtType, validProps) =>
      val maybeMotivates = (json \ "motivates").validateOpt[ObservationSubEvent]
      maybeMotivates.map { mobs =>
        ControlSubEvent(base, evtType, validProps, mobs)
      }
    }

  implicit val writes: Writes[ControlSubEvent] = (
    __.write[MusitEventBase] and
    (__ \ "eventType").write[EventType] and
    (__ \ "properties").write[EventProps] and
    (__ \ "motivates").writeNullable[ObservationSubEvent]
  )(unlift(ControlSubEvent.unapply))

}