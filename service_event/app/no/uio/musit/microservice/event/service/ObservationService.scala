package no.uio.musit.microservice.event.service

import no.uio.musit.microservice.event.dao.EventDao.EventBase
import no.uio.musit.microservice.event.dao.{ ObservationDTO, ObservationDao }
import no.uio.musit.microservice.event.domain.Observation
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.utils.ErrorHelper

object ObservationService extends EventService {
  def fromDatabase(id: Long, baseEventDto: EventBase) = {
    ObservationDao.getObservation(id)
      .toMusitFuture(ErrorHelper.badRequest(s"Unable to find observation with id: $id"))
      .musitFutureMap(observationDTO => Observation(baseEventDto, observationDTO))
  }

  def maybeActionCreator = Some((id, event) => {
    val observation = event.asInstanceOf[Observation]
    val observationDTO: ObservationDTO = ObservationDTO(Some(id), observation.temperature)
    ObservationDao.insertAction(observationDTO)
  })
}
