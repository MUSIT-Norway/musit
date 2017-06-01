package repositories.storage.old_dao.event

import com.google.inject.{Inject, Singleton}
import models.storage.event.EventTypeId
import models.storage.event.dto.{EventRoleObject, EventRolePlace}
import no.uio.musit.models.{EventId, MuseumId, StorageNodeDatabaseId}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.storage.old_dao.EventTables

import scala.concurrent.Future

@Singleton
class EventPlacesAsObjectsDao @Inject()(
    val dbConfigProvider: DatabaseConfigProvider
) extends EventTables {

  val logger = Logger(classOf[EventPlacesAsObjectsDao])

  import profile.api._

  def insertObjects(
      eventId: EventId,
      relatedObjects: Seq[EventRoleObject]
  ): DBIO[Option[Int]] = {
    val relObjectsAsPlaces = relatedObjects.map { ero =>
      EventRolePlace(Some(eventId), ero.roleId, ero.objectId, ero.eventTypeId)
    }
    placesAsObjectsTable ++= relObjectsAsPlaces
  }

  def getRelatedObjectsAction(
      eventId: EventId
  ): DBIO[Seq[EventRoleObject]] = {
    placesAsObjectsTable.filter(_.eventId === eventId).result.map { places =>
      places.map { place =>
        EventRoleObject(place.eventId, place.roleId, place.placeId, place.eventTypeId)
      }
    }
  }

  def getRelatedObjects(mid: MuseumId, eventId: EventId): Future[Seq[EventRoleObject]] = {
    val query = getRelatedObjectsAction(eventId)
    db.run(query).map { objects =>
      logger.debug(s"Found ${objects.size} places")
      objects
    }
  }

  def latestEventIdFor(
      mid: MuseumId,
      nodeId: StorageNodeDatabaseId,
      eventTypeId: EventTypeId
  ): Future[Option[EventId]] = {
    val queryMax = placesAsObjectsTable.filter { erp =>
      erp.placeId === nodeId && erp.eventTypeId === eventTypeId
    }.map(_.eventId).max.result

    db.run(queryMax)
  }

  def latestEventIdsForNode(
      mid: MuseumId,
      nodeId: StorageNodeDatabaseId,
      eventTypeId: EventTypeId,
      limit: Option[Int] = None
  ): Future[Seq[EventId]] = {
    val q = placesAsObjectsTable.filter { erp =>
      erp.placeId === nodeId && erp.eventTypeId === eventTypeId
    }.sortBy(_.eventId.desc).map(_.eventId)

    val query = limit.map {
      case l: Int if l > 0   => q.take(l)
      case l: Int if l == -1 => q
      case l: Int            => q.take(50)
    }.getOrElse(q).result
    db.run(query)
  }

}
