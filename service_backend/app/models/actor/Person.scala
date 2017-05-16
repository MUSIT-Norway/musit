package models.actor

import no.uio.musit.models.{ActorId, DatabaseId, Email}
import no.uio.musit.security.{AuthenticatedUser, UserInfo}
import play.api.libs.json._

case class Person(
    id: Option[DatabaseId],
    fn: String,
    title: Option[String] = None,
    role: Option[String] = None,
    tel: Option[String] = None,
    web: Option[String] = None,
    email: Option[Email] = None,
    dataportenId: Option[ActorId] = None,
    dataportenUser: Option[String] = None,
    applicationId: Option[ActorId] = None
)

object Person {
  val tupled          = (Person.apply _).tupled
  implicit val format = Json.format[Person]

  def fromUserInfo(user: UserInfo): Person = {
    Person(
      id = None,
      fn = user.name.getOrElse(""),
      email = user.email,
      dataportenId = Option(user.id),
      dataportenUser = user.feideUser.map(_.value)
    )
  }

  def fromAuthUser(user: AuthenticatedUser): Person = fromUserInfo(user.userInfo)
}
