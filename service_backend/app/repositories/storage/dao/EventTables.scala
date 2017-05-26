package repositories.storage.dao

import models.storage.event.EventTypeId
import no.uio.musit.models.ObjectTypes.ObjectType
import no.uio.musit.models.{ActorId, EventId, MuseumId}
import org.joda.time.DateTime
import play.api.Logger
import play.api.db.slick.HasDatabaseConfigProvider
import play.api.libs.json.JsValue
import repositories.shared.dao.{ColumnTypeMappers, StorageTables}
import slick.jdbc.JdbcProfile

private[dao] trait EventTables
    extends HasDatabaseConfigProvider[JdbcProfile]
    with ColumnTypeMappers {

  private val logger = Logger(classOf[StorageTables])

  import profile.api._

  protected val storageEventTable = TableQuery[EventTable]

  type EventRow =
    (
        Option[EventId],
        EventTypeId,
        Option[MuseumId],
        Option[DateTime],
        Option[ActorId],
        Option[DateTime],
        Option[EventId],
        Option[String],
        Option[ObjectType],
        Option[String],
        JsValue
    )

  class EventTable(
      val tag: Tag
  ) extends Table[EventRow](tag, SchemaName, StorageEventTable) {

    val eventId        = column[EventId]("EVENT_ID", O.PrimaryKey, O.AutoInc)
    val eventTypeId    = column[EventTypeId]("TYPE_ID")
    val museumId       = column[Option[MuseumId]]("MUSEUM_ID")
    val eventDate      = column[Option[DateTime]]("EVENT_DATE")
    val registeredBy   = column[Option[ActorId]]("REGISTERED_BY")
    val registeredDate = column[Option[DateTime]]("REGISTERED_DATE")
    val partOf         = column[Option[EventId]]("PART_OF")
    val affectedUuid   = column[Option[String]]("AFFECTED_UUID")
    val affectedType   = column[Option[ObjectType]]("AFFECTED_TYPE")
    val note           = column[Option[String]]("NOTE")
    val eventJson      = column[JsValue]("EVENT_JSON")

    // scalastyle:off method.name
    def * =
      (
        eventId.?,
        eventTypeId,
        museumId,
        eventDate,
        registeredBy,
        registeredDate,
        partOf,
        affectedUuid,
        affectedType,
        note,
        eventJson
      )

    // scalastyle:on method.name

  }

}
