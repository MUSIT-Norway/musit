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
  private[this] def toBaseDto[T <: MusitEvent](
    sub: T,
    maybeStr: Option[String] = None,
    maybeLong: Option[Long] = None,
    maybeDouble: Option[Double] = None,
    relEvents: Seq[RelatedEvents] = Seq.empty
  ): BaseEventDto = {
    BaseEventDto(
      id = sub.baseEvent.id,
      links = sub.baseEvent.links,
      eventTypeId = sub.eventType.registeredEventId,
      note = sub.baseEvent.note,
      relatedSubEvents = relEvents,
      partOf = sub.baseEvent.partOf,
      valueLong = maybeLong,
      valueString = maybeStr,
      valueDouble = maybeDouble
    )
  }

  /**
   * Helper function to generate a base Dto that is shared across all event
   * types. Differes from the above function in that it will initialise the
   * `releatedEvents` property with an empty collection.
   */
  private[this] def toBaseDtoNoChildren[T <: MusitEvent](
    sub: T,
    maybeStr: Option[String] = None,
    maybeLong: Option[Long] = None,
    maybeDouble: Option[Double] = None
  ): BaseEventDto = {
    toBaseDto(sub, maybeStr, maybeLong, maybeDouble)
  }

  private[this] def toExtendedDto[A <: MusitEvent, B <: DtoExtension](
    sub: A,
    maybeStr: Option[String] = None,
    maybeLong: Option[Long] = None,
    maybeDouble: Option[Double] = None,
    ext: B
  ): ExtendedDto = {
    ExtendedDto(
      id = sub.baseEvent.id,
      links = sub.baseEvent.links,
      eventTypeId = sub.eventType.registeredEventId,
      note = sub.baseEvent.note,
      relatedSubEvents = Seq.empty[RelatedEvents],
      partOf = sub.baseEvent.partOf,
      valueLong = maybeLong,
      valueString = maybeStr,
      valueDouble = maybeDouble,
      extension = ext
    )
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

    def controlToDto(ctrl: Control): BaseEventDto = {
      val relEvt = Seq(
        RelatedEvents(
          relation = EventRelations.PartsOfRelation,
          events = ctrl.parts.map { c =>
            c.map(controlSubEventToDto)
          }.getOrElse(Seq.empty)
        )
      )
      toBaseDto(sub = ctrl, relEvents = relEvt)
    }

    def controlFromDto(dto: BaseEventDto): Control = {
      val p = {
        dto.relatedSubEvents.headOption.map { re =>
          re.events.map(e => controlSubEventFromDto(e.asInstanceOf[BaseEventDto]))
        }
      }
      Control(
        baseEvent = MusitEventBase(
          id = dto.id,
          links = dto.links,
          note = dto.note,
          partOf = dto.partOf
        ),
        eventType = EventType.fromEventTypeId(dto.eventTypeId),
        parts = p
      )
    }

    def controlSubEventToDto(subCtrl: ControlSubEvent): Dto = {
      val relations = subCtrl.motivates.map { ose =>
        Seq(
          RelatedEvents(
            relation = EventRelations.MotivatesRelation,
            events = subCtrl.motivates.map { m =>
              Seq(ObsConverters.observationSubEventToDto(m))
            }.getOrElse(Seq.empty)
          )
        )
      }.getOrElse(Seq.empty)

      toBaseDto(subCtrl, maybeLong = subCtrl.ok, relEvents = relations)
    }

    // scalastyle:off method.length
    def controlSubEventFromDto(dto: BaseEventDto): ControlSubEvent = {
      val base = MusitEventBase(dto.id, dto.links, dto.note, dto.partOf)
      val registeredEvent = EventTypeRegistry.unsafeFromId(dto.eventTypeId)
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
      val relEvt = Seq(
        RelatedEvents(
          relation = EventRelations.PartsOfRelation,
          events = obs.parts.map { o =>
            o.map(ose => observationSubEventToDto(ose))
          }.getOrElse(Seq.empty)
        )
      )
      toBaseDto(sub = obs, relEvents = relEvt)
    }

    def observationFromDto(dto: BaseEventDto): Observation = {
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
        eventType = EventType.fromEventTypeId(dto.eventTypeId),
        parts = p
      )
    }

    // scalastyle:off cyclomatic.complexity method.length
    def observationSubEventToDto(subObs: ObservationSubEvent): Dto = {
      subObs match {
        case obs: ObservationFromTo =>
          toExtendedDto(
            sub = obs,
            ext = ObservationFromToDto(
              id = None,
              from = obs.range.from,
              to = obs.range.to
            )
          )

        case obs: ObservationLightingCondition =>
          toBaseDtoNoChildren(obs, maybeStr = obs.lightingCondition)

        case obs: ObservationCleaning =>
          toBaseDtoNoChildren(obs, maybeStr = obs.cleaning)

        case obs: ObservationGas =>
          toBaseDtoNoChildren(obs, maybeStr = obs.gas)

        case obs: ObservationMold =>
          toBaseDtoNoChildren(obs, maybeStr = obs.mold)

        case obs: ObservationTheftProtection =>
          toBaseDtoNoChildren(obs, maybeStr = obs.theftProtection)

        case obs: ObservationFireProtection =>
          toBaseDtoNoChildren(obs, maybeStr = obs.fireProtection)

        case obs: ObservationWaterDamageAssessment =>
          toBaseDtoNoChildren(obs, maybeStr = obs.waterDamageAssessment)

        case obs: ObservationPest =>
          toExtendedDto(
            sub = obs,
            maybeStr = obs.identification,
            ext = ObservationPestDto(
              lifeCycles = obs.lifecycles.map(LifeCyleConverters.lifecycleToDto)
            )
          )

        case obs: ObservationAlcohol =>
          toBaseDtoNoChildren(
            obs,
            maybeStr = obs.condition,
            maybeDouble = obs.volume
          )
      }
    } // scalastyle:on cyclomatic.complexity method.length

    def observationSubEventFromDto(dto: Dto): ObservationSubEvent = {
      dto match {
        case eventDto: BaseEventDto =>
          obsSubEventFromBasicDto(eventDto)

        case extended: ExtendedDto =>
          obsSubEventFromExtendedDto(extended)
      }
    }

    /**
     * Specifically handles dto mapping for observations that does not have
     * custom properties in a separate table in the DB.
     */
    // scalastyle:off cyclomatic.complexity
    def obsSubEventFromBasicDto(dto: BaseEventDto): ObservationSubEvent = {
      val base = MusitEventBase(
        id = dto.id,
        links = dto.links,
        note = dto.note,
        partOf = dto.partOf
      )
      val registeredEvent = EventTypeRegistry.unsafeFromId(dto.eventTypeId)
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
        id = dto.id,
        links = dto.links,
        note = dto.note,
        partOf = dto.partOf
      )
      val registeredEvent = EventTypeRegistry.unsafeFromId(dto.eventTypeId)
      val evtType = EventType(registeredEvent.entryName)

      registeredEvent match {
        case ObsPestType =>
          val tmpDto = dto.extension.asInstanceOf[ObservationPestDto]
          val lc = tmpDto.lifeCycles.map(LifeCyleConverters.lifecycleFromDto)

          ObservationPest(
            baseEvent = base,
            eventType = evtType,
            identification = dto.valueString,
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
