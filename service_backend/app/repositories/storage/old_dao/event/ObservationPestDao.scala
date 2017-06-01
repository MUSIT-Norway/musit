package repositories.storage.old_dao.event

import com.google.inject.{Inject, Singleton}
import models.storage.event.dto.ObservationPestDto
import no.uio.musit.models.EventId
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.storage.old_dao.EventTables

import scala.concurrent.Future

@Singleton
class ObservationPestDao @Inject()(
    val dbConfigProvider: DatabaseConfigProvider
) extends EventTables {

  import profile.api._

  /**
   * The insertAction and getObservation are somewhat more complex than
   * necessary because I don't know how to remove the EventId field from the
   * lifecycle case class and still get it inserted using Slick. Please feel
   * free to remove the need for the EventId in the dto, that would clean this
   * up a bit.
   */
  def insertAction(parentId: EventId, obsDto: ObservationPestDto): DBIO[Int] = {
    // Need to enrich the lifecycles with the parentId
    val lifeCyclesWithEventId = obsDto.lifeCycles.map { lifeCycle =>
      lifeCycle.copy(eventId = Some(parentId))
    }
    (lifeCycleTable ++= lifeCyclesWithEventId).map { maybeInt =>
      maybeInt.fold(1)(identity)
    }
  }

  def getObservation(eventId: EventId): Future[Option[ObservationPestDto]] =
    db.run(lifeCycleTable.filter(lifeCycle => lifeCycle.eventId === eventId).result)
      .map {
        case Nil        => None
        case lifeCycles => Some(ObservationPestDto(lifeCycles))
      }

}
