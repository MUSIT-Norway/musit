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

import java.util.NoSuchElementException

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
 * All events that need to be handled, in some form or other, should be
 * registered here. The registry acts as a form of type discriminator that
 * helps in identifying which event each instance of MusitEvent represents.
 */
// scalastyle:off number.of.methods
object EventTypeRegistry {

  trait EventTypeEntry { self: EnumEntry =>
    val id: EventTypeId
    val name: String = self.entryName
  }

  /**
   * All events that appear at the root of an event structure should extend
   * the TopLevelEvent type.
   */
  sealed abstract class TopLevelEvent(
    override val id: EventTypeId,
    override val entryName: String
  ) extends EnumEntry with EventTypeEntry

  /**
   * All events that typically appears below the root of an event structure
   * should be a SubEvent.
   */
  sealed abstract class SubEventType(
    override val id: EventTypeId,
    override val entryName: String
  ) extends EnumEntry with EventTypeEntry

  sealed abstract class CtrlSubEventType(
    val evtId: EventTypeId,
    val eName: String
  ) extends SubEventType(evtId, eName)

  sealed abstract class ObsSubEventType(
    val evtId: EventTypeId,
    val eName: String
  ) extends SubEventType(evtId, eName)

  object TopLevelEvents extends Enum[TopLevelEvent] {

    val values = findValues

    def fromId(id: EventTypeId): Option[TopLevelEvent] = values.find(_.id == id)

    def unsafeFromId(id: EventTypeId): TopLevelEvent = fromId(id).get

    case object MoveObjectType extends TopLevelEvent(EventTypeId(1), "MoveObject")

    case object MoveNodeType extends TopLevelEvent(EventTypeId(2), "MovePlace")

    case object EnvRequirementEventType extends TopLevelEvent(EventTypeId(3), "EnvRequirement")

    case object ControlEventType extends TopLevelEvent(EventTypeId(4), "Control")

    case object ObservationEventType extends TopLevelEvent(EventTypeId(5), "Observation")

  }

  object ControlSubEvents extends Enum[CtrlSubEventType] {

    val values = findValues

    def fromId(id: EventTypeId): Option[CtrlSubEventType] =
      values.find(_.id == id)

    def unsafeFromId(id: EventTypeId): CtrlSubEventType = fromId(id).get

    case object CtrlAlcoholType extends CtrlSubEventType(EventTypeId(6), "ControlAlcohol")

    case object CtrlCleaningType extends CtrlSubEventType(EventTypeId(7), "ControlCleaning")

    case object CtrlGasType extends CtrlSubEventType(EventTypeId(8), "ControlGas")

    case object CtrlHypoxicAirType extends CtrlSubEventType(EventTypeId(9), "ControlHypoxicAir")

    case object CtrlLightingType extends CtrlSubEventType(EventTypeId(10), "ControlLightingCondition")

    case object CtrlMoldType extends CtrlSubEventType(EventTypeId(11), "ControlMold")

    case object CtrlPestType extends CtrlSubEventType(EventTypeId(12), "ControlPest")

    case object CtrlHumidityType extends CtrlSubEventType(EventTypeId(13), "ControlRelativeHumidity")

    case object CtrlTemperatureType extends CtrlSubEventType(EventTypeId(14), "ControlTemperature")

  }

  object ObservationSubEvents extends Enum[ObsSubEventType] {
    val values = findValues

    def fromId(id: EventTypeId): Option[ObsSubEventType] =
      values.find(_.id == id)

    def unsafeFromId(id: EventTypeId): ObsSubEventType = fromId(id).get

    case object ObsAlcoholType extends ObsSubEventType(EventTypeId(15), "ObservationAlcohol")

    case object ObsCleaningType extends ObsSubEventType(EventTypeId(16), "ObservationCleaning")

    case object ObsFireType extends ObsSubEventType(EventTypeId(17), "ObservationFireProtection")

    case object ObsGasType extends ObsSubEventType(EventTypeId(18), "ObservationGas")

    case object ObsHypoxicAirType extends ObsSubEventType(EventTypeId(19), "ObservationHypoxicAir")

    case object ObsLightingType extends ObsSubEventType(EventTypeId(20), "ObservationLightingCondition")

    case object ObsMoldType extends ObsSubEventType(EventTypeId(21), "ObservationMold")

    case object ObsPerimeterType extends ObsSubEventType(EventTypeId(22), "ObservationPerimeterSecurity")

    case object ObsHumidityType extends ObsSubEventType(EventTypeId(23), "ObservationRelativeHumidity")

    case object ObsPestType extends ObsSubEventType(EventTypeId(24), "ObservationPest")

    case object ObsTemperatureType extends ObsSubEventType(EventTypeId(25), "ObservationTemperature")

    case object ObsTheftType extends ObsSubEventType(EventTypeId(26), "ObservationTheftProtection")

    case object ObsWaterDamageType extends ObsSubEventType(EventTypeId(27), "ObservationWaterDamageAssessment")

  }

  def unsafeFromId(id: EventTypeId): EventTypeEntry = {
    ControlSubEvents.fromId(id)
      .orElse(ObservationSubEvents.fromId(id))
      .orElse(TopLevelEvents.fromId(id))
      .get
  }

  def withNameInsensitiveOption(name: String): Option[EventTypeEntry] = {
    TopLevelEvents.withNameInsensitiveOption(name)
      .orElse(ControlSubEvents.withNameInsensitiveOption(name))
      .orElse(ObservationSubEvents.withNameInsensitiveOption(name))
  }

  def withNameInsensitive(name: String): EventTypeEntry = {
    withNameInsensitiveOption(name).get
  }

  /**
   * Locate a SubEvent in the event registry based on it's name.
   *
   * @param n the String representation of the SubEvent name
   * @return an Option[SubEvent]
   */
  def subEventWithNameOption(n: String): Option[SubEventType] = {
    ControlSubEvents.withNameInsensitiveOption(n)
      .orElse(ObservationSubEvents.withNameInsensitiveOption(n))
  }

  /**
   * Locate a specific sub-type `T` of SubEvent in the event registry based on
   * it's name.
   *
   * @param n  String containing the name to look for
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
   * Try to look up a SubEventType with a given ID in the registry.
   *
   * @param id the ID of the SubEventType to look for
   * @tparam T the type of SubEventType to look for
   * @return
   * @throws IllegalArgumentException if the SubEventType doesn't match T.
   * @throws NoSuchElementException   if no SubEventType was found at all.
   */
  def unsafeSubFromId[T <: SubEventType](id: EventTypeId)(implicit ct: ClassTag[T]): T = {
    ControlSubEvents.fromId(id).orElse(ObservationSubEvents.fromId(id)).map {
      case sub: T => sub
      case bad =>
        val expStr = ct.toString
        val expected = expStr.substring(expStr.lastIndexOf("$") + 1)
        // scalastyle:off
        throw new IllegalArgumentException(
          s"event type ${bad.entryName} is not a valid $expected"
        ) // scalastyle:on
    }.get
  }

}