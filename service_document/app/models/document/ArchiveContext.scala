package models.document

import models.document.ArchiveIdentifiers._
import net.scalytica.symbiotic.api.types.PartyBaseTypes.{OrgId, PartyId, UserId}
import net.scalytica.symbiotic.api.types.ResourceParties._
import net.scalytica.symbiotic.api.types.SymbioticContext
import no.uio.musit.models.{ActorId, CollectionUUID, MuseumId}
import no.uio.musit.security.{AuthenticatedUser, DocumentArchive}

/**
 * All operations towards the underlying symbiotic library require that a
 * context is passed with each API call. The context is used primarily to
 * calculate authorisations to data in a specific folder tree.
 *
 * Current user informs the library _who_ is interacting with the folder tree.
 *
 * The Owner attribute tells the library who owns the root of the folder tree
 * (and down).
 *
 * Accessible Parties is used to provide information about what the current user
 * can access. Basically, the current user can access all the folders where the
 * parties in this list are granted access.
 *
 * In this system we operate with 2 different contexts. Where one of them is
 * exclusively used for adding _new_ data. Either a new folder, or a new version
 * of a file. Passing in the wrong context to a method requiring an
 * [[ArchiveAddContext]] is not allowed.
 *
 * Since these two contexts are used for different use-cases, they are also
 * populated slightly different. For the [[ArchiveContext]] the accessible
 * parties attribute works exactly as explained above. But for the
 * [[ArchiveAddContext]], accessible parties is used to tell the system which
 * other parties should be granted access to the resource upon saving it.
 *
 *
 */
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
      collections = authUser.collectionsFor(mid, DocumentArchive).map(_.uuid)
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
      maybeCollection: Option[CollectionUUID]
  ): ArchiveAddContext = {
    val ownerId = ArchiveOwnerId(mid)
    ArchiveAddContext(
      currentUser = authUser.id,
      owner = Owner(ownerId),
      collections = maybeCollection.toSeq
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

  implicit def toArchiveContext(addCtx: ArchiveAddContext): ArchiveContext = {
    ArchiveContext(
      currentUser = addCtx.currentUser,
      owner = addCtx.owner,
      collections = addCtx.collections
    )
  }

}
