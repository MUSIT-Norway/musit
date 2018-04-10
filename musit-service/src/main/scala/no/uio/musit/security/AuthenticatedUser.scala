package no.uio.musit.security

import no.uio.musit.models.Museums.{All, Museum}
import no.uio.musit.models._
import no.uio.musit.security.Permissions.{
  ElevatedPermission,
  GodMode,
  MusitAdmin,
  Permission
}
import no.uio.musit.MusitResults.{MusitNotAuthorized, MusitResult, MusitSuccess}
import play.api.Logger

case class AuthenticatedUser(
    session: UserSession,
    userInfo: UserInfo,
    groups: Seq[GroupInfo]
) {

  val id: ActorId = userInfo.id

  private val logger = Logger(classOf[AuthenticatedUser])

  private def lowestPermission(permissions: Seq[Permission]): Permission = {
    if (permissions.nonEmpty) {
      permissions.reduceLeft((a, b) => if (a.priority > b.priority) b else a)
    } else {
      Permissions.Unspecified
    }
  }

  def hasGodMode: Boolean = groups.exists(_.hasPermission(GodMode))

  def isMusitAdmin: Boolean = groups.exists(_.hasPermission(MusitAdmin))

  private def permissionsFor(museum: Museum): Seq[Permission] =
    groups.filter(_.museumId == museum.id).map(gi => gi.permission)

  private def isAuthorizedFor(museum: Museum): Boolean = {
    groups.exists(g => g.museum.contains(museum) || g.museum.contains(All))
  }

  private def authorizeUser(
      museum: Museum,
      module: Option[ModuleConstraint],
      permissions: Seq[Permission]
  ): MusitResult[Unit] = {
    val lowest = lowestPermission(permissions)

    if (hasGodMode) {
      logger.debug(s"User with GodMode accessing system.")
      MusitSuccess(())
    } else if (isAuthorizedFor(museum)) {
      val isPermitted = permissionsFor(museum).exists(_.priority >= lowest.priority)
      val allowedForModule =
        module.forall(m => groups.exists(e => e.module == m && e.museumId == museum.id))
      if (isPermitted && allowedForModule) MusitSuccess(())
      else MusitNotAuthorized()
    } else {
      MusitNotAuthorized()
    }
  }

  def authorizeForModule(module: ModuleConstraint): MusitResult[Unit] =
    if (groups.exists(_.module == module)) MusitSuccess(())
    else MusitNotAuthorized()

  def isAuthorized(museumId: MuseumId): Boolean = {
    hasGodMode || Museum.fromMuseumId(museumId).exists(isAuthorizedFor)
  }

  def collectionsFor(museumId: MuseumId): Seq[MuseumCollection] = {
    groups.filter(_.museumId == museumId).flatMap(_.collections)
  }

  def collectionsFor(
      museumId: MuseumId,
      module: ModuleConstraint
  ): Seq[MuseumCollection] = {
    groups
      .filter(g => g.museumId == museumId && g.module == module)
      .flatMap(_.collections)
  }

  def canAccess(mid: MuseumId, collectionUUID: Option[CollectionUUID]): Boolean = {
    hasGodMode || groups.exists { g =>
      g.museumId == mid && collectionUUID.forall { id =>
        g.collections.exists(_.uuid == id)
      }
    }
  }

  def canAccess(
      mid: MuseumId,
      module: ModuleConstraint,
      collectionUUID: Option[CollectionUUID]
  ): Boolean = hasGodMode || groups.exists { g =>
    g.module == module && g.museumId == mid && collectionUUID.forall { id =>
      g.collections.exists(_.uuid == id)
    }
  }

  def authorize(
      museum: Museum,
      module: Option[ModuleConstraint],
      permissions: Seq[Permission]
  ): MusitResult[Unit] = authorizeUser(museum, module, permissions)

  def authorizeAdmin(
      maybeMuseum: Option[Museum],
      module: Option[ModuleConstraint],
      permissions: Seq[ElevatedPermission]
  ): MusitResult[Unit] = {
    maybeMuseum.map { museum =>
      authorizeUser(museum, module, permissions)
    }.getOrElse {
      // Only GodMode has access to all museums
      if (hasGodMode || isMusitAdmin) MusitSuccess(())
      else MusitNotAuthorized()
    }
  }

}

object AuthenticatedUser {

  /* A hack used when the backend needs a user object (when calling backend methods which requires a current user).
   * Some arbitrary choices here.
   *
   * */
  def createBackendUser(): AuthenticatedUser = {
    val userSession = UserSession.prepare(None)
    val userInfo = UserInfo(
      id = ActorId.fromString("10000000-0000-0000-0000-000000000000").get,
      secondaryIds = None,
      name = Some("backend user"),
      email = None,
      picture = None
    )

    /*

TODO: Create (or get) Admin group here when the backend needs to call methods which needs this group/permission

    val adminGroup = GroupInfo(   id = t._1,
      name = t._2,
      module = t._3,
      permission = t._4,
      museumId = t._5,
      description = t._6,
      collections = Seq.empty)


     */

    AuthenticatedUser(userSession, userInfo, Seq.empty)
  }

  val backendUser = createBackendUser()

}
