package models

import no.uio.musit.models.Museums.Museum
import no.uio.musit.models.{GroupId, MuseumId}
import no.uio.musit.security.Permissions.{Permission, Unspecified}
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{Format, Json, Reads}

case class Group(
    id: GroupId,
    name: String,
    permission: Permission,
    museumId: MuseumId,
    description: Option[String]
)

object Group {

  implicit def format: Format[Group] = Json.format[Group]

  def fromGroupAdd(gid: GroupId, ga: GroupAdd): Group =
    Group(gid, ga.name, ga.permission, ga.museumId, ga.description)
}

case class GroupAdd(
    name: String,
    permission: Permission,
    museumId: MuseumId,
    description: Option[String]
)

object GroupAdd {

  implicit def reads: Reads[GroupAdd] = Json.reads[GroupAdd]

  def applyForm(name: String, permInt: Int, mid: Int, maybeDesc: Option[String]) =
    GroupAdd(name, Permission.fromInt(permInt), MuseumId(mid), maybeDesc)

  def unapplyForm(g: GroupAdd) =
    Some((g.name, g.permission.priority, g.museumId.underlying, g.description))

  val groupAddForm = Form(
    mapping(
      "name"        -> text(minLength = 3),
      "permission"  -> number.verifying(Permission.fromInt(_) != Unspecified),
      "museum"      -> number.verifying(m => Museum.fromMuseumId(MuseumId(m)).nonEmpty),
      "description" -> optional(text)
    )(applyForm)(unapplyForm)
  )

}
