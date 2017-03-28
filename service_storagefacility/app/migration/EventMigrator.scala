package migration

import com.google.inject.Singleton
import models.storage.event.EventTypeRegistry.TopLevelEvents.ControlEventType
import models.storage.event.control.Control
import models.storage.event.control.ControlAttributes._
import models.storage.event.observation.Observation
import models.storage.event.envreq.EnvRequirement
import models.storage.event.move._
import models.storage.event.dto.{BaseEventDto, DtoConverters}
import models.storage.event.observation.ObservationAttributes._
import models.storage.event.old.control.{Control => OldControl}
import models.storage.event.old.control.ControlSubEvents.{
  ControlAlcohol => OldCtrlAlcohol,
  ControlCleaning => OldCtrlCleaning,
  ControlGas => OldCtrlCleaning,
  ControlHypoxicAir => OldCtrlHypoxic,
  ControlLightingCondition => OldCtrlLight,
  ControlMold => OldCtrlMold,
  ControlPest => OldCtrlPest,
  ControlRelativeHumidity => OldCtrlHum,
  ControlTemperature => OldCtrlTemp
}
import models.storage.event.old.observation.{Observation => OldObservation}
import models.storage.event.old.observation.ObservationSubEvents.{
  ObservationAlcohol => OldObsAlcohol,
  ObservationCleaning => OldObsCleaning,
  ObservationFireProtection => OldObsFire,
  ObservationGas => OldObsGas,
  ObservationHypoxicAir => OldObsHypoxic,
  ObservationLightingCondition => OldObsLight,
  ObservationMold => OldObsMold,
  ObservationPerimeterSecurity => OldOpsSec,
  ObservationPest => OldObsPest,
  ObservationRelativeHumidity => OldObsHum,
  ObservationTemperature => OldObsTemp,
  ObservationTheftProtection => OldObsTheft,
  ObservationWaterDamageAssessment => OldObsWater
}
import models.storage.event.old.envreq.{EnvRequirement => OldEnvReq}
import models.storage.event.old.move.{
  MoveNode => OldMoveEvent,
  MoveObject => OldMoveObject
}
import no.uio.musit.models.MuseumId
import play.api.Logger
import repositories.storage.dao.events.{ControlDao, EnvReqDao, MoveDao, ObservationDao}
import repositories.storage.old_dao.event.EventDao
import services.old.StorageNodeService

