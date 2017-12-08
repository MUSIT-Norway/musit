package repositories.conservation.dao

import com.google.inject.{Inject, Singleton}
import models.conservation.events._
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.models.{EventId, FileId, ObjectUUID}
import no.uio.musit.repositories.DbErrorHandlers
import play.api.db.slick.DatabaseConfigProvider
import repositories.conservation.DaoUtils
import repositories.shared.dao.ColumnTypeMappers

import scala.concurrent.ExecutionContext
@Singleton
class EventDocumentDao @Inject()(
    implicit
    val dbConfigProvider: DatabaseConfigProvider,
    val ec: ExecutionContext,
    val daoUtils: DaoUtils
) extends ConservationEventTableProvider
    with ColumnTypeMappers
    with ConservationTables
    with DbErrorHandlers {

  import profile.api._

  private val eventDocumentTable = TableQuery[EventDocumentTable]

  def insertAction(fid: FileId, eventId: EventId): DBIO[Int] = {
    val action = eventDocumentTable += EventDocument(eventId, fid)
    action
  }

  /**
   * an insert action for inserting into table objectEvent
   *
   * @param eventId the eventId
   * @param fileIds a list of objects that relates to the eventId
   * @return a DBIO[Int] Number of rows inserted?
   */
  def insertDocumentAction(
      eventId: EventId,
      fileIds: Seq[FileId]
  ): DBIO[Int] = {
    val actions = fileIds.map(fid => insertAction(fid, eventId))
    DBIO.sequence(actions).map(_.sum)
  }

  def deleteDocumentAction(eventId: EventId): DBIO[Int] = {
    val q      = eventDocumentTable.filter(oe => oe.eventId === eventId)
    val action = q.delete
    action
  }

  def updateDocumentAction(
      eventId: EventId,
      fileIds: Seq[FileId]
  ): DBIO[Int] = {
    for {
      deleted  <- deleteDocumentAction(eventId)
      inserted <- insertDocumentAction(eventId, fileIds)
    } yield inserted
  }

  def getDocuments(eventId: EventId): FutureMusitResult[Seq[FileId]] = {
    val action =
      eventDocumentTable.filter(oe => oe.eventId === eventId).map(fids => fids.fileId)
    val res = action.result
    daoUtils.dbRun(
      res,
      s"An unexpected error occurred fetching objects in getDocuments for event $eventId"
    )
  }

  private class EventDocumentTable(tag: Tag)
      extends Table[EventDocument](
        tag,
        Some(SchemaName),
        EventDocumentTableName
      ) {

    val eventId = column[EventId]("EVENT_ID")
    val fileId  = column[FileId]("FILE_ID")

    val create = (
        eventId: EventId,
        fileId: FileId
    ) =>
      EventDocument(
        eventId = eventId,
        fileId = fileId
    )

    val destroy = (ed: EventDocument) =>
      Some(
        (
          ed.eventId,
          ed.fileId
        )
    )

    // scalastyle:off method.name
    def * = (eventId, fileId) <> (create.tupled, destroy)

    // scalastyle:on method.name

  }
}
