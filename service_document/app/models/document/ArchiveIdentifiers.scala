package models.document

import net.scalytica.symbiotic.api.types.IdOps
import net.scalytica.symbiotic.api.types.PartyBaseTypes.{OrgId, UserId}
import no.uio.musit.models.{ActorId, CollectionUUID, MuseumId}

object ArchiveIdentifiers {

  /**
   * An ArchiveUserId is a module specific UserId that's necessary for using the
   * underlying document management library. It acts on a generic UserId type that
   * the implementor must provide an implementation for. In the MUSIT case, this
   * means we need to provide mapping between {{{ActorId}}} and {{{UserId}}}. In
   * addition, the companion of the {{{UserId}}} implementation provides a couple
   * of useful functions.
   *
   * @param value The ID value as a String
   */
  case class ArchiveUserId(value: String) extends UserId

  object ArchiveUserId extends IdOps[ArchiveUserId] {

    // Implicit converters to help working with the ArchiveUserId.

    override implicit def asId(s: String): ArchiveUserId = ArchiveUserId(s)

    implicit def actorId2archiveUserId(id: ActorId): ArchiveUserId =
      ArchiveUserId(id.asString)

    implicit def archiveUserId2ActorId(id: ArchiveUserId): ActorId =
      ActorId.unsafeFromString(id.value)
  }

  /**
   * A unique identifier that represents the owner of an archive item. By
   * default, in the MUSIT system, all archive items are owned by the Museum.
   *
   * The ArchiveOwnerId is typically used together with the {{{ArchiveContext}}}.
   * Where it is typically used to identify which folder tree to access per call.
   */
  case class ArchiveOwnerId(value: String) extends OrgId

  object ArchiveOwnerId extends IdOps[ArchiveOwnerId] {

    def apply(
        mid: MuseumId
    ): ArchiveOwnerId = ArchiveOwnerId(s"${mid.underlying}")

    // Implicit converters to help working with the ArchiveOwnerId

    override implicit def asId(s: String): ArchiveOwnerId = ArchiveOwnerId(s)

    implicit def midAsId(mid: MuseumId): ArchiveOwnerId = ArchiveOwnerId(mid)
  }

  /**
   * The ArchiveCollectionId is used as a grouping mechanism in archive system.
   * In the underlying library, it is stored in the {{{ManagedMetadata#accessibleBy}}}
   * list attribute.
   *
   * From a MUSIT perspective, it will constrain access to a {{{ManagedFile}}} to
   * the users with access to the given Collection.
   */
  case class ArchiveCollectionId(value: String) extends OrgId

  object ArchiveCollectionId extends IdOps[ArchiveCollectionId] {

    def apply(cid: CollectionUUID): ArchiveCollectionId =
      ArchiveCollectionId(cid.asString)

    // Implicit converters to help working with the ArchiveCollectionId

    override implicit def asId(s: String): ArchiveCollectionId = ArchiveCollectionId(s)

    implicit def cidAsId(cid: CollectionUUID): ArchiveCollectionId =
      ArchiveCollectionId(cid)

  }

}
