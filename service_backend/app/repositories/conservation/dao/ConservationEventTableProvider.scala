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

  //If you modify this, remember to update the ugly copy below in the EventAccessors object!
  override type EventRow = (
      Option[EventId], // 1 - EventId
      EventTypeId, // 2 - EventTypeId
      MuseumId, // 3 - MuseumId
      ActorId, // 4 - RegisteredBy
      DateTime, // 5 - RegisteredDate
      Option[DateTime], // 6- UpdatedDate
      Option[EventId], //7 - PartOf
      Option[String], // 8 - Note
      Option[String], // 9 - CaseNumber,
      JsValue // 10 - EventJson,
      // Option[Int] // 11 - IsDeleted
  )

  def valEventId(row: EventRow)     = row._1
  def valEventTypeId(row: EventRow) = row._2

  def valMuseumId(row: EventRow) = row._3

  def valRegisteredBy(row: EventRow)   = row._4
  def valRegisteredDate(row: EventRow) = row._5

  def valJson(row: EventRow) = row._10

  def withPartOf(row: EventRow, partOf: Option[EventId]) = row.copy(_7 = partOf)

  //def isDeleted(row: EventRow) = row._11

  override lazy val eventTable = TableQuery[ConservationEventTable]

  /**
   * Representation of the MUSARK_CONSERVATION.EVENT table
   */
  class ConservationEventTable(val t: Tag) extends BaseEventTable[EventRow](t) {
    val caseNumber = column[Option[String]]("CASE_NUMBER")
    val isDeleted  = column[Option[Int]]("IS_DELETED")

    // scalastyle:off method.name
    def * : ProvenShape[EventRow] =
      (
        eventId.?,
        eventTypeId,
        museumId,
        registeredBy,
        registeredDate,
        updatedDate,
        partOf,
        note,
        caseNumber,
        eventJson
      )
    // scalastyle:on method.name
  }
}

/** TODO: Please make the need for the copy of EventRow to go away.
 *  Without having to spread incredibly ugly code like _.10 etc outside of this file
 */
object EventAccessors {
  type EventRow = (
      Option[EventId], // 1 - EventId
      EventTypeId, // 2 - EventTypeId
      MuseumId, // 3 - MuseumId
      ActorId, // 4 - RegisteredBy
      DateTime, // 5 - RegisteredDate
      Option[DateTime], // 6- UpdatedDate
      Option[EventId], //7 - PartOf
      Option[String], // 8 - Note
      Option[String], // 9 - CaseNumber
      JsValue // 10 - EventJson,
  )
  def valJson(row: EventRow) = row._10
}