// scalastyle:off
@Singleton
class EventMigrator(
    oldEventDao: EventDao,
    oldNodeService: StorageNodeService,
    ctlDao: ControlDao,
    obsDao: ObservationDao,
    envDao: EnvReqDao,
    movDao: MoveDao
) {

  val logger = Logger(classOf[EventMigrator])

  val dummyMuseumId = MuseumId(666)

  def obsOldToNew(observations: Seq[OldObservation]): Seq[Observation] = {
    observations.map { old =>
      Observation(
        id = old.id,
        doneBy = old.doneBy,
        doneDate = old.doneDate,
        affectedThing = old.affectedThing,
        registeredBy = old.registeredBy,
        registeredDate = old.registeredDate,
        eventType = old.eventType,
        alcohol = old.alcohol.map { o =>
          ObservationAlcohol(o.note, o.condition, o.volume)
        },
        cleaning = old.cleaning.map { o =>
          ObservationCleaning(o.note, o.cleaning)
        },
        gas = old.gas.map { o =>
          ObservationGas(o.note, o.gas)
        },
        hypoxicAir = old.hypoxicAir.map { o =>
          ObservationHypoxicAir(o.note, o.range)
        },
        lightingCondition = old.lightingCondition.map { o =>
          ObservationLightingCondition(o.note, o.lightingCondition)
        },
        mold = old.mold.map { o =>
          ObservationMold(o.note, o.mold)
        },
        pest = old.pest.map { o =>
          ObservationPest(o.note, o.identification, o.lifecycles)
        },
        relativeHumidity = old.relativeHumidity.map { o =>
          ObservationRelativeHumidity(o.note, o.range)
        },
        temperature = old.temperature.map { o =>
          ObservationTemperature(o.note, o.range)
        },
        theftProtection = old.theftProtection.map { o =>
          ObservationTheftProtection(o.note, o.theftProtection)
        },
        fireProtection = old.fireProtection.map { o =>
          ObservationFireProtection(o.note, o.fireProtection)
        },
        perimeterSecurity = old.perimeterSecurity.map { o =>
          ObservationPerimeterSecurity(o.note, o.perimeterSecurity)
        },
        waterDamageAssessment = old.waterDamageAssessment.map { o =>
          ObservationWaterDamageAssessment(o.note, o.waterDamageAssessment)
        }
      )
    }
  }

  def ctrlOldToNew(ctrls: Seq[OldControl]): Seq[Control] = {
    ctrls.map { old =>
      Control(
        id = old.id,
        doneBy = old.doneBy,
        doneDate = old.doneDate,
        affectedThing = old.affectedThing,
        registeredBy = old.registeredBy,
        registeredDate = old.registeredDate,
        eventType = old.eventType,
        alcohol = old.alcohol.map { c =>
          ControlAlcohol(
            ok = c.ok,
            observation = c.observation.map { o =>
              ObservationAlcohol(
                note = o.note,
                condition = o.condition,
                volume = o.volume
              )
            }
          )
        },
        cleaning = old.cleaning.map { c =>
          ControlCleaning(
            ok = c.ok,
            observation = c.observation.map { o =>
              ObservationCleaning(
                note = o.note,
                cleaning = o.cleaning
              )
            }
          )
        },
        gas = old.gas.map { c =>
          ControlGas(
            ok = c.ok,
            observation = c.observation.map { o =>
              ObservationGas(
                note = o.note,
                gas = o.gas
              )
            }
          )
        },
        hypoxicAir = old.hypoxicAir.map { c =>
          ControlHypoxicAir(
            ok = c.ok,
            observation = c.observation.map { o =>
              ObservationHypoxicAir(
                note = o.note,
                range = o.range
              )
            }
          )
        },
        lightingCondition = old.lightingCondition.map { c =>
          ControlLightingCondition(
            ok = c.ok,
            observation = c.observation.map { o =>
              ObservationLightingCondition(
                note = o.note,
                lightingCondition = o.lightingCondition
              )
            }
          )
        },
        mold = old.mold.map { c =>
          ControlMold(
            ok = c.ok,
            observation = c.observation.map { o =>
              ObservationMold(
                note = o.note,
                mold = o.mold
              )
            }
          )
        },
        pest = old.pest.map { c =>
          ControlPest(
            ok = c.ok,
            observation = c.observation.map { o =>
              ObservationPest(
                note = o.note,
                identification = o.identification,
                lifecycles = o.lifecycles
              )
            }
          )
        },
        relativeHumidity = old.relativeHumidity.map { c =>
          ControlRelativeHumidity(
            ok = c.ok,
            observation = c.observation.map { o =>
              ObservationRelativeHumidity(
                note = o.note,
                range = o.range
              )
            }
          )
        },
        temperature = old.temperature.map { c =>
          ControlTemperature(
            ok = c.ok,
            observation = c.observation.map { o =>
              ObservationTemperature(
                note = o.note,
                range = o.range
              )
            }
          )
        }
      )
    }
  }

  def migrateControls = {
    val eventualControls = oldEventDao.getAllEvents(dummyMuseumId, ControlEventType)(
      dto => DtoConverters.CtrlConverters.controlFromDto(dto.asInstanceOf[BaseEventDto])
    )

    eventualControls.map { ctrls =>
      val affectedNodeIds = ctrls.flatMap(_.affectedThing)
      oldNodeService
    }

  }

}
// scalastyle:on
