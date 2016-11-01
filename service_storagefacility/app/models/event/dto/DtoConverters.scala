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

package models.event.dto

import models.datetime.Implicits._
import models.event.EventTypeRegistry._
import models.event._
import models.event.control._
import models.event.envreq.EnvRequirement
import models.event.move.{MoveEvent, MoveNode, MoveObject}
import models.event.observation._
import models.event.observation.ObservationSubEvents._
import models.{FromToDouble, Interval, LifeCycle}
import models.event.control.ControlSubEvents._
import no.uio.musit.models.{ActorId, ObjectId, StorageNodeId}

object DtoConverters {

  /**
   * Converts a Boolean value to a Long.
   */
  implicit def boolToOptLong(b: Boolean): Option[Long] = Option(if (b) 1 else 0)

  /**
   * Converts a Long value to a Boolean.
   */
  implicit def maybeLongToBool(mi: Option[Long]): Boolean = mi.contains(1)

  def fromMapObsSub[E](
    m: Map[EventTypeId, ObservationSubEvent],
    id: EventTypeId
  )(implicit ct: Manifest[E]): Option[E] =
    m.get(id).map(_.asInstanceOf[E])

  def fromMapCtrlSub[E](
    m: Map[EventTypeId, ControlSubEvent],
    id: EventTypeId
  )(implicit ct: Manifest[E]): Option[E] =
    m.get(id).map(_.asInstanceOf[E])

  /**
   * Converts a pair of values to an Interval.
   */
  def toInterval[A](
    base: Option[A],
    tolerance: Option[Int]
  ): Option[Interval[A]] = base.map(b => Interval[A](b, tolerance))

  /**
   * Helper function to generate a base Dto that is shared across all event
   * types.
   */
  // scalastyle:off parameter.number
  private[this] def toBaseDto[T <: MusitEvent](
    base: T,
    eventTypeId: EventTypeId,
    note: Option[String],
    maybeStr: Option[String] = None,
    maybeLong: Option[Long] = None,
    maybeDouble: Option[Double] = None,
    relEvents: Seq[RelatedEvents] = Seq.empty,
    relPlaces: Seq[PlaceRole] = Seq.empty
  ): BaseEventDto = {
    val affectedThing = base.affectedThing.map(id => ObjectRole(1, id))

    BaseEventDto(
      id = base.id,
      eventTypeId = eventTypeId,
      eventDate = base.doneDate,
      // For the following related* fields, we do not yet know the ID to set.
      // So they are initialised with the eventId property to None.
      relatedActors = base.doneBy.map(ar => EventRoleActor.fromActorRole(ActorRole(1, ar.underlying))).toSeq,
      relatedObjects = affectedThing.map(or => EventRoleObject.fromObjectRole(or, base.eventType.registeredEventId)).toSeq,
      relatedPlaces = relPlaces.map(pr => EventRolePlace.fromPlaceRole(pr, base.eventType.registeredEventId)),
      note = note,
      relatedSubEvents = relEvents,
      partOf = None,
      valueLong = maybeLong,
      valueString = maybeStr,
      valueDouble = maybeDouble,
      registeredBy = base.registeredBy,
      registeredDate = base.registeredDate
    )
  }

  // scalastyle:on parameter.number

  /**
   * Helper function to generate a base Dto that is shared across all event
   * types. Differs from the above function in that it will initialise the
   * `releatedEvents` property with an empty collection.
   */
  private[this] def toBaseDtoNoChildren[T <: MusitEvent](
    base: T,
    eventTypeId: EventTypeId,
    note: Option[String],
    maybeStr: Option[String] = None,
    maybeLong: Option[Long] = None,
    maybeDouble: Option[Double] = None
  ): BaseEventDto = {
    toBaseDto[T](base, eventTypeId, note, maybeStr, maybeLong, maybeDouble)
  }

