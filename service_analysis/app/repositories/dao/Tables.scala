package repositories.dao

import java.sql.{Timestamp => JSqlTimestamp}

import models.events.AnalysisResults.AnalysisResult
import models.events.{AnalysisEvent, AnalysisType, AnalysisTypeId, Category}
import no.uio.musit.models._
import no.uio.musit.time.Implicits._
import play.api.db.slick.HasDatabaseConfigProvider
import play.api.libs.json.{JsValue, Json}
import slick.driver.JdbcProfile

/**
 * Defines the table definitions needed to work with analysis data.
 * Also includes a few helpful functions to convert between rows and types.
 */
trait Tables extends HasDatabaseConfigProvider[JdbcProfile] with ColumnTypeMappers {

  import driver.api._

  val analysisTypeTable = TableQuery[AnalysisTypeTable]
  val analysisTable = TableQuery[AnalysisTable]
  val resultTable = TableQuery[AnalysisResultTable]

  // scalastyle:off line.size.limit
  type EventTypeRow = (AnalysisTypeId, Category, String, Option[String], Option[String], Option[JsValue])
  type EventRow = (Option[EventId], AnalysisTypeId, Option[JSqlTimestamp], Option[ActorId], Option[JSqlTimestamp], Option[EventId], Option[ObjectUUID], Option[String], JsValue)
  type ResultRow = (Option[Long], EventId, Option[ActorId], Option[JSqlTimestamp], JsValue)
  // scalastyle:on line.size.limit

  /**
   * Representation of the MUSARK_ANALYSIS.EVENT_TYPE table
   */
  class AnalysisTypeTable(
      val tag: Tag
  ) extends Table[EventTypeRow](tag, Some(SchemaName), AnalysisEventTypeTableName) {

    val typeId = column[AnalysisTypeId]("TYPE_ID", O.PrimaryKey)
    val category = column[Category]("CATEGORY")
    val name = column[String]("NAME")
    val shortName = column[Option[String]]("SHORT_NAME")
    val collections = column[Option[String]]("COLLECTIONS")
    // TODO: Need col to tag if used by Zoology, Botanics, Archeology or Ethnography?
    val attributes = column[Option[JsValue]]("ATTRIBUTES")

    // scalastyle:off method.name
    def * = (typeId, category, name, shortName, collections, attributes)
    // scalastyle:on method.name
  }

  /**
   * Representation of the MUSARK_ANALYSIS.EVENT table
   */
  class AnalysisTable(
      val tag: Tag
  ) extends Table[EventRow](tag, Some(SchemaName), AnalysisEventTableName) {

    val id = column[EventId]("EVENT_ID", O.PrimaryKey, O.AutoInc)
    val typeId = column[AnalysisTypeId]("TYPE_ID")
    val eventDate = column[Option[JSqlTimestamp]]("EVENT_DATE")
    val registeredBy = column[Option[ActorId]]("REGISTERED_BY")
    val registeredDate = column[Option[JSqlTimestamp]]("REGISTERED_DATE")
    val partOf = column[Option[EventId]]("PART_OF")
    val objectUuid = column[Option[ObjectUUID]]("OBJECT_UUID")
    val note = column[Option[String]]("NOTE")
    val eventJson = column[JsValue]("EVENT_JSON")

    // scalastyle:off method.name line.size.limit
    def * = (id.?, typeId, eventDate, registeredBy, registeredDate, partOf, objectUuid, note, eventJson)
    // scalastyle:on method.name line.size.limit
  }

  /**
   * Representation of the MUSARK_ANALUSIS.RESULT table
   */
  class AnalysisResultTable(
      val tag: Tag
  ) extends Table[ResultRow](tag, Some(SchemaName), AnalysisResultTableName) {

    val id = column[Long]("RESULT_ID", O.PrimaryKey, O.AutoInc)
    val eventId = column[EventId]("EVENT_ID")
    val registeredBy = column[Option[ActorId]]("REGISTERED_BY")
    val registeredDate = column[Option[JSqlTimestamp]]("REGISTERED_DATE")
    val resultJson = column[JsValue]("RESULT_JSON")

    // scalastyle:off method.name
    def * = (id.?, eventId, registeredBy, registeredDate, resultJson)
    // scalastyle:on method.name
  }

  private def parseCollectionUUIDCol(colStr: Option[String]): Seq[CollectionUUID] = {
    colStr.map { str =>
      str.stripPrefix(",").stripSuffix(",").split(",").toSeq.map { uuidStr =>
        CollectionUUID.unsafeFromString(uuidStr)
      }
    }.getOrElse(Seq.empty[CollectionUUID])
  }

  /**
   * Converts an EventTypeRow tuple to an instance of AnalysisType.
   *
   * @param t the EventTypeRow tuple to convert
   * @return the corresponding AnalysisType
   */
  protected[dao] def fromAnalysisTypeRow(t: EventTypeRow): AnalysisType = {
    AnalysisType(
      id = t._1,
      category = t._2,
      name = t._3,
      shortName = t._4,
      collections = parseCollectionUUIDCol(t._5),
      extraAttributes = t._6.flatMap(a => Json.fromJson[Map[String, String]](a).asOpt)
    )
  }

  /**
   * Converts an AnalysisEvent into an EventRow tuple that can be inserted into
   * the database table.
   *
   * @param event the event to convert to a tuple
   * @return an EventRow tuple
   */
  protected[dao] def asAnalysisTuple(event: AnalysisEvent): EventRow = {
    (
      None,
      event.analysisTypeId,
      event.eventDate,
      event.registeredBy,
      event.registeredDate,
      event.partOf,
      event.objectId,
      event.note,
      Json.toJson[AnalysisEvent](event)
    )
  }

  /**
   * Converts an EventRow tuple into an instance of AnalysisEvent.
   *
   * @param tuple EventRow
   * @return an Option of AnalysisEvent.
   */
  protected[dao] def fromAnalysisRow(tuple: EventRow): Option[AnalysisEvent] =
    Json.fromJson[AnalysisEvent](tuple._9).asOpt

  /**
   * Converts an AnalysisResult into a ResultRow tuple.
   *
   * @param eid the EventId the AnalysisResult belongs to
   * @param res the AnalysisResult to convert to a tuple
   * @return the corresponding ResultRow tuple
   */
  protected[dao] def asResultTuple(eid: EventId, res: AnalysisResult): ResultRow = {
    (
      None,
      eid,
      res.registeredBy,
      res.registeredDate,
      Json.toJson[AnalysisResult](res)
    )
  }

  /**
   * Converts a ResultRow tuple into an instance of an AnalysisResult
   *
   * @param tuple the ResultRow tuple to convert
   * @return an Option of the corresponding AnalysisResult
   */
  protected[dao] def fromResultRow(tuple: ResultRow): Option[AnalysisResult] =
    Json.fromJson[AnalysisResult](tuple._5).asOpt

}
