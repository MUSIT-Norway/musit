package no.uio.musit.microservice.event.service

/*#OLD
import no.uio.musit.microservice.event.dao.EventDao.{ EventBaseDto, EventBaseDto$ }
import no.uio.musit.microservice.event.domain.{ Event, Move }
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import slick.dbio._

import scala.concurrent.Future

object MoveService extends SimpleService {

  override def fromDatabase(id: Long, base: EventBaseDto) =
    Future.successful(Right(Move(Some(id), base.links, base.note, None)))

}
*/ 