package models.storage.event.dto

import no.uio.musit.models.{EventId, MuseumId, ObjectId, StorageNodeDatabaseId}

case class LocalObject(
    objectId: ObjectId,
    latestMoveId: EventId,
    currentLocationId: StorageNodeDatabaseId,
    museumId: MuseumId,
    objectType: String
)
