package models.document

import net.scalytica.symbiotic.api.types.IdOps
import net.scalytica.symbiotic.api.types.PartyBaseTypes.{OrgId, UserId}
import no.uio.musit.models.{ActorId, MuseumCollection, MuseumId}

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


  /*
    TODO:

    Need to come up with a way to assemble a uniquely identifiable
    archive owner id. This depends a lot on whether or not the root should be
    for each collection in a museum, or for just the museum.

    In any case, it _might_ be desirable to have 2 different types of owner ID.
    One being the museumId and another being the museumId + collectionId. This
    to uniquely separate the archives for each collection in a museum.

    The problem with the last solution is to allow the museum owner access to the
    museum + collection owner data.
 */

  /**
   * A unique identifier that represents the owner of an archive item. By
   * default, in the MUSIT system, all archive items are owned by the Museum.
   *
   * The ArchiveOwnerId is typically used together with the {{{ArchiveContext}}}.
   * Where it represents
   *
   * @param value
   */
  case class ArchiveOwnerId(value: String) extends OrgId

  object ArchiveOwnerId extends IdOps[ArchiveOwnerId] {

    def apply(mid: MuseumId, col: Option[MuseumCollection] = None) = {

    }

    // Implicit converters to help working with the ArchiveOwnerId

    override implicit def asId(s: String): ArchiveOwnerId = ArchiveOwnerId(s)
  }

}
