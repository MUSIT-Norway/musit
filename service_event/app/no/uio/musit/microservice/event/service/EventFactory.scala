package no.uio.musit.microservice.event.service

import no.uio.musit.microservice.event.dao.EventDao.EventBase
import no.uio.musit.microservice.event.domain.{ Event, Event$ }
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import slick.dbio._

trait EventFactory {
  def fromDatabase(id: Long, baseEventDto: EventBase): MusitFuture[Event]
  def maybeActionCreator: Option[(Long, Event) => DBIO[Int]]
}

abstract class SimpleFactory extends EventFactory {
  def maybeActionCreator: Option[(Long, Event) => DBIO[Int]] = None
}