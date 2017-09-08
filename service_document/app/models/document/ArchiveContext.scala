package models.document

import models.document.ArchiveIdentifiers._
import net.scalytica.symbiotic.api.types.PartyBaseTypes.{OrgId, PartyId, UserId}
import net.scalytica.symbiotic.api.types.ResourceParties._
import net.scalytica.symbiotic.api.types.SymbioticContext
import no.uio.musit.models.{ActorId, CollectionUUID, MuseumId}
import no.uio.musit.security.AuthenticatedUser

sealed trait BaseArchiveContext extends SymbioticContext {

  val collections: Seq[CollectionUUID]

  lazy val accessibleParties: Seq[PartyId] =
    owner.id +: collections.map(c => c: ArchiveCollectionId)

  override def toOrgId(str: String): OrgId = ArchiveOwnerId.asId(str)

  override def toUserId(str: String): UserId = ArchiveUserId.asId(str)

}

case class ArchiveContext(
    currentUser: ArchiveUserId,
    owner: Owner,
    collections: Seq[CollectionUUID]
) extends BaseArchiveContext

object ArchiveContext {

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

  def apply(
      currentUser: ActorId,
      mid: MuseumId,
      collections: Seq[CollectionUUID]
  ): ArchiveContext = {
    val ownerId = ArchiveOwnerId(mid)
    ArchiveContext(currentUser, Owner(ownerId), collections)
  }

}

// TODO: Document the differences between ArchiveContext and ArchiveAddContext

case class ArchiveAddContext(
    currentUser: ArchiveUserId,
    owner: Owner,
    collections: Seq[CollectionUUID]
) extends BaseArchiveContext {

  val collection: Option[ArchiveCollectionId] = collections.headOption

}

object ArchiveAddContext {

  def apply(
      authUser: AuthenticatedUser,
      mid: MuseumId
  ): ArchiveAddContext = {
    ArchiveAddContext(
      authUser = authUser,
      mid = mid,
      collections = Seq.empty
    )
  }

  def apply(
      authUser: AuthenticatedUser,
      mid: MuseumId,
      collection: CollectionUUID
  ): ArchiveAddContext = {
    val ownerId = ArchiveOwnerId(mid)
    ArchiveAddContext(
      currentUser = authUser.id,
      owner = Owner(ownerId),
      collections = Seq(collection)
    )
  }

  def apply(
      authUser: AuthenticatedUser,
      mid: MuseumId,
      collections: Seq[CollectionUUID]
  ): ArchiveAddContext = {
    val ownerId = ArchiveOwnerId(mid)
    ArchiveAddContext(authUser.id, Owner(ownerId), collections)
  }
}
