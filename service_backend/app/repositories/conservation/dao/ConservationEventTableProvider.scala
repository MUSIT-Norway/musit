package repositories.conservation.dao

import no.uio.musit.models._
import no.uio.musit.repositories.events.BaseEventTableProvider
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json.JsValue
import repositories.shared.dao.ColumnTypeMappers
import slick.lifted.ProvenShape

private[dao] trait ConservationEventTableProvider
    extends BaseEventTableProvider
    with ColumnTypeMappers {

  private val logger = Logger(classOf[ConservationEventTableProvider])

  import profile.api._

  override val schema = SchemaName
  override val table  = ConservationEventTableName

  override type EventRow = (
      Option[EventId],
      EventTypeId,
      MuseumId,
      ActorId,
      DateTime,
      Option[ActorId],
      Option[DateTime],
      Option[DateTime],
      Option[EventId],
      Option[String], // MusitUUID
      Option[String],
      Option[String],
      JsValue
  )

  override lazy val eventTable = TableQuery[ConservationEventTable]

  /**
   * Representation of the MUSARK_CONSERVATION.EVENT table
   */
  class ConservationEventTable(val t: Tag) extends BaseEventTable[EventRow](t) {
    val caseNumber = column[Option[String]]("CASE_NUMBER")

    // scalastyle:off method.name
    def * : ProvenShape[EventRow] =
      (
        eventId.?,
        eventTypeId,
        museumId,
        registeredBy,
        registeredDate,
        doneBy,
        doneDate,
        updatedDate,
        partOf,
        affectedUuid,
        note,
        caseNumber,
        eventJson
      )

    // scalastyle:on method.name

  }
}