  // scalastyle:off parameter.number
  private[this] def toExtendedDto[A <: MusitEvent, B <: DtoExtension](
    base: A,
    eventTypeId: EventTypeId,
    note: Option[String],
    maybeStr: Option[String] = None,
    maybeLong: Option[Long] = None,
    maybeDouble: Option[Double] = None,
    relPlaces: Seq[PlaceRole] = Seq.empty,
    ext: B
  ): ExtendedDto = {
    val affectedThing = base.affectedThing.map(id => ObjectRole(1, id))

    ExtendedDto(
      id = base.id,
      eventTypeId = eventTypeId,
      eventDate = base.doneDate,
      // For the following related* fields, we do not yet know the ID to set.
      // So they are initialised with the eventId property to None.
      relatedActors = base.doneBy.map(ar => EventRoleActor.fromActorRole(ActorRole(1, ar.underlying))).toSeq,
      relatedObjects = affectedThing.map(or => EventRoleObject.fromObjectRole(or, base.eventType.registeredEventId)).toSeq,
      relatedPlaces = relPlaces.map(pr => EventRolePlace.fromPlaceRole(pr, base.eventType.registeredEventId)),
      note = note,
      relatedSubEvents = Seq.empty[RelatedEvents],
      partOf = None,
      valueLong = maybeLong,
      valueString = maybeStr,
      valueDouble = maybeDouble,
      registeredBy = base.registeredBy,
      registeredDate = base.registeredDate,
      extension = ext
    )
  }

  // scalastyle:on parameter.number

  /**
   * Converters for the LifeCycle set of types.
   */
  object LifeCyleConverters {

    def lifecycleToDto(lc: LifeCycle): LifecycleDto = {
      LifecycleDto(
        eventId = None,
        stage = lc.stage,
        quantity = lc.quantity
      )
    }

    def lifecycleFromDto(lcd: LifecycleDto): LifeCycle = {
      LifeCycle(
        stage = lcd.stage,
        quantity = lcd.quantity
      )
    }
  }

  /**
   * Converters for the Control specific types.
   */
  object CtrlConverters {

    def controlToDto(ctrl: Control): BaseEventDto = {
      val regBy = ctrl.registeredBy
      val regDate = ctrl.registeredDate
      val affectedThing = ctrl.affectedThing.map(id => ObjectRole(1, id))
      val relEvt = Seq(
        RelatedEvents(
          relation = EventRelations.PartsOfRelation,
          events = {
          val parent = ctrl.id
          val parts = Seq.newBuilder[EventDto]
          ctrl.alcohol.map(c => parts += ctrlSubEventToDto(ctrl, c, CtrlSubEvents.CtrlAlcoholType.id, ObsSubEvents.ObsAlcoholType.id))
          ctrl.cleaning.map(c => parts += ctrlSubEventToDto(ctrl, c, CtrlSubEvents.CtrlCleaningType.id, ObsSubEvents.ObsCleaningType.id))
          ctrl.gas.map(c => parts += ctrlSubEventToDto(ctrl, c, CtrlSubEvents.CtrlGasType.id, ObsSubEvents.ObsGasType.id))
          ctrl.hypoxicAir.map(c => parts += ctrlSubEventToDto(ctrl, c, CtrlSubEvents.CtrlHypoxicAirType.id, ObsSubEvents.ObsHypoxicAirType.id))
          ctrl.lightingCondition.map(c => parts += ctrlSubEventToDto(ctrl, c, CtrlSubEvents.CtrlLightingType.id, ObsSubEvents.ObsLightingType.id))
          ctrl.mold.map(c => parts += ctrlSubEventToDto(ctrl, c, CtrlSubEvents.CtrlMoldType.id, ObsSubEvents.ObsMoldType.id))
          ctrl.pest.map(c => parts += ctrlSubEventToDto(ctrl, c, CtrlSubEvents.CtrlPestType.id, ObsSubEvents.ObsPestType.id))
          ctrl.relativeHumidity.map(c => parts += ctrlSubEventToDto(ctrl, c, CtrlSubEvents.CtrlHumidityType.id, ObsSubEvents.ObsHumidityType.id))
          ctrl.temperature.map(c => parts += ctrlSubEventToDto(ctrl, c, CtrlSubEvents.CtrlTemperatureType.id, ObsSubEvents.ObsTemperatureType.id))
          parts.result()
        }
        )
      )
      toBaseDto(
        base = ctrl,
        eventTypeId = ctrl.eventType.registeredEventId,
        note = None,
        relEvents = relEvt
      )
    }

