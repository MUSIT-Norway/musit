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

import no.uio.musit.microservice.storagefacility.domain.datetime.Implicits._
import no.uio.musit.microservice.storagefacility.domain.event.EventTypeRegistry._
import no.uio.musit.microservice.storagefacility.domain.event._
import no.uio.musit.microservice.storagefacility.domain.event.control._
import no.uio.musit.microservice.storagefacility.domain.event.envreq.EnvRequirement
import no.uio.musit.microservice.storagefacility.domain.event.move.{ Move, MoveNode, MoveObject }
import no.uio.musit.microservice.storagefacility.domain.event.observation._
import no.uio.musit.microservice.storagefacility.domain.{ FromToDouble, Interval, LifeCycle }

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
   * Converts a pair of values to an Interval.
   */
  def toInterval[A](
    base: Option[A],
    tolerance: Option[A]
  ): Option[Interval[A]] = base.map(b => Interval[A](b, tolerance))

  private[this] def baseFromDto(dto: Dto): MusitEventBase = {
    MusitEventBase(
      id = dto.id,
      doneDate = dto.eventDate,
      doneBy = dto.relatedActors.map(EventRoleActor.toActorRole).headOption,
      affectedThing = dto.relatedObjects.map(EventRoleObject.toObjectRole).headOption,
      note = dto.note,
      partOf = dto.partOf,
      registeredBy = dto.registeredBy,
      registeredDate = dto.registeredDate
    )
  }

  /**
   * Helper function to generate a base Dto that is shared across all event
   * types.
   */
  private[this] def toBaseDto[T <: MusitEvent](
    sub: T,
    maybeStr: Option[String] = None,
    maybeLong: Option[Long] = None,
    maybeDouble: Option[Double] = None,
    relEvents: Seq[RelatedEvents] = Seq.empty,
    relPlaces: Seq[PlaceRole] = Seq.empty
  ): BaseEventDto = {
    BaseEventDto(
      id = sub.baseEvent.id,
      eventTypeId = sub.eventType.registeredEventId,
      eventDate = sub.baseEvent.doneDate,
      // For the following related* fields, we do not yet know the ID to set.
      // So they are initialised with the eventId property to None.
      relatedActors = sub.baseEvent.doneBy.map(ar => EventRoleActor.toEventRoleActor(ar)).toSeq,
      relatedObjects = sub.baseEvent.affectedThing.map(or => EventRoleObject.toEventRoleObject(or)).toSeq,
      relatedPlaces = relPlaces.map(pr => EventRolePlace.toEventRolePlace(pr)),
      note = sub.baseEvent.note,
      relatedSubEvents = relEvents,
      partOf = sub.baseEvent.partOf,
      valueLong = maybeLong,
      valueString = maybeStr,
      valueDouble = maybeDouble,
      registeredBy = sub.baseEvent.registeredBy,
      registeredDate = sub.baseEvent.registeredDate
    )
  }

  /**
   * Helper function to generate a base Dto that is shared across all event
   * types. Differs from the above function in that it will initialise the
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
    relPlaces: Seq[PlaceRole] = Seq.empty,
    ext: B
  ): ExtendedDto = {
    ExtendedDto(
      id = sub.baseEvent.id,
      eventTypeId = sub.eventType.registeredEventId,
      eventDate = sub.baseEvent.doneDate,
      // For the following related* fields, we do not yet know the ID to set.
      // So they are initialised with the eventId property to None.
      relatedActors = sub.baseEvent.doneBy.map(ar => EventRoleActor.toEventRoleActor(ar)).toSeq,
      relatedObjects = sub.baseEvent.affectedThing.map(or => EventRoleObject.toEventRoleObject(or)).toSeq,
      relatedPlaces = relPlaces.map(pr => EventRolePlace.toEventRolePlace(pr)),
      note = sub.baseEvent.note,
      relatedSubEvents = Seq.empty[RelatedEvents],
      partOf = sub.baseEvent.partOf,
      valueLong = maybeLong,
      valueString = maybeStr,
      valueDouble = maybeDouble,
      registeredBy = sub.baseEvent.registeredBy,
      registeredDate = sub.baseEvent.registeredDate,
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
        baseEvent = baseFromDto(dto),
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
      val base = baseFromDto(dto)
      val registeredEvent = ControlSubEvents.unsafeFromId(dto.eventTypeId)
      val evtType = EventType(registeredEvent.entryName)
      val ok = dto.valueLong
      val motivates = {
        dto.relatedSubEvents.headOption.flatMap { re =>
          re.events.headOption.map(ose =>
            ObsConverters.observationSubEventFromDto(ose))
        }
      }

      registeredEvent match {
        case ControlSubEvents.CtrlAlcoholType =>
          ControlAlcohol(
            baseEvent = base,
            eventType = evtType,
            ok = ok,
            motivates = motivates.map(_.asInstanceOf[ObservationAlcohol])
          )

        case ControlSubEvents.CtrlCleaningType =>
          ControlCleaning(
            baseEvent = base,
            eventType = evtType,
            ok = ok,
            motivates = motivates.map(_.asInstanceOf[ObservationCleaning])
          )

        case ControlSubEvents.CtrlGasType =>
          ControlGas(
            baseEvent = base,
            eventType = evtType,
            ok = ok,
            motivates = motivates.map(_.asInstanceOf[ObservationGas])
          )

        case ControlSubEvents.CtrlHypoxicAirType =>
          ControlHypoxicAir(
            baseEvent = base,
            eventType = evtType,
            ok = ok,
            motivates = motivates.map(_.asInstanceOf[ObservationHypoxicAir])
          )

        case ControlSubEvents.CtrlLightingType =>
          ControlLightingCondition(
            baseEvent = base,
            eventType = evtType,
            ok = ok,
            motivates = motivates.map(_.asInstanceOf[ObservationLightingCondition])
          )

        case ControlSubEvents.CtrlMoldType =>
          ControlMold(
            baseEvent = base,
            eventType = evtType,
            ok = ok,
            motivates = motivates.map(_.asInstanceOf[ObservationMold])
          )

        case ControlSubEvents.CtrlPestType =>
          ControlPest(
            baseEvent = base,
            eventType = evtType,
            ok = ok,
            motivates = motivates.map(_.asInstanceOf[ObservationPest])
          )

        case ControlSubEvents.CtrlHumidityType =>
          ControlRelativeHumidity(
            baseEvent = base,
            eventType = evtType,
            ok = ok,
            motivates = motivates.map(_.asInstanceOf[ObservationRelativeHumidity])
          )

        case ControlSubEvents.CtrlTemperatureType =>
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
        baseEvent = baseFromDto(dto),
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

        case obs: ObservationPerimeterSecurity =>
          toBaseDtoNoChildren(obs, maybeStr = obs.perimeterSecurity)

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

    /**
     * Specifically handles dto mapping for observations that does not have
     * custom properties in a separate table in the DB.
     */
    // scalastyle:off cyclomatic.complexity method.length
    def observationSubEventFromDto(dto: Dto): ObservationSubEvent = {
      val base = baseFromDto(dto)
      val registeredEvent = ObservationSubEvents.unsafeFromId(dto.eventTypeId)
      val evtType = EventType(registeredEvent.entryName)

      registeredEvent match {
        case ObservationSubEvents.ObsAlcoholType =>
          ObservationAlcohol(base, evtType, dto.valueString, dto.valueDouble)

        case ObservationSubEvents.ObsCleaningType =>
          ObservationCleaning(base, evtType, dto.valueString)

        case ObservationSubEvents.ObsFireType =>
          ObservationFireProtection(base, evtType, dto.valueString)

        case ObservationSubEvents.ObsGasType =>
          ObservationGas(base, evtType, dto.valueString)

        case ObservationSubEvents.ObsLightingType =>
          ObservationLightingCondition(base, evtType, dto.valueString)

        case ObservationSubEvents.ObsMoldType =>
          ObservationMold(base, evtType, dto.valueString)

        case ObservationSubEvents.ObsPerimeterType =>
          ObservationPerimeterSecurity(base, evtType, dto.valueString)

        case ObservationSubEvents.ObsTheftType =>
          ObservationTheftProtection(base, evtType, dto.valueString)

        case ObservationSubEvents.ObsWaterDamageType =>
          ObservationWaterDamageAssessment(base, evtType, dto.valueString)

        case ObservationSubEvents.ObsPestType =>
          val extDto = dto.asInstanceOf[ExtendedDto]
          val tmpDto = extDto.extension.asInstanceOf[ObservationPestDto]
          val lc = tmpDto.lifeCycles.map(LifeCyleConverters.lifecycleFromDto)
          ObservationPest(base, evtType, dto.valueString, lc)

        case ObservationSubEvents.ObsHumidityType =>
          val extDto = dto.asInstanceOf[ExtendedDto]
          val tmpDto = extDto.extension.asInstanceOf[ObservationFromToDto]
          val fromTo = FromToDouble(tmpDto.from, tmpDto.to)
          ObservationRelativeHumidity(base, evtType, fromTo)

        case ObservationSubEvents.ObsHypoxicAirType =>
          val extDto = dto.asInstanceOf[ExtendedDto]
          val tmpDto = extDto.extension.asInstanceOf[ObservationFromToDto]
          val fromTo = FromToDouble(tmpDto.from, tmpDto.to)
          ObservationHypoxicAir(base, evtType, fromTo)

        case ObservationSubEvents.ObsTemperatureType =>
          val extDto = dto.asInstanceOf[ExtendedDto]
          val tmpDto = extDto.extension.asInstanceOf[ObservationFromToDto]
          val fromTo = FromToDouble(tmpDto.from, tmpDto.to)
          ObservationTemperature(base, evtType, fromTo)

      }
    } // scalastyle:on cyclomatic.complexity method.length

  }

  /**
   * Converters for the Environment requirement event
   */
  object EnvReqConverters {

    private[this] def toEnvReqDto(envReq: EnvRequirement): EnvRequirementDto = {
      EnvRequirementDto(
        id = envReq.baseEvent.id,
        temperature = envReq.temperature.map(_.base),
        tempInterval = envReq.temperature.flatMap(_.tolerance),
        airHumidity = envReq.airHumidity.map(_.base),
        airHumInterval = envReq.airHumidity.flatMap(_.tolerance),
        hypoxicAir = envReq.hypoxicAir.map(_.base),
        hypoxicInterval = envReq.hypoxicAir.flatMap(_.tolerance),
        cleaning = envReq.cleaning,
        light = envReq.light
      )
    }

    def envReqToDto(envReq: EnvRequirement): ExtendedDto = {
      toExtendedDto(
        sub = envReq,
        ext = toEnvReqDto(envReq)
      )
    }

    def envReqFromDto(dto: ExtendedDto): EnvRequirement = {
      val envReqDto = dto.extension.asInstanceOf[EnvRequirementDto]

      val temp = toInterval(envReqDto.temperature, envReqDto.tempInterval)
      val humidity = toInterval(envReqDto.airHumidity, envReqDto.airHumInterval)
      val hypoxic = toInterval(envReqDto.hypoxicAir, envReqDto.hypoxicInterval)

      EnvRequirement(
        baseEvent = baseFromDto(dto),
        eventType = EventType.fromEventTypeId(dto.eventTypeId),
        temperature = temp,
        airHumidity = humidity,
        hypoxicAir = hypoxic,
        cleaning = envReqDto.cleaning,
        light = envReqDto.light
      )
    }
  }

  /**
   * Converters for Move events
   */
  object MoveConverters {

    private def moveToDto[A <: Move](move: A): BaseEventDto = {
      toBaseDto(
        sub = move,
        relPlaces = Seq(move.to)
      )
    }

    private def moveFromDto[A <: Move](
      dto: BaseEventDto
    )(init: (MusitEventBase, EventType, PlaceRole) => A): A = {
      val base = baseFromDto(dto)
      val eventType = EventType.fromEventTypeId(dto.eventTypeId)
      val placeRole = EventRolePlace.toPlaceRole(dto.relatedPlaces.head)
      init(base, eventType, placeRole)
    }

    def moveObjectToDto(mo: MoveObject): BaseEventDto = moveToDto(mo)

    def moveObjectFromDto(dto: BaseEventDto): MoveObject =
      moveFromDto(dto)((base, eventType, pr) => MoveObject(base, eventType, pr))

    def moveNodeToDto(mo: MoveNode): BaseEventDto = moveToDto(mo)

    def moveNodeFromDto(dto: BaseEventDto): MoveNode =
      moveFromDto(dto)((base, eventType, pr) => MoveNode(base, eventType, pr))
  }

}
