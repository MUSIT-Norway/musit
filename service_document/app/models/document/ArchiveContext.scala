package models.document

import models.document.ArchiveIdentifiers._
import net.scalytica.symbiotic.api.types.ResourceOwner.Owner
import net.scalytica.symbiotic.api.types.SymbioticContext

case class ArchiveContext(
    currentUser: ArchiveUserId,
    owner: Owner
) extends SymbioticContext {

  override def canAccess = ???

  override def toOrgId(str: String) = ArchiveOwnerId.asId(str)

  override def toUserId(str: String) = ArchiveUserId.asId(str)

}