    def controlFromDto(dto: BaseEventDto): Control = {
      val p = {
        dto.relatedSubEvents.headOption.map { re =>
          re.events.map(e => e.eventTypeId -> ctrlSubEventFromDto(e.asInstanceOf[BaseEventDto])).toMap
        }.getOrElse(Map.empty)
      }
      Control(
        id = dto.id,
        doneDate = dto.eventDate,
        doneBy = dto.relatedActors.map(e => ActorId(e.actorId.toLong)).headOption,
        affectedThing = dto.relatedObjects.map(e => StorageNodeId(e.objectId)).headOption,
        registeredBy = dto.registeredBy,
        registeredDate = dto.registeredDate,
        eventType = EventType.fromEventTypeId(dto.eventTypeId),
        alcohol = fromMapCtrlSub[ControlAlcohol](p, CtrlSubEvents.CtrlAlcoholType.id),
        cleaning = fromMapCtrlSub[ControlCleaning](p, CtrlSubEvents.CtrlCleaningType.id),
        gas = fromMapCtrlSub[ControlGas](p, CtrlSubEvents.CtrlGasType.id),
        hypoxicAir = fromMapCtrlSub[ControlHypoxicAir](p, CtrlSubEvents.CtrlHypoxicAirType.id),
        lightingCondition = fromMapCtrlSub[ControlLightingCondition](p, CtrlSubEvents.CtrlLightingType.id),
        mold = fromMapCtrlSub[ControlMold](p, CtrlSubEvents.CtrlMoldType.id),
        pest = fromMapCtrlSub[ControlPest](p, CtrlSubEvents.CtrlPestType.id),
        relativeHumidity = fromMapCtrlSub[ControlRelativeHumidity](p, CtrlSubEvents.CtrlHumidityType.id),
        temperature = fromMapCtrlSub[ControlTemperature](p, CtrlSubEvents.CtrlTemperatureType.id)
      )
    }

    def ctrlSubEventToDto[A <: MusitEvent](
      owner: A,
      subCtrl: ControlSubEvent,
      ctrlSubEventTypeId: EventTypeId,
      obsSubEventTypeId: EventTypeId
    ): EventDto = {
      val relations = subCtrl.observation.map { ose =>
        Seq(
          RelatedEvents(
            relation = EventRelations.MotivatesRelation,
            events = subCtrl.observation.map { m =>
              Seq(ObsConverters.obsSubEventToDto(owner, obsSubEventTypeId, m))
            }.getOrElse(Seq.empty)
          )
        )
      }.getOrElse(Seq.empty)

      toBaseDto(
        base = owner,
        eventTypeId = ctrlSubEventTypeId,
        note = None,
        maybeLong = subCtrl.ok,
        relEvents = relations
      )
    }

    // scalastyle:off method.length
    def ctrlSubEventFromDto(dto: BaseEventDto): ControlSubEvent = {
      val ok = dto.valueLong
      val motivates = {
        dto.relatedSubEvents.headOption.flatMap { re =>
          re.events.headOption.map(ose =>
            ObsConverters.obsSubEventFromDto(ose))
        }
      }

      CtrlSubEvents.unsafeFromId(dto.eventTypeId) match {
        case CtrlSubEvents.CtrlAlcoholType =>
          ControlAlcohol(ok, motivates.map(_.asInstanceOf[ObservationAlcohol]))

        case CtrlSubEvents.CtrlCleaningType =>
          ControlCleaning(ok, motivates.map(_.asInstanceOf[ObservationCleaning]))

        case CtrlSubEvents.CtrlGasType =>
          ControlGas(ok, motivates.map(_.asInstanceOf[ObservationGas]))

        case CtrlSubEvents.CtrlHypoxicAirType =>
          ControlHypoxicAir(ok, motivates.map(_.asInstanceOf[ObservationHypoxicAir]))

        case CtrlSubEvents.CtrlLightingType =>
          ControlLightingCondition(ok, motivates.map(_.asInstanceOf[ObservationLightingCondition]))

        case CtrlSubEvents.CtrlMoldType =>
          ControlMold(ok, motivates.map(_.asInstanceOf[ObservationMold]))

        case CtrlSubEvents.CtrlPestType =>
          ControlPest(ok, motivates.map(_.asInstanceOf[ObservationPest]))

        case CtrlSubEvents.CtrlHumidityType =>
          ControlRelativeHumidity(ok, motivates.map(_.asInstanceOf[ObservationRelativeHumidity]))

        case CtrlSubEvents.CtrlTemperatureType =>
          ControlTemperature(ok, motivates.map(_.asInstanceOf[ObservationTemperature]))
      }
    } // scalastyle:on method.length
  }

  /**
   * Converters for the Observation specific types.
   */
  object ObsConverters {

