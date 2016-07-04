package no.uio.musit.microservice.event.service

import no.uio.musit.microservice.event.domain.{Event, BaseEventProps$}
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import slick.dbio._

/*#OLD
trait EventService {
  def fromDatabase(id: Long, baseEventDto: BaseEventProps): MusitFuture[Event]
  def maybeActionCreator: Option[(Long, Event) => DBIO[Int]]
}
*/

