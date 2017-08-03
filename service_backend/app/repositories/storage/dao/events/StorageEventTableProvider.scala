package repositories.storage.dao.events

import no.uio.musit.repositories.events.BaseEventTableProvider
import no.uio.musit.models.ObjectTypes.ObjectType
import no.uio.musit.models._
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json.JsValue
import repositories.shared.dao.ColumnTypeMappers
import repositories.storage.dao.{SchemaName, StorageEventTable}
import slick.lifted.ProvenShape

private[dao] trait StorageEventTableProvider
    extends BaseEventTableProvider
    with ColumnTypeMappers {

  private val logger = Logger(classOf[StorageEventTableProvider])

  import profile.api._

  override val schema: String = SchemaName
  override val table: String  = StorageEventTable

  override type EventRow =
    (
        Option[EventId],
        EventTypeId,
        MuseumId,
        ActorId,
        DateTime,
        Option[DateTime],
        Option[EventId],
        Option[String], // MusitUUID
        Option[ObjectType],
        Option[String],
        JsValue
    )

  override lazy val eventTable = TableQuery[StorageEventTable]

  class StorageEventTable(val t: Tag) extends BaseEventTable[EventRow](t) {

    val affectedType = column[Option[ObjectType]]("AFFECTED_TYPE")

    // scalastyle:off method.name
    def * : ProvenShape[EventRow] =
      (
        eventId.?,
        eventTypeId,
        museumId,
        registeredBy,
        registeredDate,
        doneDate,
        partOf,
        affectedUuid,
        affectedType,
        note,
        eventJson
      )

    // scalastyle:on method.name
  }

}
