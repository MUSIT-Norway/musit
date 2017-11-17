package repositories.conservation.dao

import no.uio.musit.MusitResults.{MusitError, MusitResult, MusitSuccess}
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.models._
import no.uio.musit.repositories.events.BaseEventTableProvider
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json.JsValue
import repositories.shared.dao.ColumnTypeMappers
import slick.lifted.ProvenShape

import scala.concurrent.ExecutionContext

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

  def valEventId(row: EventRow)     = row._1
  def valEventTypeId(row: EventRow) = row._2

  def valMuseumId(row: EventRow) = row._3

  def valRegisteredBy(row: EventRow)   = row._4
  def valRegisteredDate(row: EventRow) = row._5

  def valDoneBy(row: EventRow)   = row._6
  def valDoneDate(row: EventRow) = row._7

  def valAffectedThing(row: EventRow) = row._10
  def valJson(row: EventRow)          = row._13

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
