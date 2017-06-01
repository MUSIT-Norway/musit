package no.uio.musit.security.oauth2

import no.uio.musit.security.BearerToken
import play.api.libs.json.{Reads, __}
import play.api.libs.functional.syntax._

case class OAuth2Info(
    accessToken: BearerToken,
    tokenType: Option[String] = None,
    expiresIn: Option[Long] = None,
    refreshToken: Option[String] = None,
    params: Option[Map[String, String]] = None
)

object OAuth2Info extends OAuth2Constants {

  implicit val reads: Reads[OAuth2Info] = (
    (__ \ AccessToken).read[BearerToken] and
      (__ \ TokenType).readNullable[String] and
      (__ \ ExpiresIn).readNullable[Long] and
      (__ \ RefreshToken).readNullable[String]
  )(
    (accessToken, tokenType, expiresIn, refreshToken) =>
      OAuth2Info(accessToken, tokenType, expiresIn, refreshToken)
  )

}
