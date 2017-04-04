package repositories.shared.dao

import java.sql.{Timestamp => JSqlTimestamp}

import models.analysis.SampleStatuses.SampleStatus
import models.analysis.events.{AnalysisTypeId, Category, EventCategories}
import no.uio.musit.models.ObjectTypes.ObjectType
import no.uio.musit.models.{ActorId, EventId, MuseumId, ObjectUUID}
import no.uio.musit.time.DefaultTimezone
import org.joda.time.DateTime
import play.api.db.slick.HasDatabaseConfig
import play.api.libs.json.{JsValue, Json}
import slick.jdbc.JdbcProfile

trait ColumnTypeMappers { self: HasDatabaseConfig[JdbcProfile] =>

  import profile.api._

  implicit lazy val eventIdMapper: BaseColumnType[EventId] =
    MappedColumnType.base[EventId, Long](
      eid => eid.underlying,
      longId => EventId(longId)
    )

  implicit lazy val eventTypeIdMapper: BaseColumnType[AnalysisTypeId] =
    MappedColumnType.base[AnalysisTypeId, String](
      etid => etid.asString,
      strId => AnalysisTypeId.unsafeFromString(strId)
    )

  implicit lazy val museumIdMapper: BaseColumnType[MuseumId] =
    MappedColumnType.base[MuseumId, Int](
      mid => mid.underlying,
      intId => MuseumId.fromInt(intId)
    )

  implicit val objTypeMapper: BaseColumnType[ObjectType] =
    MappedColumnType.base[ObjectType, String](
      tpe => tpe.name,
      str => ObjectType.unsafeFromString(str)
    )

  implicit lazy val actorIdMapper: BaseColumnType[ActorId] =
    MappedColumnType.base[ActorId, String](
      aid => aid.asString,
      strId => ActorId.unsafeFromString(strId)
    )

  implicit lazy val objectUuidMapper: BaseColumnType[ObjectUUID] =
    MappedColumnType.base[ObjectUUID, String](
      oid => oid.asString,
      strId => ObjectUUID.unsafeFromString(strId)
    )

  implicit lazy val categoryMapper: BaseColumnType[Category] =
    MappedColumnType.base[Category, Int](
      cat => cat.id,
      catId => EventCategories.unsafeFromId(catId)
    )

  implicit lazy val sampleStatusMapper: BaseColumnType[SampleStatus] =
    MappedColumnType.base[SampleStatus, Int](
      ssid => ssid.identity,
      intId => SampleStatus.unsafeFromInt(intId)
    )

  implicit lazy val jsonMapper: BaseColumnType[JsValue] =
    MappedColumnType.base[JsValue, String](
      jsv => Json.prettyPrint(jsv),
      str => Json.parse(str)
    )

  implicit val dateTimeMapper: BaseColumnType[DateTime] =
    MappedColumnType.base[DateTime, JSqlTimestamp](
      dt => new JSqlTimestamp(dt.getMillis),
      jt => new DateTime(jt, DefaultTimezone)
    )
}
