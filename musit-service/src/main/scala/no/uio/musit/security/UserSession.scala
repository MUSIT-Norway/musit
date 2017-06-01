package no.uio.musit.security

import no.uio.musit.models.ActorId
import no.uio.musit.security.oauth2.OAuth2Info
import no.uio.musit.time.dateTimeNow
import org.joda.time.DateTime

case class UserSession(
    uuid: SessionUUID,
    oauthToken: Option[BearerToken] = None,
    userId: Option[ActorId] = None,
    loginTime: Option[DateTime] = None,
    lastActive: Option[DateTime] = None,
    isLoggedIn: Boolean = false,
    tokenExpiry: Option[Long] = None,
    client: Option[String] = None
) {

  def touch(timeoutMillis: Long): UserSession = {
    val now = dateTimeNow
    copy(
      lastActive = Option(now),
      tokenExpiry = Option(now.plus(timeoutMillis).getMillis)
    )
  }

  def activate(
      oauthInfo: OAuth2Info,
      userInfo: UserInfo,
      timeoutMillis: Long
  ): UserSession = {
    val now = dateTimeNow
    this.copy(
      oauthToken = Option(oauthInfo.accessToken),
      userId = Option(userInfo.id),
      loginTime = Option(now),
      lastActive = Option(now),
      isLoggedIn = true,
      tokenExpiry = Option(now.plus(timeoutMillis).getMillis)
    )
  }

}

object UserSession {

  def prepare(client: Option[String]): UserSession =
    UserSession(uuid = SessionUUID.generate(), client = client.map(_.toLowerCase))

}
