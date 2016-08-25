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
import no.uio.musit.microservice.storagefacility.domain.event.PropTypes.PropValidation._
import no.uio.musit.microservice.storagefacility.domain.event.PropTypes._

/**
 * Represents an entry in the event type registry
 */
sealed trait EventTypeEntry extends EnumEntry {
  val id: Int
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
   * All events that appear at the root of an event structure should extend
   * the TopLevelEvent type.
   */
  sealed abstract class TopLevelEvent(
    val id: Int,
    override val entryName: String
  ) extends EventTypeEntry

  case object MoveEventType extends TopLevelEvent(1, "Move")

  case object EnvRequirementEventType extends TopLevelEvent(1, "EnvRequirement")

  case object ControlEventType extends TopLevelEvent(3, "Control")

  case object ObservationEventType extends TopLevelEvent(4, "Observation")

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
  sealed abstract class SubEvent(
      val id: Int,
      override val entryName: String
  ) extends EventTypeEntry {
    val propTypes: Map[String, PropType[_]]
  }

  /**
   * Companion object for SubEvent.
   */
  object SubEvent {

    /**
     * Perform a diff between two instances of Map[String, Any], "a" and "b".
     *
     * @param a The map to diff against
     * @param b The map to diff with
     *
     * @return A Set[String] containing the keys that were different.
     */
    private[this] def diff(a: EventProps, b: EventProps): Set[String] =
      a.keySet.diff(b.keySet)

    /**
     * Validate the type of value in the tuple `kv`. The value is checked
     * against the PropertyType registered with the sub-event type in the
     * registry. The validated key value pair is returned if the value is OK.
     *
     * FIXME: Too much OOP. The algorithm shouldn't need the m.
     */
    private[this] def validateType(
      se: SubEvent,
      m: EventProps,
      kv: (String, Any)
    ): Option[(String, Any)] = {
      se.propTypes.get(kv._1).flatMap { typeValidator =>
        typeValidator.transform(kv._2).map { valid =>
          kv._1 -> valid
        }
      }
    }

    /**
     * This function folds over the `props` to validate the value for each key
     * is of the same type as defined for the key in the SubEvent.propTypes.
     * If a value doesn't match the expected type, the key/value pair is removed
     * from the map. Only _valid_ entries are kept.
     * The function will return an Option of a Tuple containing a Map of the
     * valid entries, and a Set with the keys that were invalid.
     *
     * @param se The SubEvent to use for validation
     * @param props The EventProps (or Map[String, Any]) to validate.
     *
     * @return Option[(EventProps, Set[String])]
     */
    private[this] def transformValid(
      se: SubEvent, props: EventProps
    ): Option[(EventProps, Set[String])] = {
      val transformed = props.foldLeft(Map.empty[String, Any]) { (pm, kv) =>
        validateType(se, pm, kv) match {
          case Some(valid) => pm + valid
          case None => pm
        }
      }
      val tpeErr = diff(props, transformed)

      if (transformed.isEmpty) None
      else Some(transformed, tpeErr)
    }

    /**
     * Validates the key/value pairs in the `props` argument with the
     * name and type defined in the SubEvent parameter.
     */
    def validateProps(se: SubEvent, props: EventProps): ValidatedProps = {
      val missing = diff(se.propTypes, props)
      val invalid = diff(props, se.propTypes)

      // Identify valid and invalid properties.
      val maybeValidated: Option[(EventProps, Set[String])] = {
        if (missing.isEmpty && invalid.isEmpty) {
          transformValid(se, props)
        } else {
          None
        }
      }

      maybeValidated.map {
        case (v, tpeErr) if tpeErr.nonEmpty => InvalidProps(missing, invalid, tpeErr)
        case (v, tpeErr) if tpeErr.isEmpty => ValidProps(v)
      }.getOrElse {
        InvalidProps(missing, invalid, Set.empty)
      }
    }
  }

  /**
   * Locate a SubEvent in the event registry based on it's name.
   *
   * @param n the String representation of the SubEvent name
   * @return an Option[SubEvent]
   */
  def subEventWithNameOption(n: String): Option[SubEvent] = {
    withNameInsensitiveOption(n).flatMap {
      case se: SubEvent => Some(se)
      case _ => None
    }
  }

