package repositories.elasticsearch.dao

import java.sql.{Timestamp => JsSqlTimestamp}

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.google.inject.{Inject, Singleton}
import models.analysis.AnalysisStatuses.AnalysisStatus
import models.analysis.LeftoverSamples.LeftoverSample
import models.analysis.SampleStatuses.SampleStatus
import models.analysis.SampleTypeId
import models.analysis.events.AnalysisResults.AnalysisResult
import models.analysis.events._
import models.elasticsearch.AnalysisModuleEventSearch
import no.uio.musit.models.ObjectTypes.ObjectType
import no.uio.musit.models._
import no.uio.musit.time.Implicits._
import org.joda.time.DateTime
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.Json
import repositories.analysis.dao.{AnalysisEventTableProvider, AnalysisTables}
import slick.jdbc.GetResult

@Singleton
class ElasticsearchEventDao @Inject()(val dbConfigProvider: DatabaseConfigProvider)
    extends AnalysisEventTableProvider
    with AnalysisTables {

  import profile.api._

  type AnalysisEventStreamRow = (EventRow, Option[ResultRow], Option[SampleObjectRow])

  implicit val s: GetResult[AnalysisEventStreamRow] = GetResult { p =>
    val eventRow = (
      EventId.fromOptLong(p.nextLongOption()),
      AnalysisTypeId(p.nextInt()),
      MuseumId(p.nextInt()),
      // registered by / date
      ActorId.unsafeFromString(p.nextString()),
      jSqlTimestampToDateTime(p.nextTimestamp()),
      // updated by / date
      p.nextStringOption().map(ActorId.unsafeFromString),
      optJSqlTimestampToDateTime(p.nextTimestampOption()),
      // updated date
      optJSqlTimestampToDateTime(p.nextTimestampOption()),
      // event id
      EventId.fromOptLong(p.nextLongOption()),
      // MusitUUID
      p.nextStringOption(),
      // note
      p.nextStringOption(),
      p.nextIntOption().map(AnalysisStatus.unsafeFromInt),
      p.nextStringOption().map(CaseNumbers.apply),
      Json.parse(p.nextString())
    )
    val resultRowId = p.nextLongOption()
    val resultRow = resultRowId.map { rowId =>
      (
        EventId.fromLong(rowId),
        MuseumId(p.nextInt()),
        p.nextStringOption().map(ActorId.unsafeFromString),
        optJSqlTimestampToDateTime(p.nextTimestampOption()),
        Json.parse(p.nextString())
      )
    }
    val sampleObjectRowId = p.nextStringOption()
    val sampleObjectRow = sampleObjectRowId.map { rowId =>
      (
        ObjectUUID.unsafeFromString(rowId),
        (
          p.nextStringOption().map(ObjectUUID.unsafeFromString),
          ObjectType.fromOptString(p.nextStringOption())
        ),
        p.nextBoolean(),
        MuseumId(p.nextInt()),
        SampleStatus.unsafeFromInt(p.nextInt()),
        p.nextStringOption().map(ActorId.unsafeFromString),
        p.nextStringOption().map(ActorId.unsafeFromString),
        optJSqlTimestampToDateTime(p.nextTimestampOption()),
        p.nextStringOption(),
        p.nextIntOption(),
        (p.nextStringOption(), p.nextStringOption()),
        SampleTypeId.fromLong(p.nextLong()),
        (p.nextDoubleOption(), p.nextStringOption()),
        p.nextStringOption(),
        p.nextStringOption(),
        p.nextStringOption(),
        p.nextStringOption(),
        LeftoverSample.unsafeFromInt(p.nextInt()),
        p.nextStringOption(),
        ObjectUUID.unsafeFromString(p.nextString()),
        (
          p.nextStringOption().map(ActorId.unsafeFromString),
          optJSqlTimestampToDateTime(p.nextTimestampOption()),
          p.nextStringOption().map(ActorId.unsafeFromString),
          optJSqlTimestampToDateTime(p.nextTimestampOption())
        ),
        p.nextBoolean()
      )
    }
    (eventRow, resultRow, sampleObjectRow)
  }

  def analysisEventsStream[E >: AnalysisModuleEventSearch](
      eventsAfter: Option[DateTime] = None
  ): Source[E, NotUsed] = {
    val dateClause = eventsAfter.map { date =>
      val ts = new JsSqlTimestamp(date.getMillis).toString
      s"""WHERE e.UPDATED_DATE > {ts '$ts'}
         |OR e.REGISTERED_DATE > {ts '$ts'}
       """.stripMargin
    }.getOrElse("")

    val query =
      sql"""
           |SELECT  e.EVENT_ID,
           |        e.TYPE_ID,
           |        e.MUSEUM_ID,
           |        e.REGISTERED_BY,
           |        e.REGISTERED_DATE,
           |        e.DONE_BY,
           |        e.DONE_DATE,
           |        e.UPDATED_DATE,
           |        e.PART_OF,
           |        e.AFFECTED_UUID,
           |        e.NOTE,
           |        e.STATUS,
           |        e.CASE_NUMBERS,
           |        e.EVENT_JSON,
           |        r.EVENT_ID,
           |        r.MUSEUM_ID,
           |        r.REGISTERED_BY,
           |        r.REGISTERED_DATE,
           |        r.RESULT_JSON,
           |        so.SAMPLE_UUID,
           |        so.PARENT_OBJECT_UUID,
           |        so.PARENT_OBJECT_TYPE,
           |        so.IS_EXTRACTED,
           |        so.MUSEUM_ID,
           |        so.STATUS,
           |        so.RESPONSIBLE_ACTOR,
           |        so.DONE_BY,
           |        so.DONE_DATE,
           |        so.SAMPLE_ID,
           |        so.SAMPLE_NUM,
           |        so.EXTERNAL_ID,
           |        so.EXTERNAL_ID_SOURCE,
           |        so.SAMPLE_TYPE_ID,
           |        so.SAMPLE_SIZE,
           |        so.SAMPLE_SIZE_UNIT,
           |        so.SAMPLE_CONTAINER,
           |        so.STORAGE_MEDIUM,
           |        so.NOTE,
           |        so.TREATMENT,
           |        so.LEFTOVER_SAMPLE,
           |        so.DESCRIPTION,
           |        so.ORIGINATED_OBJECT_UUID,
           |        so.REGISTERED_BY,
           |        so.REGISTERED_DATE,
           |        so.UPDATED_BY,
           |        so.UPDATED_DATE,
           |        so.IS_DELETED
           |  FROM MUSARK_ANALYSIS.EVENT e
           |  LEFT OUTER JOIN MUSARK_ANALYSIS.RESULT r ON e.EVENT_ID = r.EVENT_ID
           |  LEFT OUTER JOIN MUSARK_ANALYSIS.SAMPLE_OBJECT so ON e.AFFECTED_UUID = so.SAMPLE_UUID
           |#${dateClause}
       """.stripMargin.as[AnalysisEventStreamRow]

    Source.fromPublisher(db.stream(query).mapResult(toAnalysisEvent))
  }

  private def toAnalysisEvent(
      res: (EventRow, Option[ResultRow], Option[SampleObjectRow])
  ) = {
    val event = Json
      .fromJson[AnalysisModuleEvent](res._1._14)
      .map {
        case a: Analysis =>
          a.copy(id = res._1._1)
            .withResult(res._2.map(r => Json.fromJson[AnalysisResult](r._5).get))
        case ac: AnalysisCollection =>
          ac.copy(id = res._1._1)
            .withResult(res._2.map(r => Json.fromJson[AnalysisResult](r._5).get))
        case sc: SampleCreated => sc.copy(id = res._1._1)
      }
      .get

    val objUuid = res._3.map(fromSampleObjectRow).map(_.originatedObjectUuid)
    AnalysisModuleEventSearch(res._1._3, objUuid, event)
  }

}
