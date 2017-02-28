package repositories.dao

import java.sql.{Timestamp => JSqlTimestamp}

import models.events.{AnalysisEvent, Category, EventTypeId}
import no.uio.musit.models.{ActorId, EventId}
import no.uio.musit.time.DateTimeImplicits
import play.api.db.slick.HasDatabaseConfigProvider
import play.api.libs.json.{JsValue, Json}
import slick.driver.JdbcProfile

trait Tables extends HasDatabaseConfigProvider[JdbcProfile]
    with ColumnTypeMappers
    with DateTimeImplicits {

  import driver.api._

  val eventTypeTable = TableQuery[AnalysisEventTypeTable]
  val eventTable = TableQuery[AnalysisEventTable]

  type EventTypeRow = (Int, EventTypeId, Category, String, Option[String], JsValue)

  /**
   * Representation of the MUSARK_ANALYSIS.EVENT_TYPE table
   */
  class AnalysisEventTypeTable(
      val tag: Tag
  ) extends Table[EventTypeRow](tag, Some(SchemaName), AnalysisEventTypeTableName) {

    val id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    val eventTypeId = column[EventTypeId]("EVENT_TYPE_ID")
    val category = column[Category]("CATEGORY")
    val name = column[String]("NAME")
    val shortName = column[Option[String]]("SHORT_NAME")
    // TODO: Need col to tag if used byg Zoology, Botanics, Archeology or Ethnography?
    val attributes = column[JsValue]("ATTRIBUTES")

    // scalastyle:off method.name
    def * = (
      id.?,
      eventTypeId,
      category,
      name,
      shortName,
      attributes
    ) // scalastyle:on method.name
  }

  // scalastyle:off line.size.limit
  type EventRow = (Option[EventId], EventTypeId, JSqlTimestamp, Option[ActorId], Option[JSqlTimestamp], JsValue)
  // scalastyle:on line.size.limit

  class AnalysisEventTable(
      val tag: Tag
  ) extends Table[EventRow](tag, Some(SchemaName), AnalysisEventTableName) {

    val id = column[EventId]("EVENT_ID", O.PrimaryKey, O.AutoInc)
    val eventTypeId = column[EventTypeId]("EVENT_TYPE_ID")
    val eventDate = column[JSqlTimestamp]("EVENT_DATE")
    val registeredBy = column[Option[ActorId]]("REGISTERED_BY")
    val registeredDate = column[Option[JSqlTimestamp]]("REGISTERED_DATE")
    val eventJson = column[JsValue]("EVENT_JSON")

    // scalastyle:off method.name
    def * = (
      id.?,
      eventTypeId,
      eventDate,
      registeredBy,
      registeredDate,
      eventJson
    ) // scalastyle:on method.name
  }

  def asTuple[A <: AnalysisEvent](event: A): EventRow = {
    (
      None,
      event.eventType.typeId,
      event.eventDate,
      event.registeredBy,
      event.registeredDate,
      Json.toJson[AnalysisEvent](event)
    )
  }

  def asEvent(tuple: EventRow): Option[AnalysisEvent] = {
    Json.fromJson[AnalysisEvent](tuple._6).asOpt
  }

}
