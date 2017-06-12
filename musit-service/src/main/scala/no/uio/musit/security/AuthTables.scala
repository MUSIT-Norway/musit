package no.uio.musit.security

import java.sql.{Timestamp => JSqlTimestamp}

import no.uio.musit.MusitResults.{MusitResult, MusitSuccess}
import no.uio.musit.models.MuseumCollections.Collection
import no.uio.musit.models._
import no.uio.musit.security.Permissions.Permission
import no.uio.musit.time.DateTimeImplicits
import org.joda.time.DateTime
import play.api.Logger
import play.api.db.slick.HasDatabaseConfigProvider
import slick.ast._
import slick.jdbc.JdbcProfile
import slick.lifted._
import FunctionSymbolExtensionMethods._

import scala.concurrent.{ExecutionContext, Future}

trait AuthTables extends HasDatabaseConfigProvider[JdbcProfile] with DateTimeImplicits {

  private val logger = Logger(classOf[AuthTables])

  import profile.api._

  implicit lazy val actorIdMapper: BaseColumnType[ActorId] =
    MappedColumnType.base[ActorId, String](
      gid => gid.asString,
      str => ActorId.unsafeFromString(str)
    )

  // Implicit that extends Email typed columns with a toLowerCase method
  implicit class EmailColumnExtensionMethods[P1](val c: Rep[Email]) {
    implicit def emailTypedType = implicitly[TypedType[Email]]

    def toLowerCase(implicit tt: TypedType[Email]) =
      Library.LCase.column[Email](c.toNode)
  }

  implicit lazy val emailMapper: BaseColumnType[Email] =
    MappedColumnType.base[Email, String](
      email => email.value.toLowerCase,
      str => Email.fromString(str)
    )

  implicit lazy val collectionIdMapper: BaseColumnType[CollectionUUID] =
    MappedColumnType.base[CollectionUUID, String](
      cid => cid.asString,
      str => CollectionUUID.unsafeFromString(str)
    )

  implicit lazy val groupIdMapper: BaseColumnType[GroupId] =
    MappedColumnType.base[GroupId, String](
      gid => gid.asString,
      str => GroupId.unsafeFromString(str)
    )

