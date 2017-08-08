package repositories.analysis.dao

import models.analysis.AnalysisStatuses.AnalysisStatus
import models.analysis.events.AnalysisTypeId
import no.uio.musit.repositories.events.BaseEventTableProvider
import no.uio.musit.models._
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json.JsValue
import repositories.shared.dao.ColumnTypeMappers
import slick.lifted.ProvenShape

private[dao] trait AnalysisEventTableProvider
    extends BaseEventTableProvider
    with ColumnTypeMappers {

  private val logger = Logger(classOf[AnalysisEventTableProvider])

  import profile.api._

  override val schema = SchemaName
  override val table  = AnalysisEventTableName

  override type EventRow = (
      Option[EventId],
      AnalysisTypeId,
      MuseumId,
      ActorId,
      DateTime,
      Option[ActorId],
      Option[DateTime],
      Option[DateTime],
      Option[EventId],
      Option[String], // MusitUUID
      Option[String],
      Option[AnalysisStatus],
      Option[CaseNumbers],
      JsValue
  )

  type ResultRow =
    (EventId, MuseumId, Option[ActorId], Option[DateTime], JsValue)

  override lazy val eventTable = TableQuery[AnalysisEventTable]
  val resultTable              = TableQuery[AnalysisResultTable]

  /**
   * Representation of the MUSARK_ANALYSIS.EVENT table
   */
  class AnalysisEventTable(val t: Tag) extends BaseEventTable[EventRow](t) {
    val caseNumbers = column[Option[CaseNumbers]]("CASE_NUMBERS")
    val status      = column[Option[AnalysisStatus]]("STATUS")

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
        status,
        caseNumbers,
        eventJson
      )

    // scalastyle:on method.name

  }

  /**
   * Representation of the MUSARK_ANALUSIS.RESULT table
   */
  class AnalysisResultTable(
      val tag: Tag
  ) extends Table[ResultRow](tag, Some(SchemaName), AnalysisResultTableName) {

    val eventId        = column[EventId]("EVENT_ID", O.PrimaryKey)
    val museumId       = column[MuseumId]("MUSEUM_ID")
    val registeredBy   = column[Option[ActorId]]("REGISTERED_BY")
    val registeredDate = column[Option[DateTime]]("REGISTERED_DATE")
    val resultJson     = column[JsValue]("RESULT_JSON")

    // scalastyle:off method.name
    def * = (eventId, museumId, registeredBy, registeredDate, resultJson)

    // scalastyle:on method.name
  }

}
