package no.uio.musit.microservice.event.service

import no.uio.musit.microservice.event.dao.EventDao.EventBase
import no.uio.musit.microservice.event.dao.{ ControlDTO, ControlDao }
import no.uio.musit.microservice.event.domain.Control
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.utils.ErrorHelper

object ControlService extends EventService {

  def fromDatabase(id: Long, base: EventBase) = {
    ControlDao.getControl(id)
      .toMusitFuture(ErrorHelper.badRequest(s"Unable to find control with id: $id"))
      .musitFutureMap(controlDTO => Control(base, controlDTO))
  }

  def maybeActionCreator =
    Some((id, event) => {
      val control: Control = event.asInstanceOf[Control]
      val controlDTO = ControlDTO(Some(id), control.controlType)
      ControlDao.insertAction(controlDTO)
    })

}