  /* CONTROL TYPES */

  abstract class CtrlSubEvent(
    val evtId: Int,
    val eName: String
  ) extends SubEvent(evtId, eName) {
    override val propTypes: Map[String, PropType[_]] =
      Map("ok" -> BooleanPropType)
  }

  case object ControlAlcohol extends CtrlSubEvent(5, "ControlAlcohol")

  case object ControlCleaning extends CtrlSubEvent(6, "ControlCleaning")

  case object ControlGas extends CtrlSubEvent(7, "ControlGas")

  case object ControlHypoxicAir extends CtrlSubEvent(8, "ControlHypoxicAir")

  case object ControlLightingCondition extends CtrlSubEvent(9, "ControlLightingCondition")

  case object ControlMold extends CtrlSubEvent(10, "ControlMold")

  case object ControlPest extends CtrlSubEvent(11, "ControlPest")

  case object ControlRelativeHumidity extends CtrlSubEvent(12, "ControlRelativeHumidity")

  case object ControlTemperature extends CtrlSubEvent(13, "ControlTemperature")



  /* OBSERVATION TYPES */
  // TODO: Consider possibilities for separating types to separate files and enums

  case object ObservationAlcohol extends SubEvent(14, "ObservationAlcohol") {
    override val propTypes: Map[String, PropType[_]] =
      Map(
        "condition" -> StringPropType,
        "volume" -> DoublePropType
      )
  }

  case object ObservationCleaning extends SubEvent(15, "ObservationCleaning") {
    override val propTypes: Map[String, PropType[_]] =
      Map(
        "cleaning" -> StringPropType
      )
  }

  case object ObservationFireProtection extends SubEvent(16, "ObservationFireProtection") {
    override val propTypes: Map[String, PropType[_]] =
      Map(
        "fireProtection" -> StringPropType
      )
  }

  case object ObservationGas extends SubEvent(17, "ObservationGas") {
    override val propTypes: Map[String, PropType[_]] =
      Map(
        "gas" -> StringPropType
      )
  }

  case object ObservationHypoxicAir extends SubEvent(18, "ObservationHypoxicAir") {
    override val propTypes: Map[String, PropType[_]] =
      Map(
        "from" -> DoublePropType,
        "to" -> DoublePropType
      )
  }

  case object ObservationLightingCondition extends SubEvent(19, "ObservationLightingCondition") {
    override val propTypes: Map[String, PropType[_]] =
      Map(
        "lightingCondition" -> StringPropType
      )
  }

  case object ObservationMold extends SubEvent(20, "ObservationMold") {
    override val propTypes: Map[String, PropType[_]] =
      Map(
        "mold" -> StringPropType
      )
  }

  case object ObservationPerimeterSecurity extends SubEvent(21, "ObservationPerimeterSecurity") {
    override val propTypes: Map[String, PropType[_]] =
      Map(
        "perimeterSecurity" -> StringPropType
      )
  }

  case object ObservationRelativeHumidity extends SubEvent(22, "ObservationRelativeHumidity") {
    override val propTypes: Map[String, PropType[_]] =
      Map(
        "from" -> DoublePropType,
        "to" -> DoublePropType
      )
  }

  case object ObservationPest extends SubEvent(23, "ObservationPest") {
    override val propTypes: Map[String, PropType[_]] =
      Map(
        "identification" -> StringPropType
        // TODO: This event actually needs to have a Seq[Lifecycle] attribute :-(
      )
  }

  case object ObservationTemperature extends SubEvent(24, "ObservationTemperature") {
    override val propTypes: Map[String, PropType[_]] =
      Map(
        "from" -> DoublePropType,
        "to" -> DoublePropType
      )
  }

  case object ObservationTheftProtection extends SubEvent(25, "ObservationTheftProtection") {
    override val propTypes: Map[String, PropType[_]] =
      Map(
        "theftProtection" -> StringPropType
      )
  }

  case object ObservationWaterDamageAssessment extends SubEvent(26, "ObservationWaterDamageAssessment") {
    override val propTypes: Map[String, PropType[_]] =
      Map(
        "waterDamageAssessment" -> StringPropType
      )
  }

}