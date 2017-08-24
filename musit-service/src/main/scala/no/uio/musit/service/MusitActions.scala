package no.uio.musit.service

import no.uio.musit.MusitResults.{MusitNotAuthenticated, MusitNotAuthorized, MusitSuccess}
import no.uio.musit.functional.Implicits.futureMonad
import no.uio.musit.functional.MonadTransformers.MusitResultT
import no.uio.musit.models.MuseumId
import no.uio.musit.models.Museums._
import no.uio.musit.security.Permissions.{ElevatedPermission, MusitAdmin, Permission}
import no.uio.musit.security._
import no.uio.musit.security.crypto.MusitCrypto
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

/**
 * Every request that is successfully authenticated against dataporten, will
 * be transformed into a MusitRequest. It contains information necessary for
 * calculating authorisation and filtering of data.
 *
 * @param user    The authenticated user.
 * @param token   A valid BearerToken
 * @param museum  An optional Museum derived from an incoming MuseumId
 * @param request The incoming request
 * @tparam A Body content type of the incoming request
 */
case class MusitRequest[A](
    user: AuthenticatedUser,
    token: BearerToken,
    museum: Option[Museum],
    request: Request[A]
) extends WrappedRequest[A](request)

trait MusitActions {
  self: BaseController =>

  private val logger = Logger(classOf[MusitActions])

  def authService: Authenticator

  type MusitActionResult[T]  = Either[Result, MusitRequest[T]]
  type MusitActionResultF[T] = Future[MusitActionResult[T]]

  type AuthFunc[T] = (
      BearerToken,
      UserInfo,
      AuthenticatedUser,
      Option[Museum]
  ) => MusitActionResult[T]

  /**
   * Maps an incoming request to a MusitRequest
   */
  trait BaseMusitActionRefiner extends ActionRefiner[Request, MusitRequest] {

    override def refine[T](request: Request[T]): MusitActionResultF[T]

  }

  trait BaseSecureAction
      extends BaseMusitActionRefiner
      with ActionBuilder[MusitRequest, AnyContent] {

    override def parser: BodyParser[AnyContent] = self.parse.default

    override def executionContext: ExecutionContext = self.defaultExecutionContext

    protected def auth[T](
        request: Request[T],
        museumId: Option[MuseumId],
        maybeToken: Option[BearerToken]
    )(
        authorize: AuthFunc[T]
    )(implicit ec: ExecutionContext): MusitActionResultF[T] = {
      val museum = museumId.flatMap(Museum.fromMuseumId)
      maybeToken.map { token =>
        val res = for {
          session  <- MusitResultT(authService.touch(token))
          userInfo <- MusitResultT(authService.userInfo(token))
          groups   <- MusitResultT(authService.groups(userInfo))
        } yield {
          logger.debug(s"Got Groups\n${groups.map(_.name).mkString(", ")}")
          val authUser = AuthenticatedUser(session, userInfo, groups)
          authorize(token, userInfo, authUser, museum)
        }
        res.value.map {
          case MusitSuccess(actionResult) =>
            actionResult

          case MusitNotAuthenticated(msg) =>
            Left(Unauthorized(Json.obj("message" -> msg)))

          case MusitNotAuthorized() =>
            Left(Forbidden)

          case _ =>
            Left(Unauthorized)
        }
      }.getOrElse {
        Future.successful(Left(Unauthorized))
      }
    }

  }

  // scalastyle:off method.name

  /**
   * A custom Action that checks if the user is authenticated. If the request
   * contains a valid bearer token, the request is enriched with an
   * {{{AuthenticatedUser}}}. If the incoming request can't be authenticated
   * a {{{Result}}} with HTTP Forbidden is returned.
   *
   * @param museumId    An Option with the MuseumId for which the request wants info
   * @param module      An Option with the Module the request is limited to serve
   * @param permissions Varargs of Permission restrict who is authorized.
   */
  case class MusitSecureAction(
      museumId: Option[MuseumId],
      module: Option[ModuleConstraint],
      permissions: Permission*
  ) extends BaseSecureAction {

    implicit val ec = executionContext

    override def refine[T](request: Request[T]): MusitActionResultF[T] = {
      val maybeToken = BearerToken.fromRequestHeader(request)
      auth(request, museumId, maybeToken) { (token, userInfo, authUser, museum) =>
        museum match {
          case Some(m) =>
            authUser
              .authorize(m, module, permissions)
              .map { _ =>
                Right(MusitRequest(authUser, token, museum, request))
              }
              .getOrElse {
                logger.debug(s"Action is unauthorized for ${userInfo.id}")
                Left(Forbidden)
              }

          case None =>
            if (museumId.isDefined) {
              Left(BadRequest(Json.obj("message" -> s"Unknown museum $museumId")))
            } else {
              Right(MusitRequest(authUser, token, museum, request))
            }
        }
      }
    }

  }

  object MusitSecureAction {
    def apply(): MusitSecureAction = MusitSecureAction(None, None)

    def apply(mid: MuseumId): MusitSecureAction = MusitSecureAction(Some(mid), None)

    def apply(
        permissions: Permission*
    ): MusitSecureAction = MusitSecureAction(None, None, permissions: _*)

    def apply(
        mid: MuseumId,
        permissions: Permission*
    ): MusitSecureAction = MusitSecureAction(Some(mid), None, permissions: _*)

    def apply(
        mid: MuseumId,
        module: ModuleConstraint,
        permissions: Permission*
    ): MusitSecureAction = MusitSecureAction(Some(mid), Some(module), permissions: _*)
  }

  // scalastyle:on method.name

}

trait MusitAdminActions extends MusitActions {
  self: BaseController =>

  private val logger = Logger(classOf[MusitAdminActions])

  /**
   * Crypto implementation to handle special cases for parsing the access token.
   */
  val crypto: MusitCrypto

  /**
   * Play Action that should be used for endpoints that require admin level
   * authorization. The lowest allowable permission is {{{MusitAdmin}}}.
   *
   * @param museumId    The MuseumId to access.
   * @param permissions The permissions required to access the endpoint.
   */
  case class MusitAdminAction(
      museumId: Option[MuseumId],
      module: Option[ModuleConstraint],
      permissions: ElevatedPermission*
  ) extends BaseSecureAction {

    implicit val ec = executionContext

    override def refine[T](request: Request[T]): MusitActionResultF[T] = {
      val maybeToken = BearerToken
        .fromRequestHeader(request)
        .orElse(
          request.getQueryString("_at").map { qs =>
            val decrypted = crypto.decryptAES(qs)
            BearerToken(decrypted)
          }
        )

      auth(request, museumId, maybeToken) { (token, userInfo, authUser, museum) =>
        authUser
          .authorizeAdmin(museum, module, permissions)
          .map { _ =>
            Right(MusitRequest(authUser, token, museum, request))
          }
          .getOrElse {
            logger.debug(s"Action is unauthorized for ${userInfo.id}")
            Left(Forbidden)
          }
      }
    }
  }

  object MusitAdminAction {
    def apply(): MusitAdminAction = MusitAdminAction(permissions = MusitAdmin)

    def apply(mid: MuseumId): MusitAdminAction = MusitAdminAction(Some(mid), None)

    def apply(permissions: ElevatedPermission*): MusitAdminAction =
      MusitAdminAction(None, None, permissions: _*)

    def apply(
        mid: MuseumId,
        permissions: ElevatedPermission*
    ): MusitAdminAction = MusitAdminAction(Some(mid), None, permissions: _*)
  }

}
