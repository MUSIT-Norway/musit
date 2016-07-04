package no.uio.musit.microservice.event.service

import no.uio.musit.microservice.event.dao.EnvRequirementDAO.EnvRequirementDto
import no.uio.musit.microservice.event.dao.EventDao.EventBaseDto
import no.uio.musit.microservice.event.domain.EnvRequirement
import no.uio.musit.microservice.event.dao.EnvRequirementDAO
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.utils.ErrorHelper

/**
 * Created by ellenjo on 6/30/16.
 */

object EnvRequirementService extends EventService {
  def fromDatabase(id: Long, baseEventDto: EventBaseDto) = {
    EnvRequirementDAO.getEnvRequirement(id)
      .toMusitFuture(ErrorHelper.badRequest(s"Unable to find observation with id: $id"))
      .musitFutureMap(envReq => EnvRequirement(baseEventDto.id, baseEventDto.links, baseEventDto.note,
        envReq.temperature, envReq.tempInterval, envReq.airHumidity,
        envReq.airHumInterval, envReq.hypoxicAir, envReq.hypoxicInterval, envReq.cleaning,
        envReq.light))
  }

  def maybeActionCreator = Some((id, event) => {
    val envReq = event.asInstanceOf[EnvRequirement]
    val envRequirementDto: EnvRequirementDto = EnvRequirementDto(Some(id), envReq.temperature, envReq.temperatureInterval, envReq.airHumidity,
      envReq.airHumidityInterval, envReq.hypoxicAir, envReq.hypoxicAirInterval, envReq.cleaning,
      envReq.light)
    EnvRequirementDAO.insertAction(envRequirementDto.copy(id = Some(id)))
  })
}

