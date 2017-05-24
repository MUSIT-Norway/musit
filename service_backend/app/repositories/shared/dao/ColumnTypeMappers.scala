package repositories.shared.dao

import java.sql.{Timestamp => JSqlTimestamp}

import models.analysis.ActorByIdOrName
import models.analysis.AnalysisStatuses.AnalysisStatus
import models.analysis.LeftoverSamples.LeftoverSample
import models.analysis.SampleStatuses.SampleStatus
import models.analysis.SampleTypeId
import models.analysis.events.{AnalysisTypeId, Category, EventCategories}
import models.loan.{LoanEventTypes, LoanType}
import models.storage.event.EventTypeId
import models.storage.nodes.StorageType
import no.uio.musit.models.ObjectTypes.ObjectType
import no.uio.musit.models.{ActorId, EventId, MuseumId, ObjectUUID, _}
import no.uio.musit.time.Implicits.{dateTimeToJTimestamp, jSqlTimestampToDateTime}
import org.joda.time.DateTime
import play.api.db.slick.HasDatabaseConfig
import play.api.libs.json.{JsValue, Json}
import slick.jdbc.JdbcProfile

trait ColumnTypeMappers { self: HasDatabaseConfig[JdbcProfile] =>

  import profile.api._

  implicit val storageNodeDbIdMapper: BaseColumnType[StorageNodeDatabaseId] =
    MappedColumnType.base[StorageNodeDatabaseId, Long](
      snid => snid.underlying,
      longId => StorageNodeDatabaseId(longId)
    )

  implicit val storageNodeIdMapper: BaseColumnType[StorageNodeId] =
    MappedColumnType.base[StorageNodeId, String](
      sid => sid.asString,
      strId => StorageNodeId.unsafeFromString(strId)
    )

  implicit val objectIdMapper: BaseColumnType[ObjectId] =
    MappedColumnType.base[ObjectId, Long](
      oid => oid.underlying,
      longId => ObjectId(longId)
    )

  implicit val storageTypeMapper =
    MappedColumnType.base[StorageType, String](
      storageType => storageType.entryName,
      string => StorageType.withName(string)
    )

  implicit val eventTypeIdMapper: BaseColumnType[EventTypeId] =
    MappedColumnType.base[EventTypeId, Int](
      eventTypeId => eventTypeId.underlying,
      id => EventTypeId(id)
    )

  implicit val nodePathMapper: BaseColumnType[NodePath] =
    MappedColumnType.base[NodePath, String](
      nodePath => nodePath.path,
      pathStr => NodePath(pathStr)
    )

  implicit val eventIdMapper: BaseColumnType[EventId] =
    MappedColumnType.base[EventId, Long](
      eid => eid.underlying,
      longId => EventId(longId)
    )

  implicit val analysisTypeIdMapper: BaseColumnType[AnalysisTypeId] =
    MappedColumnType.base[AnalysisTypeId, Long](
      etid => etid.underlying,
      lid => AnalysisTypeId(lid)
    )

  implicit val sampleTypeIdMapper: BaseColumnType[SampleTypeId] =
    MappedColumnType.base[SampleTypeId, Long](
      etid => etid.underlying,
      lid => SampleTypeId.fromLong(lid)
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

  implicit val actorNameMapper: BaseColumnType[ActorByIdOrName] =
    MappedColumnType.base[ActorByIdOrName, String](
      an => an.name,
      strVal => ActorByIdOrName.apply(strVal)
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
