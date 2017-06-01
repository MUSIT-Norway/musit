package no.uio.musit.security

import no.uio.musit.models.Museums.{All, Museum}
import no.uio.musit.models.{ActorId, CollectionUUID, MuseumCollection, MuseumId}
import no.uio.musit.security.Permissions.{ElevatedPermission, GodMode, Permission}
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

  private def permissionsFor(museum: Museum): Seq[Permission] =
    groups.filter(_.museumId == museum.id).map(gi => gi.permission)

  private def isAuthorizedFor(museum: Museum): Boolean = {
    groups.exists(g => g.museum.contains(museum) || g.museum.contains(All))
  }

  private def authorizeUser(
      museum: Museum,
      permissions: Seq[Permission]
  ): MusitResult[Unit] = {
    val lowest = lowestPermission(permissions)

    if (hasGodMode) {
      logger.debug(s"User with GodMode accessing system.")
      MusitSuccess(())
    } else if (isAuthorizedFor(museum)) {
      val allowed = permissionsFor(museum).exists(_.priority >= lowest.priority)
      if (allowed) MusitSuccess(())
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

  def canAccess(mid: MuseumId, collection: CollectionUUID): Boolean = {
    hasGodMode || collectionsFor(mid).exists(_.uuid == collection)
  }

  def authorize(
      museum: Museum,
      permissions: Seq[Permission]
  ): MusitResult[Unit] = authorizeUser(museum, permissions)

  def authorizeAdmin(
      maybeMuseum: Option[Museum],
      permissions: Seq[ElevatedPermission]
  ): MusitResult[Unit] = {
    maybeMuseum.map { museum =>
      authorizeUser(museum, permissions)
    }.getOrElse {
      // Only GodMode has access to all museums
      if (hasGodMode) MusitSuccess(())
      else MusitNotAuthorized()
    }
  }

}
