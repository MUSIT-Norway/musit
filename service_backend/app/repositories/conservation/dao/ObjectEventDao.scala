package repositories.conservation.dao

import com.google.inject.{Inject, Singleton}
import models.conservation.events._
import no.uio.musit.MusitResults.{MusitResult, MusitSuccess}
import no.uio.musit.models.{EventId, ObjectUUID}
import no.uio.musit.repositories.DbErrorHandlers
import play.api.db.slick.DatabaseConfigProvider
import repositories.shared.dao.ColumnTypeMappers

import scala.concurrent.{ExecutionContext, Future}
@Singleton
class ObjectEventDao @Inject()(
    implicit
    val dbConfigProvider: DatabaseConfigProvider,
    val ec: ExecutionContext
) extends ConservationEventTableProvider
    with ColumnTypeMappers
    with ConservationTables
    with DbErrorHandlers {

  import profile.api._

  private val objectEventTable = TableQuery[ObjectEventTable]

  def dbRun[T](res: DBIO[T], onErrorMsg: String): Future[MusitResult[T]] = {
    db.run(res)
      .map(MusitSuccess.apply)
      .recover(
        nonFatal(onErrorMsg)
      )
  }

  def insertAction(objectUuid: ObjectUUID, eventId: EventId): DBIO[Int] = {
    val action = objectEventTable += ObjectEvent(objectUuid, eventId)
    action
  }

  def getEventObjectsAction(eventId: EventId): DBIO[Seq[ObjectUUID]] = {
    val action =
      objectEventTable.filter(oe => oe.eventId === eventId).map(oe => oe.objectUuid)
    val res = action.result
    res

  }

  def getObjectEventIds(objectUuid: ObjectUUID): Future[MusitResult[Seq[EventId]]] = {
    val action =
      objectEventTable.filter(oe => oe.objectUuid === objectUuid).map(oe => oe.eventId)
    val res = action.result
    dbRun(res, s"An unexpected error occurred fetching object $objectUuid")
  }

  /**
   * an insert action for inserting into table objectEvent
   *
   * @param eventId the eventId
   * @param objectUuids a list of objects that relates to the eventId
   * @return a DBIO[Int] Number of rows inserted?
   */
  def insertObjectEventAction(
      eventId: EventId,
      objectUuids: Seq[ObjectUUID]
  ): DBIO[Int] = {
    val actions = objectUuids.map(oid => insertAction(oid, eventId))
    DBIO.sequence(actions).map(_.sum)
  }

  def deleteObjectEventAction(eventId: EventId): DBIO[Int] = {
    val q      = objectEventTable.filter(oe => oe.eventId === eventId)
    val action = q.delete
    action
  }

  def updateObjectEventAction(
      eventId: EventId,
      objectUuids: Seq[ObjectUUID]
  ): DBIO[Int] = {
    for {
      deleted  <- deleteObjectEventAction(eventId)
      inserted <- insertObjectEventAction(eventId, objectUuids)
    } yield inserted
  }

  /*def getEventListForObjectAction(objectUuid: ObjectUUID): DBIO[Seq[ObjectEvent]] = {
    val action = objectEventTable.filter(a => a.objectUuid === objectUuid)
    action
  }

  def getEventsForObjectAction(eventIds: Seq[EventId]: DBIO[Seq[ObjectEvent]] = {
    val action = objectEventTable.filter(a => a.eventId === eventIds)
    action
  }
   */
  /**
   * Locate all events related to the provided ObjectUUID.
   *
   * @param mid the MuseumId
   * @param objectUuid The ObjectUUID to find events' for
   * @return eventually a result with a list of events and their results
   */
  /* def getEventForObject(mid: MuseumId, objectUuid: ObjectUUID)(
      implicit currUsr: AuthenticatedUser
  ): Future[MusitResult[Seq[ConservationEvent]]] = {
    val eventsRes = for {
      eventIds <- db.run(getEventListForObjectAction(objectUuid))
      events   <- db.run(getEventsForObjectAction(eventIds))
    } yield MusitSuccess(events)

    eventsRes.recover(
      nonFatal(s"An unexpected error occurred fetching events for object $oid")
    )
  }*/

  /* def getEventForObject(objectUuid: ObjectUUID): Future[Seq[ObjectEvent]] = {
    val query = objectEventTable.filter(a => a.objectUuid === objectUuid)
    db.run(query.result)
  }*/

  private class ObjectEventTable(tag: Tag)
      extends Table[ObjectEvent](
        tag,
        Some(SchemaName),
        ObjectEventTableName
      ) {

    val objectUuid = column[ObjectUUID]("OBJECT_UUID")
    val eventId    = column[EventId]("EVENT_ID")

    val create = (
        objectUuid: ObjectUUID,
        eventId: EventId
    ) =>
      ObjectEvent(
        objectUuid = objectUuid,
        eventId = eventId
    )

    val destroy = (coe: ObjectEvent) =>
      Some(
        (
          coe.objectUuid,
          coe.eventId
        )
    )

    // scalastyle:off method.name
    def * = (objectUuid, eventId) <> (create.tupled, destroy)

    // scalastyle:on method.name

  }

}
