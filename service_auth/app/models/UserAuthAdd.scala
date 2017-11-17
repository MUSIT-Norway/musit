package models

import no.uio.musit.models.{CollectionUUID, Email, GroupId}
import play.api.data.Forms._
import play.api.data.{Form, Mapping}
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

  val userAuthForm: Form[UserAuthAdd] = Form(
    mapping(
      "email"   -> email,
      "groupId" -> text.verifying(id => GroupId.validate(id).isSuccess),
      "collections" -> optional(
        list(uuid).transform[List[CollectionUUID]](
          _.map(CollectionUUID.apply),
          _.map(_.underlying)
        )
      )
    )(UserAuthAdd.apply)(UserAuthAdd.unapply)
  )
}

// TODO: Remove above code when below is completed.

case class UserAdd(
    email: Email,
    maybeDbCoord: Option[GroupId],
    accesses: List[ModuleAddAccess]
)

object UserAdd {
  val form: Form[UserAdd] = Form(
    mapping(
      "email"    -> email.transform[Email](Email.apply, _.value),
      "db_coord" -> optional(uuid.transform[GroupId](GroupId.apply, _.underlying)),
      "modules"  -> list[ModuleAddAccess](ModuleAddAccess.formMapping)
    )(UserAdd.apply)(UserAdd.unapply)
  )
}

case class ModuleAddAccess(
    module: Int,
    aa: List[AddAccess]
)

object ModuleAddAccess {

  val formMapping: Mapping[ModuleAddAccess] = mapping(
    "moduleId" -> number,
    "groups"   -> list[AddAccess](AddAccess.addAccessFormMapping)
  )(ModuleAddAccess.apply)(ModuleAddAccess.unapply)

}

case class AddAccess(
    groupId: GroupId,
    collections: Option[List[CollectionUUID]]
) {

  def hasNoCollections: Boolean = !collections.exists(_.nonEmpty)

}

object AddAccess {
  val addAccessFormMapping: Mapping[AddAccess] = mapping(
    "groupId" -> uuid.transform[GroupId](GroupId.apply, _.underlying),
    "collections" -> optional(
      list(uuid).transform[List[CollectionUUID]](
        _.map(CollectionUUID.apply),
        _.map(_.underlying)
      )
    )
  )(AddAccess.apply)(AddAccess.unapply)
}
