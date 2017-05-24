package models.storage

import no.uio.musit.models.ObjectTypes.ObjectType
import no.uio.musit.models.{EventId, MuseumId, ObjectUUID, StorageNodeId}

case class LocalObject(
    objectId: ObjectUUID,
    latestMoveId: EventId,
    currentLocationId: StorageNodeId,
    museumId: MuseumId,
    objectType: ObjectType
)
