package repositories.storage.old_dao.event

import com.google.inject.{Inject, Singleton}
import models.storage.event.EventTypeId
import models.storage.event.dto.EventRoleObject
import no.uio.musit.models.{EventId, ObjectId}
import play.api.db.slick.DatabaseConfigProvider
import repositories.storage.old_dao.EventTables

import scala.concurrent.Future

@Singleton
class EventObjectsDao @Inject()(
    val dbConfigProvider: DatabaseConfigProvider
) extends EventTables {

  import profile.api._

  def insertObjects(
      eventId: EventId,
      relatedObjects: Seq[EventRoleObject]
  ): DBIO[Option[Int]] = {
    val relObjects = relatedObjects.map(_.copy(eventId = Some(eventId)))
    eventObjectsTable ++= relObjects
  }

  def getRelatedObjectsAction(eventId: EventId): DBIO[Seq[EventRoleObject]] = {
    eventObjectsTable.filter(_.eventId === eventId).result
  }

  def getRelatedObjects(eventId: EventId): Future[Seq[EventRoleObject]] = {
    val query = getRelatedObjectsAction(eventId)
    db.run(query)
  }

  def latestEventIdsForObject(
      objectId: ObjectId,
      eventTypeId: EventTypeId,
      limit: Option[Int] = None
  ): Future[Seq[EventId]] = {
    val q = eventObjectsTable.filter { erp =>
      erp.objectId === objectId && erp.eventTypeId === eventTypeId
    }.sortBy(_.eventId.desc).map(_.eventId)

    val query = limit.map {
      case l: Int if l > 0   => q.take(l)
      case l: Int if l == -1 => q
      case l: Int            => q.take(50)
    }.getOrElse(q).result
    db.run(query)
  }

}
