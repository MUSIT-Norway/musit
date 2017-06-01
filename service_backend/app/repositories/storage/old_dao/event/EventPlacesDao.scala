package repositories.storage.old_dao.event

import com.google.inject.{Inject, Singleton}
import models.storage.event.dto.EventRolePlace
import no.uio.musit.models.{EventId, MuseumId}
import play.api.db.slick.DatabaseConfigProvider
import repositories.storage.old_dao.EventTables

import scala.concurrent.Future

@Singleton
class EventPlacesDao @Inject()(
    val dbConfigProvider: DatabaseConfigProvider
) extends EventTables {

  import profile.api._

  def getRelatedPlacesAction(
      eventId: EventId
  ): DBIO[Seq[EventRolePlace]] = {
    eventPlacesTable.filter(evt => evt.eventId === eventId).result
  }

  def insertPlaces(
      eventId: EventId,
      relatedPlaces: Seq[EventRolePlace]
  ): DBIO[Option[Int]] = {
    val relPlaces = relatedPlaces.map(_.copy(eventId = Some(eventId)))
    eventPlacesTable ++= relPlaces
  }

  def getRelatedPlaces(mid: MuseumId, eventId: EventId): Future[Seq[EventRolePlace]] = {
    val query = getRelatedPlacesAction(eventId)
    db.run(query)
  }

}
