package models.document

import models.document.ArchiveIdentifiers._
import net.scalytica.symbiotic.api.types.PartyBaseTypes.{OrgId, PartyId, UserId}
import net.scalytica.symbiotic.api.types.ResourceParties._
import net.scalytica.symbiotic.api.types.SymbioticContext
import no.uio.musit.models.{ActorId, CollectionUUID, MuseumId}
import no.uio.musit.security.AuthenticatedUser

case class ArchiveContext(
    currentUser: ArchiveUserId,
    owner: Owner,
    accessibleParties: Seq[PartyId]
) extends SymbioticContext {

  override def toOrgId(str: String): OrgId = ArchiveOwnerId.asId(str)

  override def toUserId(str: String): UserId = ArchiveUserId.asId(str)
}

object ArchiveContext {

  def apply(
      currentUser: ActorId,
      mid: MuseumId,
      collections: Seq[CollectionUUID]
  ): ArchiveContext = {
    val ownerId = ArchiveOwnerId(mid)
    val acids   = collections.map(c => c: ArchiveCollectionId)
    ArchiveContext(currentUser, Owner(ownerId), acids)
  }

  def apply(
      authUser: AuthenticatedUser,
      mid: MuseumId
  ): ArchiveContext = {
    ArchiveContext(
      currentUser = authUser.id,
      mid = mid,
      collections = authUser.collectionsFor(mid).map(_.uuid)
    )
  }

}
