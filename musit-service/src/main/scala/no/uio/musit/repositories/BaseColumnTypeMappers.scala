package no.uio.musit.repositories

import java.sql.{Timestamp => JSqlTimestamp}

import no.uio.musit.models.MuseumCollections.Collection
import no.uio.musit.models.ObjectTypes.ObjectType
import no.uio.musit.models._
import no.uio.musit.time.Implicits.{dateTimeToJTimestamp, jSqlTimestampToDateTime}
import org.joda.time.DateTime
import play.api.db.slick.HasDatabaseConfig
import play.api.libs.json.{JsValue, Json}
import slick.ast._
import slick.jdbc.JdbcProfile
import slick.lifted._
import FunctionSymbolExtensionMethods._
import no.uio.musit.security.Permissions.Permission
import no.uio.musit.security.{BearerToken, ModuleConstraint, SessionUUID}

trait BaseColumnTypeMappers extends ColumnTypesImplicits {
  self: HasDatabaseConfig[JdbcProfile] =>

  import columnTypes._
  import profile.api.{MappedColumnType, BaseColumnType}

  // Implicit that extends Email typed columns with a toLowerCase method
  implicit class EmailColumnExtensionMethods[P1](val c: Rep[Email]) {
    implicit def emailTypedType: TypedType[Email] = implicitly[TypedType[Email]]

    def toLowerCase(implicit tt: TypedType[Email]) =
      Library.LCase.column[Email](c.toNode)
  }

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

  implicit val dbIdMapper: BaseColumnType[DatabaseId] =
    MappedColumnType.base[DatabaseId, Long](
      did => did.underlying,
      longId => DatabaseId(longId)
    )

  implicit val objectIdMapper: BaseColumnType[ObjectId] =
    MappedColumnType.base[ObjectId, Long](
      oid => oid.underlying,
      longId => ObjectId(longId)
    )

  implicit val nodePathMapper: BaseColumnType[NodePath] =
    MappedColumnType.base[NodePath, String](
      nodePath => nodePath.path,
      pathStr => NodePath(pathStr)
    )

  implicit val objTypeMapper: BaseColumnType[ObjectType] =
    MappedColumnType.base[ObjectType, String](
      tpe => tpe.name,
      str => ObjectType.unsafeFromString(str)
    )

  implicit val caseNumberMapper: BaseColumnType[CaseNumbers] =
    MappedColumnType.base[CaseNumbers, String](
      ref => ref.toDbString,
      str => CaseNumbers(str)
    )

  implicit val museumNoMapper: BaseColumnType[MuseumNo] =
    MappedColumnType.base[MuseumNo, String](
      museumNo => museumNo.value,
      noStr => MuseumNo(noStr)
    )

  implicit val subNoMapper: BaseColumnType[SubNo] =
    MappedColumnType.base[SubNo, String](
      subNo => subNo.value,
      noStr => SubNo(noStr)
    )

  implicit val collectionMapper: BaseColumnType[Collection] =
    MappedColumnType.base[Collection, Int](
      mc => mc.id,
      id => Collection.fromInt(id)
    )

  implicit lazy val oldSchemaMapper: BaseColumnType[Seq[Collection]] =
    MappedColumnType.base[Seq[Collection], String](
      seqSchemas => seqSchemas.map(_.id).mkString("[", ",", "]"),
      str => MuseumCollections.fromJsonString(str)
    )

  implicit lazy val collectionIdMapper: BaseColumnType[CollectionUUID] =
    MappedColumnType.base[CollectionUUID, String](
      cid => cid.asString,
      str => CollectionUUID.unsafeFromString(str)
    )

  implicit val orgIdMapper: BaseColumnType[OrgId] =
    MappedColumnType.base[OrgId, Long](
      oid => oid.underlying,
      longId => OrgId(longId)
    )

  implicit val eventIdMapper: BaseColumnType[EventId] =
    MappedColumnType.base[EventId, Long](
      eid => eid.underlying,
      longId => EventId(longId)
    )

  implicit val eventTypeIdMapper: BaseColumnType[EventTypeId] =
    MappedColumnType.base[EventTypeId, Int](
      eventTypeId => eventTypeId.underlying,
      id => EventTypeId(id)
    )

  implicit val museumIdMapper: BaseColumnType[MuseumId] =
    MappedColumnType.base[MuseumId, Int](
      mid => mid.underlying,
      intId => MuseumId.fromInt(intId)
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

  implicit val dateTimeMapper: BaseColumnType[DateTime] =
    MappedColumnType.base[DateTime, JSqlTimestamp](
      dt => dateTimeToJTimestamp(dt),
      jst => jSqlTimestampToDateTime(jst)
    )

  implicit val jsonMapper: BaseColumnType[JsValue] =
    MappedColumnType.base[JsValue, String](
      jsv => Json.prettyPrint(jsv),
      str => Json.parse(str)
    )

  implicit lazy val emailMapper: BaseColumnType[Email] =
    MappedColumnType.base[Email, String](
      email => email.value.toLowerCase,
      str => Email.fromString(str)
    )

  implicit lazy val groupIdMapper: BaseColumnType[GroupId] =
    MappedColumnType.base[GroupId, String](
      gid => gid.asString,
      str => GroupId.unsafeFromString(str)
    )

  implicit lazy val groupModuleMapper: BaseColumnType[ModuleConstraint] =
    MappedColumnType.base[ModuleConstraint, Int](
      m => m.id,
      i => ModuleConstraint.unsafeFromInt(i)
    )

  implicit lazy val permissionMapper: BaseColumnType[Permission] =
    MappedColumnType.base[Permission, Int](
      p => p.priority,
      i => Permission.fromInt(i)
    )

  implicit lazy val sessionIdMapper: BaseColumnType[SessionUUID] =
    MappedColumnType.base[SessionUUID, String](
      sid => sid.asString,
      str => SessionUUID.unsafeFromString(str)
    )

  implicit lazy val bearerTokenMapper: BaseColumnType[BearerToken] =
    MappedColumnType.base[BearerToken, String](
      bt => bt.underlying,
      str => BearerToken(str)
    )
}
