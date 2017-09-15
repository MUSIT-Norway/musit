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
      val isPermitted      = permissionsFor(museum).exists(_.priority >= lowest.priority)
      val allowedForModule = module.forall(m => groups.exists(_.module == m))
      if (isPermitted && allowedForModule) MusitSuccess(())
      else MusitNotAuthorized()
    } else {
      MusitNotAuthorized()
    }
  }

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
