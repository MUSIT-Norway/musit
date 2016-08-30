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

import no.uio.musit.microservice.storagefacility.domain.event.EventTypeRegistry._
import no.uio.musit.microservice.storagefacility.domain.event._
import no.uio.musit.microservice.storagefacility.domain.event.control._
import no.uio.musit.microservice.storagefacility.domain.event.observation._
import no.uio.musit.microservice.storagefacility.domain.{ FromToDouble, LifeCycle }

object DtoConverters {

  /**
   * Converts a Boolean value to a Long.
   */
  implicit def boolToOptLong(b: Boolean): Option[Long] = Option(if (b) 1 else 0)

  /**
   * Converts a Long value to a Boolean.
   */
  implicit def maybeLongToBool(mi: Option[Long]): Boolean =
    mi.exists(i => if (i == 1) true else false)

  /**
   * Helper function to generate a base Dto that is shared across all event
   * types.
   */
  private[this] def toDto[T <: MusitEvent](
    sub: T,
    maybeStr: Option[String] = None,
    maybeLong: Option[Long] = None,
    maybeDouble: Option[Double] = None
  )(relatedEvents: Unit => Seq[RelatedEvents]): EventDto = {
    EventDto(
      id = sub.baseEvent.id,
      links = sub.baseEvent.links,
      eventType = sub.eventType.registeredEventId,
      note = sub.baseEvent.note,
      relatedSubEvents = relatedEvents(),
      partOf = sub.baseEvent.partOf,
      valueLong = maybeLong,
      valueString = maybeStr,
      valueDouble = maybeDouble
    )
  }

  /**
   * Helper function to generate a base Dto that is shared across all event
   * types. Differes from `toEventDto` in that it will initialise the
   * `releatedEvents` property with an empty collection.
   */
  private[this] def toDtoNoChildren[T <: MusitEvent](
    sub: T,
    maybeStr: Option[String] = None,
    maybeLong: Option[Long] = None,
    maybeDouble: Option[Double] = None
  ): EventDto = {
    toDto(sub, maybeStr, maybeLong, maybeDouble)(_ => Seq.empty)
  }

  /**
   * Converters for the LifeCycle set of types.
   */
  object LifeCyleConverters {

    def lifecycleToDto(lc: LifeCycle): LifecycleDto = {
      LifecycleDto(
        eventId = None,
        stage = lc.stage,
        number = lc.number
      )
    }

    def lifecycleFromDto(lcd: LifecycleDto): LifeCycle = {
      LifeCycle(
        stage = lcd.stage,
        number = lcd.number
      )
    }
  }

  /**
   * Converters for the Control specific types.
   */
  object CtrlConverters {

    def controlToDto(ctrl: Control): EventDto = {
      toDto(ctrl) { _ =>
        // TODO: Clarify what the nested collections in for related events mean.
        Seq(
          RelatedEvents(
            relation = EventRelations.PartsOfRelation,
            events = ctrl.parts.map { c =>
              c.map(controlSubEventToDto)
            }.getOrElse(Seq.empty)
          )
        )
      }
    }

    def controlFromDto(dto: EventDto): Control = {
      val p = {
        dto.relatedSubEvents.headOption.map { re =>
          re.events.map(e => controlSubEventFromDto(e.asInstanceOf[EventDto]))
        }
      }
      Control(
        baseEvent = MusitEventBase(
          id = dto.id,
          links = dto.links,
          note = dto.note,
          partOf = dto.partOf
        ),
        eventType = EventType.fromEventTypeId(dto.eventType),
        parts = p
      )
    }

    def controlSubEventToDto(subCtrl: ControlSubEvent): Dto = {
      toDto(subCtrl, maybeLong = subCtrl.ok) { _ =>
        subCtrl.motivates.map { ose =>
          Seq(
            RelatedEvents(
              relation = EventRelations.MotivatesRelation,
              events = subCtrl.motivates.map { m =>
                Seq(ObsConverters.observationSubEventToDto(m))
              }.getOrElse(Seq.empty)
            )
          )
        }.getOrElse(Seq.empty)
      }
    }

