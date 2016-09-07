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

import enumeratum._
import play.api.libs.json._

import scala.reflect.ClassTag

case class EventTypeId(underlying: Int) extends AnyVal

object EventTypeId {

  implicit val reads: Reads[EventTypeId] = __.read[Int].map(EventTypeId.apply)
  implicit val writes: Writes[EventTypeId] = Writes { etid =>
    Json.toJson[Int](etid.underlying)
  }

}

/**
 * Represents an entry in the event type registry
 *
 * TODO: RE-THINK - This should possibly be modified slightly to avoid very
 * large pattern matches.
 */
sealed trait EventTypeEntry extends EnumEntry {
  val id: EventTypeId
}

/**
 * All events that need to be handled, in some form or other, should be
 * registered here. The registry acts as a form of type discriminator that
 * helps in identifying which event each instance of MusitEvent represents.
 */
// scalastyle:off number.of.methods
object EventTypeRegistry extends Enum[EventTypeEntry] {

  val values = findValues

  /**
   * Function that will try to locate an EventTypeEntry based on its EventTypeId
   *
   * @param id the EventTypeId to look for
   * @return An Option[EventTypeEntry] that may contain the located entry.
   */
  def fromId(id: EventTypeId): Option[EventTypeEntry] = values.find(_.id == id)

  /**
   * Unsafe retrieval of an EventTypeEntry that assumes the entry exists.
   * If it does not exist, the function will explode in your face with an
   * exception. Use with care!
   */
  def unsafeFromId(id: EventTypeId): EventTypeEntry = fromId(id).get

  /**
   * All events that appear at the root of an event structure should extend
   * the TopLevelEvent type.
   */
  sealed abstract class TopLevelEvent(
    val id: EventTypeId,
    override val entryName: String
  ) extends EventTypeEntry

  case object MoveEventType extends TopLevelEvent(EventTypeId(1), "Move")

  case object EnvRequirementEventType extends TopLevelEvent(EventTypeId(2), "EnvRequirement")

  case object ControlEventType extends TopLevelEvent(EventTypeId(3), "Control")

  case object ObservationEventType extends TopLevelEvent(EventTypeId(4), "Observation")

  /**
   * All events that typically appears below the root of an event structure
   * should be a SubEvent.
   *
   * Top-level MusitEvents are strongly typed by themselves. But the specific
   * MusitSubEvents are not. They have a specific type down to the level of
   * "ParentNameSubEvent" (e.g. `ObservationSubEvent`)
   *
   * Since MusitSubEvents typically have additional attributes to be complete,
   * these are kept in a Map[String, Any]. Because of this, they are not
   * validated automatically by Play. As a work around for this problem, all
   * SubEvents in this registry contain a `propTypes` map. Each entry key in
   * this map represents the valid name of the extra attribute. And each value
   * represents the expected data type for the MusitSubEvent property.
   */
  sealed abstract class SubEventType(
    val id: EventTypeId,
    override val entryName: String
  ) extends EventTypeEntry

  /**
   * Locate a SubEvent in the event registry based on it's name.
   *
   * @param n the String representation of the SubEvent name
   * @return an Option[SubEvent]
   */
  def subEventWithNameOption(n: String): Option[SubEventType] = {
    withNameInsensitiveOption(n).flatMap {
      case se: SubEventType => Some(se)
      case _ => None
    }
  }

  /**
   * Locate a specific sub-type `T` of SubEvent in the event registry based on
   * it's name.
   *
   * @param n String containing the name to look for
   * @param ct implicit `ClassTag` argument to ensure the type `T` isn't erased.
   * @tparam T The sub-type of `SubEventType` to look for.
   * @return An Option[T]
   */
  def typedWithNameOption[T <: SubEventType](n: String)(implicit ct: ClassTag[T]): Option[T] = {
    subEventWithNameOption(n).flatMap {
      case se: T => Some(se)
      case _ => None
    }
  }

