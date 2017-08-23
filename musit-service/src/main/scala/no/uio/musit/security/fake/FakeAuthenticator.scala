package no.uio.musit.security.fake

import com.google.inject.Inject
import no.uio.musit.MusitResults._
import no.uio.musit.models._
import no.uio.musit.security.Permissions.Permission
import no.uio.musit.security._
import no.uio.musit.security.fake.FakeAuthenticator.FakeUserDetails
import no.uio.musit.time.dateTimeNow
import play.api.libs.json.{JsArray, Json}
import play.api.mvc.{Request, Result, Results}

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

// TODO: Refactor the entire Fake implementation to be usable in _tests only_.
class FakeAuthenticator @Inject()(implicit val ec: ExecutionContext)
    extends Authenticator {

  private val fakeFile = "/fake_security.json"

  val config = Json.parse(
    Source.fromInputStream(getClass.getResourceAsStream(fakeFile)).getLines().mkString
  )

  private case class FakeGroup(
      id: GroupId,
      name: String,
      module: ModuleConstraint,
      permission: Permission,
      museumId: MuseumId,
      description: Option[String],
      collections: Seq[CollectionUUID]
  )

  private implicit val formatFakeGroup = Json.format[FakeGroup]

  private lazy val allCols = (config \ "museumCollections").as[Seq[MuseumCollection]]

  private lazy val collectionsMap = allCols.map(c => (c.uuid, c)).toMap

  private lazy val allGroups = (config \ "groups").as[Seq[FakeGroup]].map { fg =>
    GroupInfo(
      id = fg.id,
      name = fg.name,
      module = fg.module,
      permission = fg.permission,
      museumId = fg.museumId,
      description = fg.description,
      collections = fg.collections.map(cid => collectionsMap(cid))
    )
  }

  private lazy val fakeUsers: Map[BearerToken, FakeUserDetails] = {
    (config \ "users")
      .as[JsArray]
      .value
      .map { usrJs =>
        val token      = BearerToken((usrJs \ "accessToken").as[String])
        val usrGrps    = (usrJs \ "groups").as[Seq[GroupId]]
        val usrInfo    = usrJs.as[UserInfo]
        val userGroups = allGroups.filter(g => usrGrps.contains(g.id))

        (token, FakeUserDetails(usrInfo, userGroups))
      }
      .toMap
  }

  /**
   * Method for retrieving the UserInfo from the FakeAuthService.
   *
   * @param token the BearerToken to use when performing the request
   * @return Will eventually return the UserInfo wrapped in a MusitResult
   */
  override def userInfo(token: BearerToken): Future[MusitResult[UserInfo]] = {
    Future.successful {
      fakeUsers
        .get(token)
        .map { fud =>
          MusitSuccess(fud.info)
        }
        .getOrElse {
          MusitNotAuthenticated()
        }
    }
  }

  /**
   * Method for retrieving the users GroupInfo from the AuthService based
   * on the UserInfo found.
   *
   * @param userInfo the UserInfo found by calling the userInfo method above.
   * @return Will eventually return a Seq of GroupInfo
   */
  override def groups(userInfo: UserInfo): Future[MusitResult[Seq[GroupInfo]]] =
    Future.successful {
      MusitSuccess(
        fakeUsers.find(_._2.info.id == userInfo.id).map(_._2.groups).getOrElse(Seq.empty)
      )
    }

  /**
   * Fake authenticate implementation that "logs" in a user based on the data
   * found in the fake_security.json
   *
   * @param client the client making the auth request
   * @param req The current request.
   * @tparam A The type of the request body.
   * @return Either a Result or the active UserSession
   */
  override def authenticate[A](client: Option[String])(
      implicit req: Request[A]
  ): Future[Either[Result, UserSession]] = Future.successful {
    Left(
      Results.NotImplemented(
        Json.obj("message" -> "authenticate is not implemented for fake security")
      )
    )
  }

  /**
   * Method to "touch" the UserSession whenever a User interacts with a service.
   * This implementation is completely faked, and should only be used for
   * testing purposes.
   *
   * @param token BearerToken
   * @return eventually it returns the updated MusitResult[UserSession]
   */
  def touch(token: BearerToken): Future[MusitResult[UserSession]] = {
    userInfo(token).map(_.map { ui =>
      UserSession(
        uuid = SessionUUID.fromBearerToken(token),
        oauthToken = Option(token),
        userId = Option(ui.id),
        loginTime = None,
        lastActive = Option(dateTimeNow),
        isLoggedIn = true,
        tokenExpiry = None
      )
    })
  }

  /**
   * Just a dummy implementation that does nothing other than returning a
   * successful future with a MusitSuccess.
   *
   * @param token BearerToken
   * @return a MusitResult[Unit] wrapped in a Future.
   */
  override def invalidate(token: BearerToken): Future[MusitResult[Unit]] = {
    Future.successful(MusitSuccess(()))
  }

}

object FakeAuthenticator {

  // Must match the fake_security.json file!
  val fakeAccessTokenPrefix = "fake-token-zab-xy-"

  case class FakeUserDetails(info: UserInfo, groups: Seq[GroupInfo])

}