    // scalastyle:off method.length
    def controlSubEventFromDto(dto: EventDto): ControlSubEvent = {
      val base = MusitEventBase(dto.id, dto.links, dto.note, dto.partOf)
      val registeredEvent = EventTypeRegistry.unsafeFromId(dto.eventType)
      val evtType = EventType(registeredEvent.entryName)
      val ok = dto.valueLong
      val motivates = {
        dto.relatedSubEvents.headOption.flatMap { re =>
          re.events.headOption.map(ose =>
            ObsConverters.observationSubEventFromDto(ose))
        }
      }

      registeredEvent match {
        case CtrlAlcoholType =>
          ControlAlcohol(
            baseEvent = base,
            eventType = evtType,
            ok = ok,
            motivates = motivates.map(_.asInstanceOf[ObservationAlcohol])
          )

        case CtrlCleaningType =>
          ControlCleaning(
            baseEvent = base,
            eventType = evtType,
            ok = ok,
            motivates = motivates.map(_.asInstanceOf[ObservationCleaning])
          )

        case CtrlGasType =>
          ControlGas(
            baseEvent = base,
            eventType = evtType,
            ok = ok,
            motivates = motivates.map(_.asInstanceOf[ObservationGas])
          )

        case CtrlHypoxicAirType =>
          ControlHypoxicAir(
            baseEvent = base,
            eventType = evtType,
            ok = ok,
            motivates = motivates.map(_.asInstanceOf[ObservationHypoxicAir])
          )

        case CtrlLightingType =>
          ControlLightingCondition(
            baseEvent = base,
            eventType = evtType,
            ok = ok,
            motivates = motivates.map(_.asInstanceOf[ObservationLightingCondition])
          )

        case CtrlMoldType =>
          ControlMold(
            baseEvent = base,
            eventType = evtType,
            ok = ok,
            motivates = motivates.map(_.asInstanceOf[ObservationMold])
          )

        case CtrlPestType =>
          ControlPest(
            baseEvent = base,
            eventType = evtType,
            ok = ok,
            motivates = motivates.map(_.asInstanceOf[ObservationPest])
          )

        case CtrlHumidityType =>
          ControlRelativeHumidity(
            baseEvent = base,
            eventType = evtType,
            ok = ok,
            motivates = motivates.map(_.asInstanceOf[ObservationRelativeHumidity])
          )

        case CtrlTemperatureType =>
          ControlTemperature(
            baseEvent = base,
            eventType = evtType,
            ok = ok,
            motivates = motivates.map(_.asInstanceOf[ObservationTemperature])
          )
      }
    } // scalastyle:on method.length
  }

  /**
   * Converters for the Observation specific types.
   */
  object ObsConverters {

    def observationToDto(obs: Observation): Dto = {
      toDto(obs) { _ =>
        // TODO: Clarify what the nested collections in for related events mean.
        Seq(
          RelatedEvents(
            relation = EventRelations.PartsOfRelation,
            events = obs.parts.map { o =>
              o.map(ose => observationSubEventToDto(ose))
            }.getOrElse(Seq.empty)
          )
        )
      }
    }

    def observationFromDto(dto: EventDto): Observation = {
      val p = {
        dto.relatedSubEvents.headOption.map { re =>
          re.events.map(e => observationSubEventFromDto(e))
        }
      }
      Observation(
        baseEvent = MusitEventBase(
          id = dto.id,
          links = dto.links,
          note = dto.note,
          partOf = dto.partOf
        ),
        eventType = EventType.fromEventTypeId(dto.eventType),
        parts = p
      )
    }

    // scalastyle:off cyclomatic.complexity method.length
    def observationSubEventToDto(subObs: ObservationSubEvent): Dto = {
      subObs match {
        case obs: ObservationFromTo =>
          ExtendedDto(
            baseEvent = toDtoNoChildren(obs),
            extension = ObservationFromToDto(
              id = None,
              from = obs.range.from,
              to = obs.range.to
            )
          )

        case obs: ObservationLightingCondition =>
          toDtoNoChildren(obs, maybeStr = obs.lightingCondition)

        case obs: ObservationCleaning =>
          toDtoNoChildren(obs, maybeStr = obs.cleaning)

        case obs: ObservationGas =>
          toDtoNoChildren(obs, maybeStr = obs.gas)

        case obs: ObservationMold =>
          toDtoNoChildren(obs, maybeStr = obs.mold)

        case obs: ObservationTheftProtection =>
          toDtoNoChildren(obs, maybeStr = obs.theftProtection)

        case obs: ObservationFireProtection =>
          toDtoNoChildren(obs, maybeStr = obs.fireProtection)

        case obs: ObservationWaterDamageAssessment =>
          toDtoNoChildren(obs, maybeStr = obs.waterDamageAssessment)

        case obs: ObservationPest =>
          ExtendedDto(
            baseEvent = toDtoNoChildren(obs, maybeStr = obs.identification),
            extension = ObservationPestDto(
              lifeCycles = obs.lifecycles.map(LifeCyleConverters.lifecycleToDto)
            )
          )

        case obs: ObservationAlcohol =>
          toDtoNoChildren(
            obs,
            maybeStr = obs.condition,
            maybeDouble = obs.volume
          )
      }
    } // scalastyle:on cyclomatic.complexity method.length

