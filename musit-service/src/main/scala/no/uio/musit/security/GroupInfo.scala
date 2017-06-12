package no.uio.musit.security

import no.uio.musit.models.Museums.Museum
import no.uio.musit.models.{GroupId, MuseumCollection, MuseumId}
import no.uio.musit.security.Permissions.Permission
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class GroupInfo(
    id: GroupId,
    name: String,
    module: ModuleConstraint,
    permission: Permission,
    museumId: MuseumId,
    description: Option[String],
    collections: Seq[MuseumCollection]
) {

  val museum: Option[Museum] = Museum.fromMuseumId(museumId)

  def hasPermission(p: Permission): Boolean = permission == p

}

object GroupInfo {

  implicit val formats: Format[GroupInfo] = (
    (__ \ "id").format[GroupId] and
      (__ \ "name").format[String] and
      (__ \ "module").format[ModuleConstraint] and
      (__ \ "permission").format[Permission] and
      (__ \ "museumId").format[MuseumId] and
      (__ \ "description").formatNullable[String] and
      (__ \ "collections").format[Seq[MuseumCollection]]
  )(GroupInfo.apply, unlift(GroupInfo.unapply))

  def fromTuple(
      t: (GroupId, String, ModuleConstraint, Permission, MuseumId, Option[String])
  ): GroupInfo = {
    GroupInfo(
      id = t._1,
      name = t._2,
      module = t._3,
      permission = t._4,
      museumId = t._5,
      description = t._6,
      collections = Seq.empty
    )
  }

}
