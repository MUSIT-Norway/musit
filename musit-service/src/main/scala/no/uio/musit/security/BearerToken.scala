package no.uio.musit.security

import no.uio.musit.models.MusitUUID
import play.api.http.HeaderNames.AUTHORIZATION
import play.api.libs.json.{Reads, __}
import play.api.mvc.Request

import scala.util.Try

/**
 * Value class providing a wrapper around a bearer token String.
 *
 * @param underlying String value representation of the bearer token
 */
case class BearerToken(underlying: String) extends AnyVal {

  def asHeader: (String, String) = (AUTHORIZATION, BearerToken.prefix + underlying)

}

object BearerToken {

  implicit val reads: Reads[BearerToken] = __.read[String].map(BearerToken.apply)

  val prefix = "Bearer "

  def fromMusitUUID(uuid: MusitUUID): BearerToken = {
    BearerToken(uuid.underlying.toString)
  }

  /**
   * Function to assist in extracting the BearerToken from incoming requests.
   *
   * @param request The incoming request
   * @tparam A The body content type of the incoming request
   * @return Option of BearerToken
   */
  def fromRequestHeader[A](request: Request[A]): Option[BearerToken] = {
    request.headers.get(AUTHORIZATION).find(_.startsWith(prefix)).flatMap { headerValue =>
      Try(headerValue.substring(prefix.length)).toOption.map { token =>
        BearerToken(token)
      }
    }
  }

}