    def observationSubEventFromDto(dto: Dto): ObservationSubEvent = {
      dto match {
        case eventDto: EventDto =>
          obsSubEventFromBasicDto(eventDto)

        case extended: ExtendedDto =>
          obsSubEventFromExtendedDto(extended)

        case unhandled =>
          // It shouldn't be possible, but this is unfortunate :-(
          // This is needed due to the weak typing of the DTO's for Event.
          throw new IllegalStateException(
            s"Encountered unexpected Dto type ${unhandled.getClass} when" +
              s"mapping to an ObservationSubEvent"
          )
      }
    }

    /**
     * Specifically handles dto mapping for observations that does not have
     * custom properties in a separate table in the DB.
     */
    // scalastyle:off cyclomatic.complexity
    def obsSubEventFromBasicDto(dto: EventDto): ObservationSubEvent = {
      val base = MusitEventBase(
        id = dto.id,
        links = dto.links,
        note = dto.note,
        partOf = dto.partOf
      )
      val registeredEvent = EventTypeRegistry.unsafeFromId(dto.eventType)
      val evtType = EventType(registeredEvent.entryName)

      registeredEvent match {
        case ObsAlcoholType =>
          ObservationAlcohol(base, evtType, dto.valueString, dto.valueDouble)

        case ObsCleaningType =>
          ObservationCleaning(base, evtType, dto.valueString)

        case ObsFireType =>
          ObservationFireProtection(base, evtType, dto.valueString)

        case ObsGasType =>
          ObservationGas(base, evtType, dto.valueString)

        case ObsLightingType =>
          ObservationLightingCondition(base, evtType, dto.valueString)

        case ObsMoldType =>
          ObservationMold(base, evtType, dto.valueString)

        case ObsPerimeterType =>
          ObservationPerimeterSecurity(base, evtType, dto.valueString)

        case ObsTheftType =>
          ObservationTheftProtection(base, evtType, dto.valueString)

        case ObsWaterDamageType =>
          ObservationWaterDamageAssessment(base, evtType, dto.valueString)

        case unhandled =>
          // TODO: Complete me
          ???
      }
    } // scalastyle:on cyclomatic.complexity

    /**
     * Specific handling for observations that have properties in more than one
     * database table.
     */
    def obsSubEventFromExtendedDto(dto: ExtendedDto): ObservationSubEvent = {
      val base = MusitEventBase(
        id = dto.baseEvent.id,
        links = dto.baseEvent.links,
        note = dto.baseEvent.note,
        partOf = dto.baseEvent.partOf
      )
      val registeredEvent = EventTypeRegistry.unsafeFromId(dto.baseEvent.eventType)
      val evtType = EventType(registeredEvent.entryName)

      registeredEvent match {
        case ObsPestType =>
          val tmpDto = dto.extension.asInstanceOf[ObservationPestDto]
          val lc = tmpDto.lifeCycles.map(LifeCyleConverters.lifecycleFromDto)

          ObservationPest(
            baseEvent = base,
            eventType = evtType,
            identification = dto.baseEvent.valueString,
            lifecycles = lc
          )

        case ObsHumidityType =>
          val tmpDt = dto.extension.asInstanceOf[ObservationFromToDto]
          ObservationRelativeHumidity(
            baseEvent = base,
            eventType = evtType,
            range = FromToDouble(tmpDt.from, tmpDt.to)
          )

        case ObsHypoxicAirType =>
          val tmpDt = dto.extension.asInstanceOf[ObservationFromToDto]
          ObservationHypoxicAir(
            baseEvent = base,
            eventType = evtType,
            range = FromToDouble(tmpDt.from, tmpDt.to)
          )

        case ObsTemperatureType =>
          val tmpDt = dto.extension.asInstanceOf[ObservationFromToDto]
          ObservationTemperature(
            baseEvent = base,
            eventType = evtType,
            range = FromToDouble(tmpDt.from, tmpDt.to)
          )

        case unhandled =>
          // TODO: Complete me
          ???
      }
    }

  }

}