    def observationToDto(obs: Observation): EventDto = {
      val relEvt = Seq(
        RelatedEvents(
          relation = EventRelations.PartsOfRelation,
          events = {
          val parent = obs.id
          val parts = Seq.newBuilder[EventDto]
          obs.alcohol.map(o => parts += obsSubEventToDto(obs, ObsSubEvents.ObsAlcoholType.id, o))
          obs.cleaning.map(o => parts += obsSubEventToDto(obs, ObsSubEvents.ObsCleaningType.id, o))
          obs.gas.map(o => parts += obsSubEventToDto(obs, ObsSubEvents.ObsGasType.id, o))
          obs.hypoxicAir.map(o => parts += obsSubEventToDto(obs, ObsSubEvents.ObsHypoxicAirType.id, o))
          obs.lightingCondition.map(o => parts += obsSubEventToDto(obs, ObsSubEvents.ObsLightingType.id, o))
          obs.mold.map(o => parts += obsSubEventToDto(obs, ObsSubEvents.ObsMoldType.id, o))
          obs.pest.map(o => parts += obsSubEventToDto(obs, ObsSubEvents.ObsPestType.id, o))
          obs.relativeHumidity.map(o => parts += obsSubEventToDto(obs, ObsSubEvents.ObsHumidityType.id, o))
          obs.temperature.map(o => parts += obsSubEventToDto(obs, ObsSubEvents.ObsTemperatureType.id, o))
          obs.theftProtection.map(o => parts += obsSubEventToDto(obs, ObsSubEvents.ObsTheftType.id, o))
          obs.fireProtection.map(o => parts += obsSubEventToDto(obs, ObsSubEvents.ObsFireType.id, o))
          obs.perimeterSecurity.map(o => parts += obsSubEventToDto(obs, ObsSubEvents.ObsPerimeterType.id, o))
          obs.waterDamageAssessment.map(o => parts += obsSubEventToDto(obs, ObsSubEvents.ObsWaterDamageType.id, o))

          parts.result()
        }
        )
      )
      toBaseDto(
        base = obs,
        note = None,
        eventTypeId = obs.eventType.registeredEventId,
        relEvents = relEvt
      )
    }

    def observationFromDto(dto: BaseEventDto): Observation = {
      val p = {
        dto.relatedSubEvents.headOption.map { re =>
          re.events.map(e => e.eventTypeId -> obsSubEventFromDto(e)).toMap
        }.getOrElse(Map.empty)
      }

      Observation(
        id = dto.id,
        doneDate = dto.eventDate,
        doneBy = dto.relatedActors.map(e => ActorId(e.actorId.toLong)).headOption,
        affectedThing = dto.relatedObjects.map(e => StorageNodeId(e.objectId)).headOption,
        registeredBy = dto.registeredBy,
        registeredDate = dto.registeredDate,
        eventType = EventType.fromEventTypeId(dto.eventTypeId),
        alcohol = fromMapObsSub[ObservationAlcohol](p, ObsSubEvents.ObsAlcoholType.id),
        cleaning = fromMapObsSub[ObservationCleaning](p, ObsSubEvents.ObsCleaningType.id),
        gas = fromMapObsSub[ObservationGas](p, ObsSubEvents.ObsGasType.id),
        hypoxicAir = fromMapObsSub[ObservationHypoxicAir](p, ObsSubEvents.ObsHypoxicAirType.id),
        lightingCondition = fromMapObsSub[ObservationLightingCondition](p, ObsSubEvents.ObsLightingType.id),
        mold = fromMapObsSub[ObservationMold](p, ObsSubEvents.ObsMoldType.id),
        pest = fromMapObsSub[ObservationPest](p, ObsSubEvents.ObsPestType.id),
        relativeHumidity = fromMapObsSub[ObservationRelativeHumidity](p, ObsSubEvents.ObsHumidityType.id),
        temperature = fromMapObsSub[ObservationTemperature](p, ObsSubEvents.ObsTemperatureType.id),
        theftProtection = fromMapObsSub[ObservationTheftProtection](p, ObsSubEvents.ObsTheftType.id),
        fireProtection = fromMapObsSub[ObservationFireProtection](p, ObsSubEvents.ObsFireType.id),
        perimeterSecurity = fromMapObsSub[ObservationPerimeterSecurity](p, ObsSubEvents.ObsPerimeterType.id),
        waterDamageAssessment = fromMapObsSub[ObservationWaterDamageAssessment](p, ObsSubEvents.ObsWaterDamageType.id)
      )
    }

