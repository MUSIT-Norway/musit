package no.uio.musit.repositories.events

import no.uio.musit.models._
import no.uio.musit.repositories.BaseColumnTypeMappers
import org.joda.time.DateTime
import play.api.db.slick.HasDatabaseConfigProvider
import play.api.libs.json.JsValue
import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape

import scala.reflect.ClassTag

/**
 * This trait defines the shared table structure for all DAO implementations
 * for MusitEvents.
 *
 * It's up to the implementation to define the name of the DB schema and table
 * to use. As well as the tupled type of a table row for the `EventRow` type.
 *
 * The function `eventTable` also needs to be defined.
 *
 * Typically, the trait is extended by a specialisation trait that describes
 * the events for a specific MUSIT module (e.g. StorageEventTableProvider in
 * service_backend).
 */
trait BaseEventTableProvider
    extends HasDatabaseConfigProvider[JdbcProfile]
    with BaseColumnTypeMappers {

  import profile.api._

  /** The name of the database schema */
  val schema: String

  /** The name of the table in the above database schema */
  val table: String

  /** The data type representing a ROW in the database table */
  type EventRow

  /** Provides a TableQuery handle into the table definition  */
  def eventTable: TableQuery[_ <: BaseEventTable[EventRow]]

  /**
   * All {{{BaseEventTableProvider}}} implementations must contain an overridden
   * class of {{{BaseEventTable}}} that defines the domain specific event table.
   */
  abstract class BaseEventTable[RowType: ClassTag](
      val tag: Tag
  ) extends Table[RowType](tag, Some(schema), table) {

    val eventId        = column[EventId]("EVENT_ID", O.PrimaryKey, O.AutoInc)
    val eventTypeId    = column[EventTypeId]("TYPE_ID")
    val museumId       = column[MuseumId]("MUSEUM_ID")
    val registeredBy   = column[ActorId]("REGISTERED_BY")
    val registeredDate = column[DateTime]("REGISTERED_DATE")
    val doneBy         = column[Option[ActorId]]("DONE_BY")
    val doneDate       = column[Option[DateTime]]("DONE_DATE")
    val partOf         = column[Option[EventId]]("PART_OF")
    val affectedUuid   = column[Option[String]]("AFFECTED_UUID")
    val note           = column[Option[String]]("NOTE")
    val eventJson      = column[JsValue]("EVENT_JSON")

    // scalastyle:off method.name
    def * : ProvenShape[RowType]

    // scalastyle:on method.name

  }

}
