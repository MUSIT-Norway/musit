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

package models.storage.event.dto

import models.storage.event.EventTypeRegistry.CtrlSubEvents._
import models.storage.event.EventTypeRegistry.ObsSubEvents._
import models.storage.event.EventTypeRegistry._
import models.storage.event._
import models.storage.event.control.ControlSubEvents._
import models.storage.event.control._
import models.storage.event.dto.EventRoleActor._
import models.storage.event.dto.EventRoleObject._
import models.storage.event.dto.EventRolePlace._
import models.storage.event.envreq.EnvRequirement
import models.storage.event.move.{MoveEvent, MoveNode, MoveObject}
import models.storage.event.observation.ObservationSubEvents._
import models.storage.event.observation._
import models.storage.{FromToDouble, Interval, LifeCycle}
import no.uio.musit.models.ObjectTypes.{CollectionObject, ObjectType}
import no.uio.musit.models.{ObjectId, StorageNodeDatabaseId}
import no.uio.musit.time.Implicits._

object DtoConverters {

  /**
   * Converts a Boolean value to a Long.
   */
  implicit def boolToOptLong(b: Boolean): Option[Long] = Option(if (b) 1L else 0L)

  /**
   * Converts a Long value to a Boolean.
   */
  implicit def maybeLongToBool(mi: Option[Long]): Boolean = mi.contains(1L)

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

    // scalastyle:off line.size.limit
    BaseEventDto(
      id = base.id,
      eventTypeId = eventTypeId,
      eventDate = base.doneDate,
      // For the following related* fields, we do not yet know the ID to set.
      // So they are initialised with the eventId property to None.
      relatedActors =
        base.doneBy.map(ar => fromActorRole(ActorRole(1, ar.underlying))).toSeq,
      relatedObjects = affectedThing
        .map(or => fromObjectRole(or, base.eventType.registeredEventId))
        .toSeq,
      relatedPlaces =
        relPlaces.map(pr => fromPlaceRole(pr, base.eventType.registeredEventId)),
      note = note,
      relatedSubEvents = relEvents,
      partOf = None,
      valueLong = maybeLong,
      valueString = maybeStr,
      valueDouble = maybeDouble,
      registeredBy = base.registeredBy,
      registeredDate = base.registeredDate
    )
    // scalastyle:on line.size.limit
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