    // scalastyle:off cyclomatic.complexity method.length
    def obsSubEventToDto[A <: MusitEvent, T <: ObservationSubEvent](
      owner: A,
      subEventTypeId: EventTypeId,
      subObs: T
    ): EventDto = {
      subObs match {
        case obs: ObservationFromTo =>
          toExtendedDto(
            base = owner,
            eventTypeId = subEventTypeId,
            note = obs.note,
            ext = ObservationFromToDto(
              id = None,
              from = obs.range.from,
              to = obs.range.to
            )
          )

        case obs: ObservationLightingCondition =>
          toBaseDtoNoChildren(owner, ObsSubEvents.ObsLightingType.id, obs.note, obs.lightingCondition)

        case obs: ObservationCleaning =>
          toBaseDtoNoChildren(owner, ObsSubEvents.ObsCleaningType.id, obs.note, obs.cleaning)

        case obs: ObservationGas =>
          toBaseDtoNoChildren(owner, ObsSubEvents.ObsGasType.id, obs.note, obs.gas)

        case obs: ObservationMold =>
          toBaseDtoNoChildren(owner, ObsSubEvents.ObsMoldType.id, obs.note, obs.mold)

        case obs: ObservationTheftProtection =>
          toBaseDtoNoChildren(owner, ObsSubEvents.ObsTheftType.id, obs.note, obs.theftProtection)

        case obs: ObservationFireProtection =>
          toBaseDtoNoChildren(owner, ObsSubEvents.ObsFireType.id, obs.note, obs.fireProtection)

        case obs: ObservationWaterDamageAssessment =>
          toBaseDtoNoChildren(owner, ObsSubEvents.ObsWaterDamageType.id, obs.note, obs.waterDamageAssessment)

        case obs: ObservationPerimeterSecurity =>
          toBaseDtoNoChildren(owner, ObsSubEvents.ObsPerimeterType.id, obs.note, obs.perimeterSecurity)

        case obs: ObservationPest =>
          toExtendedDto(
            base = owner,
            eventTypeId = ObsSubEvents.ObsPestType.id,
            note = obs.note,
            maybeStr = obs.identification,
            ext = ObservationPestDto(
              lifeCycles = obs.lifecycles.map(LifeCyleConverters.lifecycleToDto)
            )
          )

        case obs: ObservationAlcohol =>
          toBaseDtoNoChildren(
            base = owner,
            eventTypeId = ObsSubEvents.ObsAlcoholType.id,
            note = obs.note,
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
    def obsSubEventFromDto(dto: EventDto): ObservationSubEvent = {
      ObsSubEvents.unsafeFromId(dto.eventTypeId) match {
        case ObsSubEvents.ObsAlcoholType =>
          ObservationAlcohol(dto.note, dto.valueString, dto.valueDouble)

        case ObsSubEvents.ObsCleaningType =>
          ObservationCleaning(dto.note, dto.valueString)

        case ObsSubEvents.ObsFireType =>
          ObservationFireProtection(dto.note, dto.valueString)

        case ObsSubEvents.ObsGasType =>
          ObservationGas(dto.note, dto.valueString)

        case ObsSubEvents.ObsLightingType =>
          ObservationLightingCondition(dto.note, dto.valueString)

        case ObsSubEvents.ObsMoldType =>
          ObservationMold(dto.note, dto.valueString)

        case ObsSubEvents.ObsPerimeterType =>
          ObservationPerimeterSecurity(dto.note, dto.valueString)

        case ObsSubEvents.ObsTheftType =>
          ObservationTheftProtection(dto.note, dto.valueString)

        case ObsSubEvents.ObsWaterDamageType =>
          ObservationWaterDamageAssessment(dto.note, dto.valueString)

        case ObsSubEvents.ObsPestType =>
          val extDto = dto.asInstanceOf[ExtendedDto]
          val tmpDto = extDto.extension.asInstanceOf[ObservationPestDto]
          val lc = tmpDto.lifeCycles.map(LifeCyleConverters.lifecycleFromDto)
          ObservationPest(dto.note, dto.valueString, lc)

        case ObsSubEvents.ObsHumidityType =>
          val extDto = dto.asInstanceOf[ExtendedDto]
          val tmpDto = extDto.extension.asInstanceOf[ObservationFromToDto]
          val fromTo = FromToDouble(tmpDto.from, tmpDto.to)
          ObservationRelativeHumidity(dto.note, fromTo)

        case ObsSubEvents.ObsHypoxicAirType =>
          val extDto = dto.asInstanceOf[ExtendedDto]
          val tmpDto = extDto.extension.asInstanceOf[ObservationFromToDto]
          val fromTo = FromToDouble(tmpDto.from, tmpDto.to)
          ObservationHypoxicAir(dto.note, fromTo)

        case ObsSubEvents.ObsTemperatureType =>
          val extDto = dto.asInstanceOf[ExtendedDto]
          val tmpDto = extDto.extension.asInstanceOf[ObservationFromToDto]
          val fromTo = FromToDouble(tmpDto.from, tmpDto.to)
          ObservationTemperature(dto.note, fromTo)

      }
    } // scalastyle:on cyclomatic.complexity method.length

  }

  /**
   * Converters for the Environment requirement event
   */
  object EnvReqConverters {

    private[this] def toEnvReqDto(envReq: EnvRequirement): EnvRequirementDto = {
      EnvRequirementDto(
        id = envReq.id,
        temperature = envReq.temperature.map(_.base),
        tempTolerance = envReq.temperature.flatMap(_.tolerance),
        airHumidity = envReq.airHumidity.map(_.base),
        airHumTolerance = envReq.airHumidity.flatMap(_.tolerance),
        hypoxicAir = envReq.hypoxicAir.map(_.base),
        hypoxicTolerance = envReq.hypoxicAir.flatMap(_.tolerance),
        cleaning = envReq.cleaning,
        light = envReq.light
      )
    }

    def envReqToDto(envReq: EnvRequirement): ExtendedDto = {
      toExtendedDto(
        base = envReq,
        note = envReq.note,
        eventTypeId = TopLevelEvents.EnvRequirementEventType.id,
        ext = toEnvReqDto(envReq)
      )
    }

    def envReqFromDto(dto: ExtendedDto): EnvRequirement = {
      val envReqDto = dto.extension.asInstanceOf[EnvRequirementDto]

      val temp = toInterval(envReqDto.temperature, envReqDto.tempTolerance)
      val humidity = toInterval(envReqDto.airHumidity, envReqDto.airHumTolerance)
      val hypoxic = toInterval(envReqDto.hypoxicAir, envReqDto.hypoxicTolerance)

      EnvRequirement(
        id = dto.id,
        doneDate = dto.eventDate,
        doneBy = dto.relatedActors.map(e => ActorId(e.actorId.toLong)).headOption,
        affectedThing = dto.relatedObjects.map(e => StorageNodeId(e.objectId)).headOption,
        note = dto.note,
        registeredBy = dto.registeredBy,
        registeredDate = dto.registeredDate,
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

    def moveToDto[A <: MoveEvent](move: A): BaseEventDto = {
      toBaseDto(
        base = move,
        note = None,
        eventTypeId = move.eventType.registeredEventId,
        relPlaces = Seq(PlaceRole(1, move.to)),
        maybeLong = move.from
      )
    }

    def moveFromDto[A <: MoveEvent](
      dto: BaseEventDto
    )(init: (EventType, Option[StorageNodeId], StorageNodeId) => A): A = {
      val eventType = EventType.fromEventTypeId(dto.eventTypeId)
      val from = dto.valueLong
      val to = dto.relatedPlaces.head.placeId
      init(eventType, from, to)
    }

    def moveObjectToDto(mo: MoveObject): BaseEventDto = moveToDto(mo)

    def moveObjectFromDto(dto: BaseEventDto): MoveObject =
      moveFromDto(dto)((eventType, from, to) =>
        MoveObject(
          id = dto.id,
          doneDate = dto.eventDate,
          doneBy = dto.relatedActors.map(e => ActorId(e.actorId.toLong)).headOption,
          affectedThing = dto.relatedObjects.map(e => ObjectId(e.objectId)).headOption,
          registeredBy = dto.registeredBy,
          registeredDate = dto.registeredDate,
          eventType = eventType,
          from = from,
          to = to
        ))

    def moveNodeToDto(mo: MoveNode): BaseEventDto = moveToDto(mo)

    def moveNodeFromDto(dto: BaseEventDto): MoveNode =
      moveFromDto(dto)((eventType, from, to) =>
        MoveNode(
          id = dto.id,
          doneDate = dto.eventDate,
          doneBy = dto.relatedActors.map(e => ActorId(e.actorId.toLong)).headOption,
          affectedThing = dto.relatedObjects.map(e => StorageNodeId(e.objectId)).headOption,
          registeredBy = dto.registeredBy,
          registeredDate = dto.registeredDate,
          eventType = eventType,
          from = from,
          to = to
        ))
  }

}
