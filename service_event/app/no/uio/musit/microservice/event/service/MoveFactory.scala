package no.uio.musit.microservice.event.service

import no.uio.musit.microservice.event.dao.EventDao.EventBase
import no.uio.musit.microservice.event.domain.{ Event, Move }
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import slick.dbio._

import scala.concurrent.Future

object MoveFactory extends SimpleFactory {

  override def fromDatabase(id: Long, base: EventBase) =
    Future.successful(Right(Move(Some(id), base.links, base.note)))

}
