package repositories.shared.dao

import java.sql.{Timestamp => JSqlTimestamp}

import models.analysis.AnalysisStatuses.AnalysisStatus
import models.analysis.LeftoverSamples.LeftoverSample
import models.analysis.SampleStatuses.SampleStatus
import models.analysis.events.{AnalysisTypeId, Category, EventCategories}
import models.loan.{LoanEventTypes, LoanType}
import no.uio.musit.models.ObjectTypes.ObjectType
import no.uio.musit.models.{ActorId, EventId, MuseumId, ObjectUUID, _}
import no.uio.musit.time.Implicits.{dateTimeToJTimestamp, jSqlTimestampToDateTime}
import org.joda.time.DateTime
import play.api.db.slick.HasDatabaseConfig
import play.api.libs.json.{JsValue, Json}
import slick.jdbc.JdbcProfile

trait ColumnTypeMappers { self: HasDatabaseConfig[JdbcProfile] =>

  import profile.api._

  implicit val eventIdMapper: BaseColumnType[EventId] =
    MappedColumnType.base[EventId, Long](
      eid => eid.underlying,
      longId => EventId(longId)
    )

  implicit val eventTypeIdMapper: BaseColumnType[AnalysisTypeId] =
    MappedColumnType.base[AnalysisTypeId, String](
      etid => etid.asString,
      strId => AnalysisTypeId.unsafeFromString(strId)
    )

  implicit val museumIdMapper: BaseColumnType[MuseumId] =
    MappedColumnType.base[MuseumId, Int](
      mid => mid.underlying,
      intId => MuseumId.fromInt(intId)
    )

  implicit val objTypeMapper: BaseColumnType[ObjectType] =
    MappedColumnType.base[ObjectType, String](
      tpe => tpe.name,
      str => ObjectType.unsafeFromString(str)
    )

  implicit val actorIdMapper: BaseColumnType[ActorId] =
    MappedColumnType.base[ActorId, String](
      aid => aid.asString,
      strId => ActorId.unsafeFromString(strId)
    )

  implicit val objectUuidMapper: BaseColumnType[ObjectUUID] =
    MappedColumnType.base[ObjectUUID, String](
      oid => oid.asString,
      strId => ObjectUUID.unsafeFromString(strId)
    )

  implicit val categoryMapper: BaseColumnType[Category] =
    MappedColumnType.base[Category, Int](
      cat => cat.id,
      catId => EventCategories.unsafeFromId(catId)
    )

  implicit val sampleStatusMapper: BaseColumnType[SampleStatus] =
    MappedColumnType.base[SampleStatus, Int](
      ssid => ssid.key,
      intId => SampleStatus.unsafeFromInt(intId)
    )

  implicit val loanTypeMapper: BaseColumnType[LoanType] =
    MappedColumnType.base[LoanType, Long](
      loanType => loanType.id,
      longId => LoanEventTypes.unsafeFromId(longId)
    )

  implicit val dateTimeMapper: BaseColumnType[DateTime] =
    MappedColumnType.base[DateTime, JSqlTimestamp](
      dt => dateTimeToJTimestamp(dt),
      jst => jSqlTimestampToDateTime(jst)
    )

  implicit val leftoverSampleMapper: BaseColumnType[LeftoverSample] =
    MappedColumnType.base[LeftoverSample, Int](
      rm => rm.key,
      intId => LeftoverSample.unsafeFromInt(intId)
    )

  implicit val jsonMapper: BaseColumnType[JsValue] =
    MappedColumnType.base[JsValue, String](
      jsv => Json.prettyPrint(jsv),
      str => Json.parse(str)
    )

  implicit val caseNumberMapper: BaseColumnType[CaseNumbers] =
    MappedColumnType.base[CaseNumbers, String](
      ref => ref.toDbString,
      str => CaseNumbers(str)
    )

  implicit val analysisStatusMapper: BaseColumnType[AnalysisStatus] =
    MappedColumnType.base[AnalysisStatus, Int](
      st => st.key,
      key => AnalysisStatus.unsafeFromInt(key)
    )

}
