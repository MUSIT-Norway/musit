package no.uio.musit.microservice.event.service

import no.uio.musit.microservice.event.dao.EventDao.EventBase
import no.uio.musit.microservice.event.dao.ObservationDAO
import no.uio.musit.microservice.event.domain.{Event, Observation}
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.utils.ErrorHelper
import slick.dbio._

object ObservationFactory extends EventFactory {
  def fromDatabase(id: Long, baseEventDto: EventBase): MusitFuture[Observation] = {
    ObservationDAO.getObservation(id)
      .toMusitFuture(ErrorHelper.badRequest(s"Unable to find observation with id: $id"))
      .musitFutureMap(observationDTO => Observation(baseEventDto, observationDTO))
  }

  def toDatabase(id: Long, event: Event): DBIO[Int] =
    ObservationDAO.insertAction(id, event.asInstanceOf[Observation])
}
