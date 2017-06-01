package models

import no.uio.musit.models.{CollectionUUID, GroupId}
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{Format, Json}

/**
 * Command message for adding a user to an auth group with access to
 * specified museum collections in the form of CollectionUUID's.
 */
case class UserAuthAdd(
    email: String,
    groupId: String,
    collections: Option[List[CollectionUUID]]
)

object UserAuthAdd {
  implicit val formats: Format[UserAuthAdd] = Json.format[UserAuthAdd]

  def applyForm(
      email: String,
      groupId: String,
      collections: Option[List[CollectionUUID]]
  ) = UserAuthAdd(email, groupId, collections)

  def unapplyForm(uga: UserAuthAdd) = Some((uga.email, uga.groupId, uga.collections))

  val userAuthForm = Form(
    mapping(
      "email"   -> email,
      "groupId" -> text.verifying(id => GroupId.validate(id).isSuccess),
      "collections" -> optional(
        list(uuid).transform[List[CollectionUUID]](
          _.map(CollectionUUID.apply),
          _.map(_.underlying)
        )
      )
    )(applyForm)(unapplyForm)
  )
}