  implicit lazy val museumIdMapper: BaseColumnType[MuseumId] =
    MappedColumnType.base[MuseumId, Int](
      m => m.underlying,
      i => MuseumId.fromInt(i)
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

  implicit lazy val oldSchemaMapper: BaseColumnType[Seq[Collection]] =
    MappedColumnType.base[Seq[Collection], String](
      seqSchemas => seqSchemas.map(_.id).mkString("[", ",", "]"),
      str => MuseumCollections.fromJsonString(str)
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

  implicit lazy val dateTimeMapper: BaseColumnType[DateTime] =
    MappedColumnType.base[DateTime, JSqlTimestamp](
      dt => dateTimeToJTimestamp(dt),
      jst => jSqlTimestampToDateTime(jst)
    )

  val schema = "MUSARK_AUTH"

  val grpTable        = TableQuery[GroupTable]
  val usrGrpTable     = TableQuery[UserGroupTable]
  val usrInfoTable    = TableQuery[UserInfoTable]
  val musColTable     = TableQuery[MuseumCollectionTable]
  val usrSessionTable = TableQuery[UserSessionTable]

  type GroupDBTuple =
    ((GroupId, String, ModuleConstraint, Permission, MuseumId, Option[String]))

  class GroupTable(
      val tag: Tag
  ) extends Table[GroupDBTuple](tag, Some(schema), "AUTH_GROUP") {

    val id          = column[GroupId]("GROUP_UUID", O.PrimaryKey)
    val name        = column[String]("GROUP_NAME")
    val module      = column[ModuleConstraint]("GROUP_MODULE")
    val permission  = column[Permission]("GROUP_PERMISSION")
    val museumId    = column[MuseumId]("GROUP_MUSEUMID")
    val description = column[Option[String]]("GROUP_DESCRIPTION")

    // scalastyle:off
    override def * = (id, name, module, permission, museumId, description)
    // scalastyle:on

  }

  type CollectionDBTuple = ((CollectionUUID, Option[String], Seq[Collection]))

  class MuseumCollectionTable(
      val tag: Tag
  ) extends Table[CollectionDBTuple](tag, Some(schema), "MUSEUM_COLLECTION") {

    val uuid      = column[CollectionUUID]("COLLECTION_UUID", O.PrimaryKey)
    val name      = column[Option[String]]("COLLECTION_NAME")
    val schemaIds = column[Seq[Collection]]("COLLECTION_SCHEMA_IDENTIFIERS")

    override def * = (uuid, name, schemaIds) // scalastyle:ignore

  }

  type UserInfoDBTuple =
    ((ActorId, Option[Email], Option[String], Option[Email], Option[String]))

  def userInfoFromTuple(tuple: UserInfoDBTuple): UserInfo = {
    UserInfo(
      id = tuple._1,
      secondaryIds = tuple._2.map(sec => Seq(sec.value)),
      name = tuple._3,
      email = tuple._4,
      picture = tuple._5
    )
  }

  class UserInfoTable(
      val tag: Tag
  ) extends Table[UserInfoDBTuple](tag, Some(schema), "USER_INFO") {

    val uuid    = column[ActorId]("USER_UUID", O.PrimaryKey)
    val secId   = column[Option[Email]]("SECONDARY_ID")
    val name    = column[Option[String]]("NAME")
    val email   = column[Option[Email]]("EMAIL")
    val picture = column[Option[String]]("PICTURE")

    override def * = (uuid, secId, name, email, picture) // scalastyle:ignore

  }

  class UserSessionTable(
      val tag: Tag
  ) extends Table[UserSession](tag, Some(schema), "USER_SESSION") {

    val uuid        = column[SessionUUID]("SESSION_UUID", O.PrimaryKey)
    val token       = column[Option[BearerToken]]("TOKEN")
    val userUuid    = column[Option[ActorId]]("USER_UUID")
    val loginTime   = column[Option[DateTime]]("LOGIN_TIME")
    val lastActive  = column[Option[DateTime]]("LAST_ACTIVE")
    val isLoggedIn  = column[Boolean]("IS_LOGGED_IN")
    val tokenExpiry = column[Option[Long]]("TOKEN_EXPIRES_IN")
    val client      = column[Option[String]]("CLIENT")

    val create = (
        uuid: SessionUUID,
        accessToken: Option[BearerToken],
        userId: Option[ActorId],
        loginTimestamp: Option[DateTime],
        lastActiveTimestamp: Option[DateTime],
        loggedIn: Boolean,
        expiration: Option[Long],
        client: Option[String]
    ) =>
      UserSession(
        uuid = uuid,
        oauthToken = accessToken,
        userId = userId,
        loginTime = loginTimestamp,
        lastActive = lastActiveTimestamp,
        isLoggedIn = loggedIn,
        tokenExpiry = expiration,
        client = client
    )

    val destroy = (us: UserSession) =>
      Some(
        (
          us.uuid,
          us.oauthToken,
          us.userId,
          us.loginTime,
          us.lastActive,
          us.isLoggedIn,
          us.tokenExpiry,
          us.client
        )
    )

    // scalastyle:off
    override def * =
      (
        uuid,
        token,
        userUuid,
        loginTime,
        lastActive,
        isLoggedIn,
        tokenExpiry,
        client
      ) <> (create.tupled, destroy)
    // scalastyle:on

  }

  class UserGroupTable(
      val tag: Tag
  ) extends Table[UserGroupMembership](tag, Some(schema), "USER_AUTH_GROUP") {

    val id           = column[Option[Int]]("UAG_ID", O.PrimaryKey, O.AutoInc)
    val feideEmail   = column[Email]("USER_FEIDE_EMAIL")
    val groupId      = column[GroupId]("GROUP_UUID")
    val collectionId = column[Option[CollectionUUID]]("COLLECTION_UUID")

    val create = (
        id: Option[Int],
        feideEmail: Email,
        groupId: GroupId,
        collection: Option[CollectionUUID]
    ) => UserGroupMembership(id, feideEmail, groupId, collection)

    val destroy = (ugm: UserGroupMembership) =>
      Some((ugm.id, ugm.feideEmail, ugm.groupId, ugm.collection))

    // scalastyle:off
    override def * =
      (
        id,
        feideEmail,
        groupId,
        collectionId
      ) <> (create.tupled, destroy)
    // scalastyle:on
  }

  type GrpColTuples = Seq[(GroupDBTuple, CollectionDBTuple)]

  /**
   * Helper function to locate GroupInfo entries for a query based on the
   * UserGroupTable. The query will be joined with the GroupTable and the
   * MuseumCollectionTable to calculate valid GroupInfo instances.
   *
   * @param q
   * @param ec
   * @return A Future MusitResult containing collection of GroupInfos
   */
  def findGroupInfoBy(
      q: Query[UserGroupTable, UserGroupMembership, Seq]
  )(implicit ec: ExecutionContext): Future[MusitResult[Seq[GroupInfo]]] = {
    val q1 = q join grpTable on (_.groupId === _.id)
    val q2 = q1 join musColTable on { (ugg, col) =>
      ugg._1.collectionId === col.uuid || ugg._1.collectionId.isEmpty
    }
    val query = for (((_, grp), col) <- q2) yield (grp, col)

    db.run(query.distinct.result).map(gc => MusitSuccess(foldGroupCol(gc)))
  }

  def convertColGrpTuples(tuples: GrpColTuples): Seq[(GroupInfo, MuseumCollection)] = {
    tuples.map(convertColGrpTuple)
  }

  def convertColGrpTuple(
      tuple: (GroupDBTuple, CollectionDBTuple)
  ): (GroupInfo, MuseumCollection) = {
    (GroupInfo.fromTuple(tuple._1), MuseumCollection.fromTuple(tuple._2))
  }

  /**
   * Function that helps to reduce rows of GroupInfo and MuseumCollections into
   * one GroupInfo (per unique group) containing all its MuseumCollection.
   *
   * @param tuples A Sequence of tuples of GroupTableType and CollectionTableType
   * @return A List of GroupInfo data
   */
  def foldGroupCol(tuples: GrpColTuples): List[GroupInfo] = {
    convertColGrpTuples(tuples).foldLeft(List.empty[GroupInfo]) { (grpInfos, tuple) =>
      val currGrp = tuple._1
      val currCol = tuple._2

      if (grpInfos.exists(_.id == currGrp.id)) {
        grpInfos.map { gi =>
          if (gi.id == currGrp.id) gi.copy(collections = gi.collections :+ currCol)
          else gi
        }
      } else {
        grpInfos :+ currGrp.copy(collections = currGrp.collections :+ currCol)
      }
    }
  }

}