    // scalastyle:off line.size.limit
    ExtendedDto(
      id = base.id,
      eventTypeId = eventTypeId,
      eventDate = base.doneDate,
      // For the following related* fields, we do not yet know the ID to set.
      // So they are initialised with the eventId property to None.
      relatedActors =
        base.doneBy.map(ar => fromActorRole(ActorRole(1, ar.underlying))).toSeq,
      relatedObjects = affectedThing
        .map(or => fromObjectRole(or, base.eventType.registeredEventId))
        .toSeq,
      relatedPlaces =
        relPlaces.map(pr => fromPlaceRole(pr, base.eventType.registeredEventId)),
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
    // scalastyle:on line.size.limit
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

    // scalastyle:off method.length line.size.limit
    def controlToDto(ctrl: Control): BaseEventDto = {
      val regBy         = ctrl.registeredBy
      val regDate       = ctrl.registeredDate
      val affectedThing = ctrl.affectedThing.map(id => ObjectRole(1, id))
      val relEvt = Seq(
        RelatedEvents(
          relation = EventRelations.PartsOfRelation,
          events = {
            val parent = ctrl.id
            val parts  = Seq.newBuilder[EventDto]
            // format: off
            ctrl.alcohol.map { c =>
              parts += ctrlSubEventToDto(ctrl, c, CtrlAlcoholType.id, ObsAlcoholType.id)
            }
            ctrl.cleaning.map { c =>
              parts += ctrlSubEventToDto(ctrl, c, CtrlCleaningType.id, ObsCleaningType.id)
            }
            ctrl.gas.map { c =>
              parts += ctrlSubEventToDto(ctrl, c, CtrlGasType.id, ObsGasType.id)
            }
            ctrl.hypoxicAir.map { c =>
              parts += ctrlSubEventToDto(ctrl, c, CtrlHypoxicAirType.id, ObsHypoxicAirType.id)
            }
            ctrl.lightingCondition.map { c =>
              parts += ctrlSubEventToDto(ctrl, c, CtrlLightingType.id, ObsLightingType.id)
            }
            ctrl.mold.map { c =>
              parts += ctrlSubEventToDto(ctrl, c, CtrlMoldType.id, ObsMoldType.id)
            }
            ctrl.pest.map { c =>
              parts += ctrlSubEventToDto(ctrl, c, CtrlPestType.id, ObsPestType.id)
            }
            ctrl.relativeHumidity.map { c =>
              parts += ctrlSubEventToDto(ctrl, c, CtrlHumidityType.id, ObsHumidityType.id)
            }
            ctrl.temperature.map { c =>
              parts += ctrlSubEventToDto(ctrl, c, CtrlTemperatureType.id, ObsTemperatureType.id)
            }
            // format: on
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

    // scalastyle:on method.length line.size.limit

    def controlFromDto(dto: BaseEventDto): Control = {
      val p = {
        dto.relatedSubEvents.headOption.map { re =>
          re.events.map { e =>
            e.eventTypeId -> ctrlSubEventFromDto(e.asInstanceOf[BaseEventDto])
          }.toMap
        }.getOrElse(Map.empty)
      }
      // scalastyle:off line.size.limit
      Control(
        id = dto.id,
        doneDate = dto.eventDate,
        doneBy = dto.relatedActors.map(_.actorId).headOption,
        affectedThing =
          dto.relatedObjects.map(e => StorageNodeDatabaseId(e.objectId)).headOption,
        registeredBy = dto.registeredBy,
        registeredDate = dto.registeredDate,
        eventType = EventType.fromEventTypeId(dto.eventTypeId),
        alcohol = fromMapCtrlSub[ControlAlcohol](p, CtrlAlcoholType.id),
        cleaning = fromMapCtrlSub[ControlCleaning](p, CtrlCleaningType.id),
        gas = fromMapCtrlSub[ControlGas](p, CtrlGasType.id),
        hypoxicAir = fromMapCtrlSub[ControlHypoxicAir](p, CtrlHypoxicAirType.id),
        lightingCondition =
          fromMapCtrlSub[ControlLightingCondition](p, CtrlLightingType.id),
        mold = fromMapCtrlSub[ControlMold](p, CtrlMoldType.id),
        pest = fromMapCtrlSub[ControlPest](p, CtrlPestType.id),
        relativeHumidity =
          fromMapCtrlSub[ControlRelativeHumidity](p, CtrlHumidityType.id),
        temperature = fromMapCtrlSub[ControlTemperature](p, CtrlTemperatureType.id)
        // scalastyle:on line.size.limit
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

    // scalastyle:off method.length line.size.limit
    def ctrlSubEventFromDto(dto: BaseEventDto): ControlSubEvent = {
      val ok = dto.valueLong
      val motivates = {
        dto.relatedSubEvents.headOption.flatMap { re =>
          re.events.headOption.map(ose => ObsConverters.obsSubEventFromDto(ose))
        }
      }

      // format: off
      CtrlSubEvents.unsafeFromId(dto.eventTypeId) match {
        case CtrlAlcoholType =>
          ControlAlcohol(ok, motivates.map(_.asInstanceOf[ObservationAlcohol]))

        case CtrlCleaningType =>
          ControlCleaning(ok, motivates.map(_.asInstanceOf[ObservationCleaning]))

        case CtrlGasType =>
          ControlGas(ok, motivates.map(_.asInstanceOf[ObservationGas]))

        case CtrlSubEvents.CtrlHypoxicAirType =>
          ControlHypoxicAir(ok, motivates.map(_.asInstanceOf[ObservationHypoxicAir]))

        case CtrlLightingType =>
          ControlLightingCondition(ok, motivates.map(_.asInstanceOf[ObservationLightingCondition]))

        case CtrlMoldType =>
          ControlMold(ok, motivates.map(_.asInstanceOf[ObservationMold]))

        case CtrlPestType =>
          ControlPest(ok, motivates.map(_.asInstanceOf[ObservationPest]))

        case CtrlHumidityType =>
          ControlRelativeHumidity(ok, motivates.map(_.asInstanceOf[ObservationRelativeHumidity]))

        case CtrlTemperatureType =>
          ControlTemperature(ok, motivates.map(_.asInstanceOf[ObservationTemperature]))
      }
      // format: on
    } // scalastyle:on method.length line.size.limit
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
            val parts  = Seq.newBuilder[EventDto]
            // scalastyle:off line.size.limit
            obs.alcohol.map(o => parts += obsSubEventToDto(obs, ObsAlcoholType.id, o))
            obs.cleaning.map(o => parts += obsSubEventToDto(obs, ObsCleaningType.id, o))
            obs.gas.map(o => parts += obsSubEventToDto(obs, ObsGasType.id, o))
            obs.hypoxicAir.map(
              o => parts += obsSubEventToDto(obs, ObsHypoxicAirType.id, o)
            )
            obs.lightingCondition.map(
              o => parts += obsSubEventToDto(obs, ObsLightingType.id, o)
            )
            obs.mold.map(o => parts += obsSubEventToDto(obs, ObsMoldType.id, o))
            obs.pest.map(o => parts += obsSubEventToDto(obs, ObsPestType.id, o))
            obs.relativeHumidity.map(
              o => parts += obsSubEventToDto(obs, ObsHumidityType.id, o)
            )
            obs.temperature.map(
              o => parts += obsSubEventToDto(obs, ObsTemperatureType.id, o)
            )
            obs.theftProtection.map(
              o => parts += obsSubEventToDto(obs, ObsTheftType.id, o)
            )
            obs.fireProtection.map(
              o => parts += obsSubEventToDto(obs, ObsFireType.id, o)
            )
            obs.perimeterSecurity.map(
              o => parts += obsSubEventToDto(obs, ObsPerimeterType.id, o)
            )
            obs.waterDamageAssessment.map(
              o => parts += obsSubEventToDto(obs, ObsWaterDamageType.id, o)
            )
            // scalastyle:on line.size.limit
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

      // scalastyle:off line.size.limit
      Observation(
        id = dto.id,
        doneDate = dto.eventDate,
        doneBy = dto.relatedActors.map(_.actorId).headOption,
        affectedThing =
          dto.relatedObjects.map(e => StorageNodeDatabaseId(e.objectId)).headOption,
        registeredBy = dto.registeredBy,
        registeredDate = dto.registeredDate,
        eventType = EventType.fromEventTypeId(dto.eventTypeId),
        alcohol = fromMapObsSub[ObservationAlcohol](p, ObsAlcoholType.id),
        cleaning = fromMapObsSub[ObservationCleaning](p, ObsCleaningType.id),
        gas = fromMapObsSub[ObservationGas](p, ObsGasType.id),
        hypoxicAir = fromMapObsSub[ObservationHypoxicAir](p, ObsHypoxicAirType.id),
        lightingCondition =
          fromMapObsSub[ObservationLightingCondition](p, ObsLightingType.id),
        mold = fromMapObsSub[ObservationMold](p, ObsMoldType.id),
        pest = fromMapObsSub[ObservationPest](p, ObsPestType.id),
        relativeHumidity =
          fromMapObsSub[ObservationRelativeHumidity](p, ObsHumidityType.id),
        temperature = fromMapObsSub[ObservationTemperature](p, ObsTemperatureType.id),
        theftProtection = fromMapObsSub[ObservationTheftProtection](p, ObsTheftType.id),
        fireProtection = fromMapObsSub[ObservationFireProtection](p, ObsFireType.id),
        perimeterSecurity =
          fromMapObsSub[ObservationPerimeterSecurity](p, ObsPerimeterType.id),
        waterDamageAssessment =
          fromMapObsSub[ObservationWaterDamageAssessment](p, ObsWaterDamageType.id)
      )
      // scalastyle:on line.size.limit
    }

    // scalastyle:off cyclomatic.complexity method.length line.size.limit
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
          toBaseDtoNoChildren(owner, ObsLightingType.id, obs.note, obs.lightingCondition)

        case obs: ObservationCleaning =>
          toBaseDtoNoChildren(owner, ObsCleaningType.id, obs.note, obs.cleaning)

        case obs: ObservationGas =>
          toBaseDtoNoChildren(owner, ObsGasType.id, obs.note, obs.gas)

        case obs: ObservationMold =>
          toBaseDtoNoChildren(owner, ObsMoldType.id, obs.note, obs.mold)

        case obs: ObservationTheftProtection =>
          toBaseDtoNoChildren(owner, ObsTheftType.id, obs.note, obs.theftProtection)

        case obs: ObservationFireProtection =>
          toBaseDtoNoChildren(owner, ObsFireType.id, obs.note, obs.fireProtection)

        case obs: ObservationWaterDamageAssessment =>
          toBaseDtoNoChildren(
            owner,
            ObsWaterDamageType.id,
            obs.note,
            obs.waterDamageAssessment
          )

        case obs: ObservationPerimeterSecurity =>
          toBaseDtoNoChildren(
            owner,
            ObsPerimeterType.id,
            obs.note,
            obs.perimeterSecurity
          )

        case obs: ObservationPest =>
          toExtendedDto(
            base = owner,
            eventTypeId = ObsPestType.id,
            note = obs.note,
            maybeStr = obs.identification,
            ext = ObservationPestDto(
              lifeCycles = obs.lifecycles.map(LifeCyleConverters.lifecycleToDto)
            )
          )

        case obs: ObservationAlcohol =>
          toBaseDtoNoChildren(
            base = owner,
            eventTypeId = ObsAlcoholType.id,
            note = obs.note,
            maybeStr = obs.condition,
            maybeDouble = obs.volume
          )
      }
    } // scalastyle:on cyclomatic.complexity method.length line.size.limit

    /**
     * Specifically handles dto mapping for observations that does not have
     * custom properties in a separate table in the DB.
     */
    // scalastyle:off cyclomatic.complexity method.length
    def obsSubEventFromDto(dto: EventDto): ObservationSubEvent = {
      ObsSubEvents.unsafeFromId(dto.eventTypeId) match {
        case ObsAlcoholType =>
          ObservationAlcohol(dto.note, dto.valueString, dto.valueDouble)

        case ObsCleaningType =>
          ObservationCleaning(dto.note, dto.valueString)

        case ObsFireType =>
          ObservationFireProtection(dto.note, dto.valueString)

        case ObsGasType =>
          ObservationGas(dto.note, dto.valueString)

        case ObsLightingType =>
          ObservationLightingCondition(dto.note, dto.valueString)

        case ObsMoldType =>
          ObservationMold(dto.note, dto.valueString)

        case ObsPerimeterType =>
          ObservationPerimeterSecurity(dto.note, dto.valueString)

        case ObsTheftType =>
          ObservationTheftProtection(dto.note, dto.valueString)

        case ObsWaterDamageType =>
          ObservationWaterDamageAssessment(dto.note, dto.valueString)

        case ObsPestType =>
          val extDto = dto.asInstanceOf[ExtendedDto]
          val tmpDto = extDto.extension.asInstanceOf[ObservationPestDto]
          val lc     = tmpDto.lifeCycles.map(LifeCyleConverters.lifecycleFromDto)
          ObservationPest(dto.note, dto.valueString, lc)

        case ObsHumidityType =>
          val extDto = dto.asInstanceOf[ExtendedDto]
          val tmpDto = extDto.extension.asInstanceOf[ObservationFromToDto]
          val fromTo = FromToDouble(tmpDto.from, tmpDto.to)
          ObservationRelativeHumidity(dto.note, fromTo)

        case ObsHypoxicAirType =>
          val extDto = dto.asInstanceOf[ExtendedDto]
          val tmpDto = extDto.extension.asInstanceOf[ObservationFromToDto]
          val fromTo = FromToDouble(tmpDto.from, tmpDto.to)
          ObservationHypoxicAir(dto.note, fromTo)

        case ObsTemperatureType =>
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

      val temp     = toInterval(envReqDto.temperature, envReqDto.tempTolerance)
      val humidity = toInterval(envReqDto.airHumidity, envReqDto.airHumTolerance)
      val hypoxic  = toInterval(envReqDto.hypoxicAir, envReqDto.hypoxicTolerance)

      EnvRequirement(
        id = dto.id,
        doneDate = dto.eventDate,
        doneBy = dto.relatedActors.map(_.actorId).headOption,
        affectedThing =
          dto.relatedObjects.map(e => StorageNodeDatabaseId(e.objectId)).headOption,
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
        maybeLong = move.from,
        maybeStr = Option(move.objectType.name)
      )
    }

    def moveFromDto[A <: MoveEvent](
        dto: BaseEventDto
    )(init: (EventType, Option[StorageNodeDatabaseId], StorageNodeDatabaseId) => A): A = {
      val eventType = EventType.fromEventTypeId(dto.eventTypeId)
      val from      = dto.valueLong
      val to        = dto.relatedPlaces.head.placeId
      init(eventType, from, to)
    }

    def moveObjectToDto(mo: MoveObject): BaseEventDto = moveToDto(mo)

    def moveObjectFromDto(dto: BaseEventDto): MoveObject =
      moveFromDto(dto)(
        (eventType, from, to) =>
          MoveObject(
            id = dto.id,
            doneDate = dto.eventDate,
            doneBy = dto.relatedActors.map(_.actorId).headOption,
            affectedThing = dto.relatedObjects.map(e => ObjectId(e.objectId)).headOption,
            registeredBy = dto.registeredBy,
            registeredDate = dto.registeredDate,
            eventType = eventType,
            objectType =
              ObjectType.fromOptString(dto.valueString).getOrElse(CollectionObject),
            from = from,
            to = to
        )
      )

    def moveNodeToDto(mo: MoveNode): BaseEventDto = moveToDto(mo)

    def moveNodeFromDto(dto: BaseEventDto): MoveNode =
      moveFromDto(dto)(
        (eventType, from, to) =>
          MoveNode(
            id = dto.id,
            doneDate = dto.eventDate,
            doneBy = dto.relatedActors.map(_.actorId).headOption,
            affectedThing = dto.relatedObjects
              .map(e => StorageNodeDatabaseId(e.objectId))
              .headOption, // scalastyle:ignore
            registeredBy = dto.registeredBy,
            registeredDate = dto.registeredDate,
            eventType = eventType,
            from = from,
            to = to
        )
      )
  }

}
