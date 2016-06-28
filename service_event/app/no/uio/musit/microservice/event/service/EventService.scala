package no.uio.musit.microservice.event.service

import no.uio.musit.microservice.event.dao.EventDao.EventBase
import no.uio.musit.microservice.event.domain.{ Event, Event$ }
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import slick.dbio._

trait EventService {
  def fromDatabase(id: Long, baseEventDto: EventBase): MusitFuture[Event]
  def maybeActionCreator: Option[(Long, Event) => DBIO[Int]]
}

