package models

import no.uio.musit.models.MuseumCollections.Collection
import no.uio.musit.models._
import no.uio.musit.security.Permissions.Permission
import no.uio.musit.security.{ModuleConstraint, UserInfo}

case class UserPermissions(
    feideEmail: Email,
    name: Option[String],
    email: Option[Email],
    userId: Option[ActorId],
    access: Seq[ModuleAccess] = Seq.empty
) {

  def appendModule(mod: ModuleAccess): UserPermissions = {
    this.access
      .find(_.module.id == mod.module.id)
      .map(_ => this)
      .getOrElse(copy(access = mod +: this.access))
  }

  def addPermission(
      mid: MuseumId,
      module: ModuleConstraint,
      permission: Permission,
      collection: Option[Collection]
  ): UserPermissions =
    access.zipWithIndex
      .find(m => m._1.mid == mid && m._1.module == module)
      .map {
        case (mod, index) =>
          copy(
            access = access.updated(index, mod.appendPermission(permission, collection))
          )
      }
      .getOrElse {
        appendModule(ModuleAccess(mid, module, Seq(permission -> collection)))
      }

}

case class ModuleAccess(
    mid: MuseumId,
    module: ModuleConstraint,
    permissions: Seq[(Permission, Option[Collection])]
) {

  def appendPermission(perm: Permission, col: Option[Collection]): ModuleAccess = {
    permissions.find(cp => cp._1 == perm && cp._2 == col).map(_ => this).getOrElse {
      this.copy(permissions = (perm -> col) +: permissions)
    }
  }

}

case class RegisteredUser(
    feideEmail: Email,
    maybeUserInfo: Option[UserInfo]
)
