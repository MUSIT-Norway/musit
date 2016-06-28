package no.uio.musit.microservice.event.service

import no.uio.musit.microservice.event.dao.EventDao.EventBase
import no.uio.musit.microservice.event.dao.ControlDAO
import no.uio.musit.microservice.event.domain.{ Control, Event }
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.microservices.common.utils.ErrorHelper
import slick.dbio._

object ControlFactory extends EventFactory {
  def fromDatabase(id: Long, baseEventDto: EventBase): MusitFuture[Control] = {
    ControlDAO.getControl(id)
      .toMusitFuture(ErrorHelper.badRequest(s"Unable to find control with id: $id"))
      .musitFutureMap(controlDTO => Control(baseEventDto, controlDTO))
  }

  def toDatabase(id: Long, event: Event): DBIO[Int] =
    ControlDAO.insertAction(id, event.asInstanceOf[Control])
}