  /**
   * Try to look up a TopLevelEventType with a given ID in the registry.
   *
   * @param id the ID of the SubEventType to look for
   * @return
   * @throws IllegalArgumentException if the SubEventType doesn't match T.
   */
  // FIXME: refactor to avoid the unsafeness
  def unsafeTopLevelFromId(id: EventTypeId): TopLevelEvent =
    unsafeFromId(id) match {
      case top: TopLevelEvent => top
      case bad =>
        // scalastyle:off
        throw new IllegalArgumentException(
          s"event type ${bad.entryName} is not a valid TopLevelEvent"
        ) // scalastyle:on
    }

  /**
   * Try to look up a SubEventType with a given ID in the registry.
   *
   * @param id the ID of the SubEventType to look for
   * @tparam T the type of SubEventType to look for
   * @return
   * @throws IllegalArgumentException if the SubEventType doesn't match T.
   */
  // FIXME: refactor to avoid the unsafeness
  def unsafeSubFromId[T <: SubEventType](id: EventTypeId)(implicit ct: ClassTag[T]): T =
    unsafeFromId(id) match {
      case sub: T => sub
      case bad =>
        val expStr = ct.toString
        val expected = expStr.substring(expStr.lastIndexOf("$") + 1)
        // scalastyle:off
        throw new IllegalArgumentException(
          s"event type ${bad.entryName} is not a valid $expected"
        ) // scalastyle:on
    }

  /**
   * CONTROL TYPES
   */
  sealed abstract class CtrlSubEventType(
    val evtId: EventTypeId,
    val eName: String
  ) extends SubEventType(evtId, eName)

  case object CtrlAlcoholType extends CtrlSubEventType(EventTypeId(5), "ControlAlcohol")

  case object CtrlCleaningType extends CtrlSubEventType(EventTypeId(6), "ControlCleaning")

  case object CtrlGasType extends CtrlSubEventType(EventTypeId(7), "ControlGas")

  case object CtrlHypoxicAirType extends CtrlSubEventType(EventTypeId(8), "ControlHypoxicAir")

  case object CtrlLightingType extends CtrlSubEventType(EventTypeId(9), "ControlLightingCondition")

  case object CtrlMoldType extends CtrlSubEventType(EventTypeId(10), "ControlMold")

  case object CtrlPestType extends CtrlSubEventType(EventTypeId(11), "ControlPest")

  case object CtrlHumidityType extends CtrlSubEventType(EventTypeId(12), "ControlRelativeHumidity")

  case object CtrlTemperatureType extends CtrlSubEventType(EventTypeId(13), "ControlTemperature")

  /**
   * OBSERVATION TYPES
   */
  sealed abstract class ObsSubEventType(
    val evtId: EventTypeId,
    val eName: String
  ) extends SubEventType(evtId, eName)

  case object ObsAlcoholType extends ObsSubEventType(EventTypeId(14), "ObservationAlcohol")

  case object ObsCleaningType extends ObsSubEventType(EventTypeId(15), "ObservationCleaning")

  case object ObsFireType extends ObsSubEventType(EventTypeId(16), "ObservationFireProtection")

  case object ObsGasType extends ObsSubEventType(EventTypeId(17), "ObservationGas")

  case object ObsHypoxicAirType extends ObsSubEventType(EventTypeId(18), "ObservationHypoxicAir")

  case object ObsLightingType extends ObsSubEventType(EventTypeId(19), "ObservationLightingCondition")

  case object ObsMoldType extends ObsSubEventType(EventTypeId(20), "ObservationMold")

  case object ObsPerimeterType extends ObsSubEventType(EventTypeId(21), "ObservationPerimeterSecurity")

  case object ObsHumidityType extends ObsSubEventType(EventTypeId(22), "ObservationRelativeHumidity")

  case object ObsPestType extends ObsSubEventType(EventTypeId(23), "ObservationPest")

  case object ObsTemperatureType extends ObsSubEventType(EventTypeId(24), "ObservationTemperature")

  case object ObsTheftType extends ObsSubEventType(EventTypeId(25), "ObservationTheftProtection")

  case object ObsWaterDamageType extends ObsSubEventType(EventTypeId(26), "ObservationWaterDamageAssessment")

}