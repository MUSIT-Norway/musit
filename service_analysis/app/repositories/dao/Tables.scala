package repositories.dao

import java.sql.{Timestamp => JSqlTimestamp}

import models.events.EventTypes.EventTypeId
import models.events.{AnalysisEvent, Category}
import no.uio.musit.models.{ActorId, EventId}
import no.uio.musit.time.Implicits._
import play.api.db.slick.HasDatabaseConfigProvider
import play.api.libs.json.{JsValue, Json}
import slick.driver.JdbcProfile

trait Tables extends HasDatabaseConfigProvider[JdbcProfile] with ColumnTypeMappers {

  import driver.api._

  val eventTypeTable = TableQuery[AnalysisEventTypeTable]
  val eventTable = TableQuery[AnalysisEventTable]

  // scalastyle:off line.size.limit
  type EventTypeRow = (Option[Int], EventTypeId, Category, String, Option[String], JsValue)
  type EventRow = (Option[EventId], EventTypeId, Option[JSqlTimestamp], Option[ActorId], Option[JSqlTimestamp], Option[String], JsValue)
  type ResultRow = (Option[Long], EventId, JsValue)
  // scalastyle:on line.size.limit

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
    def * = (id.?, eventTypeId, category, name, shortName, attributes)
    // scalastyle:on method.name
  }

  /**
   * Representation of the MUSARK_ANALYSIS.EVENT table
   */
  class AnalysisEventTable(
      val tag: Tag
  ) extends Table[EventRow](tag, Some(SchemaName), AnalysisEventTableName) {

    val id = column[EventId]("EVENT_ID", O.PrimaryKey, O.AutoInc)
    val eventTypeId = column[EventTypeId]("EVENT_TYPE_ID")
    val eventDate = column[Option[JSqlTimestamp]]("EVENT_DATE")
    val registeredBy = column[Option[ActorId]]("REGISTERED_BY")
    val registeredDate = column[Option[JSqlTimestamp]]("REGISTERED_DATE")
    val note = column[Option[String]]("NOTE")
    val eventJson = column[JsValue]("EVENT_JSON")

    // scalastyle:off method.name
    def * = (id.?, eventTypeId, eventDate, registeredBy, registeredDate, note, eventJson)
    // scalastyle:on method.name
  }

  /**
   * Representation of the MUSARK_ANALUSIS.RESULT table
   */
  class AnalysisResultTable(
      val tag: Tag
  ) extends Table[ResultRow](tag, Some(SchemaName), AnalysisResultTableName) {

    val id = column[Long]("RESULT_ID", O.PrimaryKey, O.AutoInc)
    val eventId = column[EventId]("EVENT_ID")
    val resultJson = column[JsValue]("RESULT_JSON")

    // scalastyle:off method.name
    def * = (id.?, eventId, resultJson)
    // scalastyle:on method.name
  }

  def asTuple(event: AnalysisEvent): EventRow = {
    (
      None,
      event.eventTypeId,
      event.eventDate,
      event.registeredBy,
      event.registeredDate,
      event.note,
      Json.toJson(event)
    )
  }

  def asEvent(tuple: EventRow): Option[AnalysisEvent] = {
    Json.fromJson[AnalysisEvent](tuple._7).asOpt
  }

}
