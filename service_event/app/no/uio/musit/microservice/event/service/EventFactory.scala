package no.uio.musit.microservice.event.service

import no.uio.musit.microservice.event.dao.EventDao.EventBase
import no.uio.musit.microservice.event.domain.{ Event, Event$ }
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import slick.dbio._

/**
 * Created by jarlandre on 28/06/16.
 */
trait EventFactory {
  def fromDatabase(id: Long, baseEventDto: EventBase): MusitFuture[Event]
  def toDatabase(id: Long, event: Event): DBIO[Int]
}
