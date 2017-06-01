package no.uio.musit.security

import no.uio.musit.MusitResults.MusitResult
import no.uio.musit.models.{ActorId, Email}
import play.api.Logger

import scala.concurrent.{ExecutionContext, Future}

trait AuthResolver {

  private val logger = Logger(classOf[AuthResolver])

  /**
   * Locate the GroupInfos where the provided feide Email is a member.
   *
   * @param email Email with the feide username
   * @param ec    ExecutionContext
   * @return A MusitResult with a Seq of GroupInfo where the user is a member.
   */
  def findGroupInfoByFeideEmail(
      email: Email
  )(implicit ec: ExecutionContext): Future[MusitResult[Seq[GroupInfo]]]

  /**
   * Persist the provided UserInfo.
   *
   * @param userInfo UserInfo to save.
   * @param ec       ExecutionContext
   * @return a MusitResult[Unit].
   */
  def saveUserInfo(
      userInfo: UserInfo
  )(implicit ec: ExecutionContext): Future[MusitResult[Unit]]

  /**
   * Find the UserInfo data for the given ActorId
   *
   * @param userId ActorId
   * @param ec     ExecutionContext
   * @return MusitResult of an Option of UserInfo
   */
  def userInfo(
      userId: ActorId
  )(implicit ec: ExecutionContext): Future[MusitResult[Option[UserInfo]]]

  /**
   * Prepares a new persistent user session
   */
  def sessionInit(
      client: Option[String]
  )(implicit ec: ExecutionContext): Future[MusitResult[SessionUUID]] = {
    logger.debug("Initialize a new UserSession with a generated SessionUUID")
    upsertUserSession(UserSession.prepare(client))
  }

  def upsertUserSession(
      session: UserSession
  )(implicit ec: ExecutionContext): Future[MusitResult[SessionUUID]]

  /**
   * Fetch the UserSession with the given SessionUUID
   *
   * @param sessionUUID SessionUUID to fetch
   * @param ec          ExecutionContext
   * @return MusitResult containing the located UserSession
   */
  def userSession(
      sessionUUID: SessionUUID
  )(implicit ec: ExecutionContext): Future[MusitResult[Option[UserSession]]]

  /**
   * Save the changes to the provided UserSession.
   *
   * @param userSession the UserSession to save
   * @param ec ExecutionContext
   * @return a MusitResult[Unit]
   */
  def updateSession(
      userSession: UserSession
  )(implicit ec: ExecutionContext): Future[MusitResult[Unit]]

}
