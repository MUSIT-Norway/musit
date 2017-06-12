package controllers.web

import com.google.inject.Inject
import controllers.web
import models._
import no.uio.musit.MusitResults.{MusitError, MusitSuccess}
import no.uio.musit.functional.Implicits.futureMonad
import no.uio.musit.functional.MonadTransformers.MusitResultT
import no.uio.musit.models.Museums.Museum
import no.uio.musit.models._
import no.uio.musit.security.Permissions.Permission
import no.uio.musit.security.crypto.MusitCrypto
import no.uio.musit.security.{
  AuthenticatedUser,
  Authenticator,
  EncryptedToken,
  GroupInfo
}
import no.uio.musit.service.MusitAdminController
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repositories.auth.dao.AuthDao

class UserController @Inject()(
    implicit val authService: Authenticator,
    val crypto: MusitCrypto,
    val dao: AuthDao,
    val messagesApi: MessagesApi
) extends MusitAdminController
    with I18nSupport {

  def users = MusitAdminAction().async { implicit request =>
    val encTok = EncryptedToken.fromBearerToken(request.token)
    dao.allUsers.map {
      case MusitSuccess(usrs) =>
        Ok(views.html.users(request.user, encTok, usrs))

      case err: MusitError =>
        InternalServerError(views.html.error(request.user, encTok, err.message))
    }
  }

  def usersPerModule(mid: Int) = MusitAdminAction().async { implicit request =>
    val encTok = EncryptedToken.fromBearerToken(request.token)
    Museum
      .fromMuseumId(mid)
      .map { museum =>
        dao.findUserDetailsForMuseum(mid).map {
          case MusitSuccess(res) =>
            Ok(views.html.museumUsersInModules(request.user, encTok, museum, res))

          case err: MusitError =>
            serverErr(
              request.user,
              encTok,
              s"An error occurred fetching users for museum $mid"
            )
        }
      }
      .getOrElse {
        badRequestF(request.user, encTok, s"Invalid museum ID $mid")
      }
  }

  def userView(email: String, mid: Option[Int]) =
    MusitAdminAction().async { implicit request =>
      val encTok     = EncryptedToken.fromBearerToken(request.token)
      val feideEmail = Email.fromString(email)

      dao.findUserDetails(feideEmail).map {
        case MusitSuccess(mud) =>
          mud.map { ud =>
            Ok(
              views.html.userView(
                request.user,
                encTok,
                ud,
                mid.flatMap(i => Museum.fromMuseumId(i))
              )
            )
          }.getOrElse {
            badRequest(request.user, encTok, s"Could not find user $email")
          }

        case err: MusitError =>
          serverErr(request.user, encTok, err.message)
      }
    }

  // scalastyle:off
  private def processUserViewData(
      feideEmail: Email,
      userPermissions: Option[UserPermissions],
      userGroups: Seq[GroupInfo],
      groups: Seq[Group],
      collections: Seq[MuseumCollection],
      mid: Option[MuseumId]
  )(implicit token: EncryptedToken, currUser: AuthenticatedUser) = {
    val currAccesses = UserAdd(
      email = feideEmail,
      accesses = userGroups.groupBy(_.module).toList.map { a =>
        ModuleAddAccess(
          module = a._1.id,
          aa = a._2.toList.map { ugm =>
            AddAccess(ugm.id, Option(ugm.collections.toList.map(_.uuid)))
          }
        )
      }
    )

    userPermissions.map { userPermissions =>
      Ok(
        views.html.userEdit(
          user = currUser,
          etok = token,
          selectedUser = userPermissions,
          museum = mid.flatMap(i => Museum.fromMuseumId(i)),
          collections = collections,
          groups = groups
        )
      )
    }.getOrElse {
      badRequest(currUser, token, s"Could not find details for ${feideEmail.value}")
    }
  }

  // scalastyle:on

  private def fetchUserViewData(feideEmail: Email, maybeMuseumId: Option[MuseumId]) = {
    val res = for {
      up   <- MusitResultT(dao.findUserDetails(feideEmail))
      usrg <- MusitResultT(dao.findGroupInfoFor(feideEmail))
      grps <- MusitResultT(maybeMuseumId.map(dao.allGroupsFor).getOrElse(dao.allGroups))
      cols <- MusitResultT(dao.allCollections)
    } yield (up, usrg, grps, cols)

    res.value
  }

  def userEditView(email: String, mid: Option[Int]) =
    MusitAdminAction().async { implicit request =>
      implicit val encTok  = EncryptedToken.fromBearerToken(request.token)
      implicit val fe      = Email.fromString(email)
      implicit val currUsr = request.user
      val maybeMid         = mid.map(i => MuseumId.fromInt(i))

      fetchUserViewData(fe, maybeMid).map {
        case MusitSuccess((up, usrg, grps, cols)) =>
          processUserViewData(fe, up, usrg, grps, cols, maybeMid)

        case err: MusitError =>
          serverErr(currUsr, encTok, err.message)
      }
    }

  def userAddView(
      mid: Int
  ) = MusitAdminAction().async { implicit request =>
    val encTok = EncryptedToken.fromBearerToken(request.token)
    Museum
      .fromMuseumId(mid)
      .map { museum =>
        dao.allCollections.flatMap { cols =>
          dao.allGroups.map { groups =>
            Ok(
              views.html.museumUserAdd(
                request.user,
                encTok,
                UserAdd.form,
                museum,
                cols.getOrElse(Seq.empty),
                groups.getOrElse(Seq.empty)
              )
            )
          }
        }
      }
      .getOrElse {
        badRequestF(request.user, encTok, s"Invalid museum Id: $mid")
      }
  }

  // scalastyle:off method.length
  def userAddPost(
      mid: Int
  ) = MusitAdminAction().async { implicit request =>
    val encTok = EncryptedToken.fromBearerToken(request.token)

    Museum
      .fromMuseumId(MuseumId.fromInt(mid))
      .map { museum =>
        UserAdd.form
          .bindFromRequest()
          .fold(
            formWithErrors => {
              (for {
                cols <- MusitResultT(dao.allCollections)
                grps <- MusitResultT(dao.allGroups)
              } yield {
                BadRequest(
                  views.html.museumUserAdd(
                    request.user,
                    encTok,
                    formWithErrors,
                    museum,
                    cols,
                    grps
                  )
                )
              }).value.map(_.getOrElse {
                serverErr(
                  request.user,
                  encTok,
                  s"An error occurred trying to fetch required data"
                )
              })
            },
            userAdd => {
              val response = for {
                grps <- MusitResultT(dao.allGroups)
                acessesToAdd <- MusitResultT
                                 .successful(cleanAddAccessList(userAdd.accesses, grps))
                added <- MusitResultT(
                          dao.addUserToGroups(userAdd.email, acessesToAdd)
                        )
              } yield {
                Redirect(
                  url = controllers.web.routes.UserController.usersPerModule(mid).url,
                  queryString = Map("_at" -> Seq(encTok.asString))
                ).flashing("success" -> "User added!")
              }

              response.value.map {
                case MusitSuccess(success) => success
                case err: MusitError       => serverErr(request.user, encTok, err.message)
              }
            }
          )
      }
      .getOrElse(badRequestF(request.user, encTok, s"Invalid museum Id $mid"))
  }

  // scalastyle:on method.length

  /**
   * Strip away any AddAccess entries that would give access to normal permission
   * without any collections assigned. This is necessary because of the way HTML
   * forms pass in data.
   */
  private def cleanAddAccessList(
      maas: List[ModuleAddAccess],
      grps: Seq[Group]
  ): List[AddAccess] = {
    maas.flatMap { ma =>
      ma.aa.filterNot { aa =>
        aa.hasNoCollections &&
        grps.exists(g => g.id == aa.groupId && !Permission.isElevated(g.permission))
      }
    }
  }

  def revoke(
      email: String,
      gid: String,
      cid: String,
      mid: Option[Int]
  ) = MusitAdminAction().async { implicit request =>
    val encTok = EncryptedToken.fromBearerToken(request.token)

    Email
      .validate(email)
      .map { feideEmail =>
        GroupId
          .validate(gid)
          .toOption
          .map(GroupId.apply)
          .map { groupId =>
            CollectionUUID
              .validate(cid)
              .toOption
              .map(CollectionUUID.apply)
              .map { colId =>
                dao.revokeCollectionFor(feideEmail, groupId, colId).map {
                  case MusitSuccess(res) =>
                    Redirect(
                      url = web.routes.UserController.userEditView(email, mid).url,
                      queryString = Map("_at" -> Seq(encTok.asString))
                    ).flashing("success" -> "Collection access revoked")
                  case err: MusitError =>
                    serverErr(request.user, encTok, err.message)
                }
              }
              .getOrElse(notFoundF(request.user, encTok, s"Wrong uuid format: $cid"))
          }
          .getOrElse(notFoundF(request.user, encTok, s"Wrong uuid format: $gid"))
      }
      .getOrElse(notFoundF(request.user, encTok, s"Not a valid email: $email"))
  }

  def grant(
      email: String,
      gid: String,
      cid: String,
      mid: Option[Int]
  ) = MusitAdminAction().async { implicit request =>
    val encTok = EncryptedToken.fromBearerToken(request.token)

    Email
      .validate(email)
      .map { feideEmail =>
        GroupId
          .validate(gid)
          .toOption
          .map(GroupId.apply)
          .map { groupId =>
            CollectionUUID
              .validate(cid)
              .toOption
              .map(CollectionUUID.apply)
              .map { colId =>
                dao.addUserToGroup(feideEmail, groupId, Option(Seq(colId))).map {
                  case MusitSuccess(res) =>
                    Redirect(
                      url = web.routes.UserController.userEditView(email, mid).url,
                      queryString = Map("_at" -> Seq(encTok.asString))
                    )

                  case err: MusitError =>
                    serverErr(request.user, encTok, err.message)
                }
              }
              .getOrElse(notFoundF(request.user, encTok, s"Wrong uuid format: $cid"))
          }
          .getOrElse(notFoundF(request.user, encTok, s"Wrong uuid format: $gid"))
      }
      .getOrElse(notFoundF(request.user, encTok, s"Not a valid email: $email"))
  }

}
