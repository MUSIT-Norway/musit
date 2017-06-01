package repositories.storage.old_dao.event

import com.google.inject.{Inject, Singleton}
import models.storage.event.dto.EventRoleActor
import no.uio.musit.models.EventId
import play.api.db.slick.DatabaseConfigProvider
import repositories.storage.old_dao.EventTables

import scala.concurrent.Future

@Singleton
class EventActorsDao @Inject()(
    val dbConfigProvider: DatabaseConfigProvider
) extends EventTables {

  import profile.api._

  def insertActors(
      eventId: EventId,
      relatedActors: Seq[EventRoleActor]
  ): DBIO[Option[Int]] = {
    val relActors = relatedActors.map(_.copy(eventId = Some(eventId)))
    eventActorsTable ++= relActors
  }

  def getRelatedActorsAction(eventId: EventId): DBIO[Seq[EventRoleActor]] = {
    eventActorsTable.filter(evt => evt.eventId === eventId).result
  }

  def getRelatedActors(eventId: EventId): Future[Seq[EventRoleActor]] = {
    val query = getRelatedActorsAction(eventId)
    db.run(query)
  }

}
