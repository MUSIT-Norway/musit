package repositories.loan.dao

import java.sql.{Timestamp => JSqlTimestamp}

import models.loan.LoanType
import models.loan.event.{LentObject, ReturnedObject}
import no.uio.musit.models._
import no.uio.musit.time.Implicits._
import org.joda.time.DateTime
import play.api.db.slick.HasDatabaseConfigProvider
import play.api.libs.json.{JsValue, Json}
import repositories.shared.dao.ColumnTypeMappers
import slick.jdbc.JdbcProfile

/**
 * Defines the table definitions needed to work with analysis data.
 * Also includes a few helpful functions to convert between rows and types.
 */
trait Tables extends HasDatabaseConfigProvider[JdbcProfile] with ColumnTypeMappers {

  import profile.api._

  val loanTable       = TableQuery[LoanEventTable]
  val activeLoanTable = TableQuery[ActiveLoanTable]
  val lentObjectTable = TableQuery[LentObjectTable]

  // scalastyle:off line.size.limit
  type LoanEventRow = (
      Option[EventId],
      LoanType,
      Option[JSqlTimestamp],
      Option[ActorId],
      Option[JSqlTimestamp],
      MuseumId,
      Option[EventId],
      Option[ObjectUUID],
      Option[ExternalRef],
      Option[String],
      JsValue
  )
  type ActiveLoanRow = (Option[Long], MuseumId, ObjectUUID, EventId, DateTime)
  type LentObjectRow = (Option[Long], EventId, ObjectUUID)
  // scalastyle:on line.size.limit

  class LoanEventTable(
      val tag: Tag
  ) extends Table[LoanEventRow](tag, Some(SchemaName), LoanEventTableName) {

    val id             = column[EventId]("EVENT_ID", O.PrimaryKey, O.AutoInc)
    val typeId         = column[LoanType]("TYPE_ID")
    val eventDate      = column[Option[JSqlTimestamp]]("EVENT_DATE")
    val registeredBy   = column[Option[ActorId]]("REGISTERED_BY")
    val registeredDate = column[Option[JSqlTimestamp]]("REGISTERED_DATE")
    val museumId       = column[MuseumId]("MUSEUM_ID")
    val partOf         = column[Option[EventId]]("PART_OF")
    val objectUuid     = column[Option[ObjectUUID]]("OBJECT_UUID")
    val externalRef    = column[Option[ExternalRef]]("EXTERNAL_REF")
    val note           = column[Option[String]]("NOTE")
    val eventJson      = column[JsValue]("EVENT_JSON")

    // scalastyle:off method.name line.size.limit
    override def * =
      (
        id.?,
        typeId,
        eventDate,
        registeredBy,
        registeredDate,
        museumId,
        partOf,
        objectUuid,
        externalRef,
        note,
        eventJson
      )

    // scalastyle:on method.name line.size.limit
  }

  class ActiveLoanTable(
      val tag: Tag
  ) extends Table[ActiveLoanRow](tag, Some(SchemaName), ActiveLoanTableName) {

    val id         = column[Long]("ACTIVE_LOAN_ID", O.PrimaryKey, O.AutoInc)
    val museumId   = column[MuseumId]("MUSEUM_ID")
    val objectUuid = column[ObjectUUID]("OBJECT_UUID")
    val eventId    = column[EventId]("EVENT_ID")
    val returnDate = column[DateTime]("RETURN_DATE")

    // scalastyle:off method.name line.size.limit
    override def * = (id.?, museumId, objectUuid, eventId, returnDate)

    // scalastyle:on method.name line.size.limit
  }

  class LentObjectTable(
      val tag: Tag
  ) extends Table[LentObjectRow](tag, Some(SchemaName), LentObjectTableName) {

    val id         = column[Long]("LENT_OBJECT_ID", O.PrimaryKey, O.AutoInc)
    val eventId    = column[EventId]("EVENT_ID")
    val objectUuid = column[ObjectUUID]("OBJECT_UUID")

    // scalastyle:off method.name line.size.limit
    override def * = (id.?, eventId, objectUuid)

    // scalastyle:on method.name line.size.limit
  }

  private[dao] def asEventRowTuple(mid: MuseumId, lentObject: LentObject): LoanEventRow =
    (
      lentObject.id,
      lentObject.loanType,
      lentObject.eventDate,
      lentObject.registeredBy,
      lentObject.registeredDate,
      mid,
      lentObject.partOf,
      lentObject.objectId,
      lentObject.externalRef,
      lentObject.note,
      Json.toJson[LentObject](lentObject)
    )

  private[dao] def asEventRowTuple(
      mid: MuseumId,
      returnedObject: ReturnedObject
  ): LoanEventRow = (
    returnedObject.id,
    returnedObject.loanType,
    returnedObject.eventDate,
    returnedObject.registeredBy,
    returnedObject.registeredDate,
    mid,
    returnedObject.partOf,
    returnedObject.objectId,
    returnedObject.externalRef,
    returnedObject.note,
    Json.toJson[ReturnedObject](returnedObject)
  )
}
