package no.uio.musit.security

import no.uio.musit.models.{ActorId, Email}
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class UserInfo(
    id: ActorId,
    secondaryIds: Option[Seq[String]],
    name: Option[String],
    email: Option[Email],
    picture: Option[String]
) {

  def feideUser: Option[Email] = {
    secondaryIds.flatMap(_.find { sid =>
      sid.toLowerCase.startsWith("feide") || sid.contains("@")
    }.map { fid =>
      Email.fromString(fid.reverse.takeWhile(_ != ':').reverse.trim)
    })
  }

}

object UserInfo {

  /**
   * Manually handling conversion to a tuple representation. Because we only
   * keep the "feide" part of the secondary ID in the DB (for now)
   *
   * @param userInfo the UserInfo to convert to a tuple.
   * @return A tuple representation of the UserInfo argument.
   */
  def asTuple(userInfo: UserInfo) = {
    (
      userInfo.id,
      userInfo.feideUser,
      userInfo.name,
      userInfo.email.map(e => Email.fromString(e.value)),
      userInfo.picture
    )
  }

  def removePrefix(str: String): String = str.reverse.takeWhile(_ != ':').reverse.trim

  implicit val format: Format[UserInfo] = (
    (__ \ "userid").format[ActorId] and
      (__ \ "userid_sec").formatNullable[Seq[String]] and
      (__ \ "name").formatNullable[String] and
      (__ \ "email").formatNullable[Email] and
      (__ \ "profilephoto").formatNullable[String]
  )(UserInfo.apply, unlift(UserInfo.unapply))

}
