package repositories.conservation.dao

import com.google.inject.{Inject, Singleton}
import models.conservation.events._
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.models.{ActorId, EventId, EventTypeId, ObjectUUID}
import no.uio.musit.repositories.DbErrorHandlers
import oracle.net.aso.e
import org.joda.time.DateTime
import play.api.db.slick.DatabaseConfigProvider
import repositories.conservation.DaoUtils
import repositories.shared.dao.ColumnTypeMappers

import scala.concurrent.ExecutionContext
@Singleton
class ObjectEventDao @Inject()(
    implicit
    val dbConfigProvider: DatabaseConfigProvider,
    val ec: ExecutionContext,
    val daoUtils: DaoUtils
) extends ConservationEventTableProvider
    with ColumnTypeMappers
    with ConservationTables
    with DbErrorHandlers {

  import profile.api._

  private val objectEventTable = TableQuery[ObjectEventTable]

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

  def getObjectEventIds(objectUuid: ObjectUUID): FutureMusitResult[Seq[EventId]] = {
    val action = for {
      oe <- objectEventTable
      e  <- eventTable
      if oe.objectUuid === objectUuid && oe.eventId === e.eventId && e.isDeleted === 0 && e.eventTypeId =!= ConservationProcess.eventTypeId
    } yield oe.eventId
    val res = action.result
    daoUtils
      .dbRun(res, s"An unexpected error occurred fetching eventIds object $objectUuid")
  }

  def getConservationProcessIdsAndCaseNumbersForObject(
      objectUuid: ObjectUUID
  ): FutureMusitResult[Seq[(EventId, Option[String], DateTime, ActorId)]] = {
    val action = for {
      oe <- objectEventTable
      e  <- eventTable
      if oe.objectUuid === objectUuid && oe.eventId === e.eventId && e.isDeleted === 0 && e.eventTypeId === ConservationProcess.eventTypeId
    } yield (oe.eventId, e.caseNumber, e.registeredDate, e.registeredBy)
    val res = action.result
    daoUtils.dbRun(
      res,
      s"An unexpected error occurred fetching conservationProcessIds for object $objectUuid"
    )
  }

  def getEventObjects(eventId: EventId): FutureMusitResult[Seq[ObjectUUID]] = {
    val action =
      objectEventTable.filter(oe => oe.eventId === eventId).map(oids => oids.objectUuid)
    val res = action.result
    daoUtils.dbRun(
      res,
      s"An unexpected error occurred fetching objects in getEventObjects for event $eventId"
    )
  }

  def getEventsForSpecificCpAndObject(
      cpId: EventId,
      objectUuid: ObjectUUID
  ): FutureMusitResult[Seq[(String, String)]] = {
    val action = for {
      oe <- objectEventTable
      e  <- eventTable
      et <- conservationTypeTable
      if oe.objectUuid === objectUuid && oe.eventId === e.eventId && e.isDeleted === 0 && e.partOf === cpId && e.eventTypeId === et.typeId
    } yield (et.noName, et.enName)
    val res = action.result
    daoUtils.dbRun(
      res,
      s"An unexpected error occurred fetching conservationEventIds for object $objectUuid"
    )
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
