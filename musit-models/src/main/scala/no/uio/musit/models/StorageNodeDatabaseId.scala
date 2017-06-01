package no.uio.musit.models

import play.api.libs.json._

//  TODO: This class should be removed and its usage should be replaced with
//    DatabaseId everywhere in the code where. It's only temporary to allow the
//    _proper_ StorageNodeId type to be a UUID. Which is the type that _should_
//    be used to reference a StorageNode eventually

/**
 * Class to give the storage node ID a strong typing.
 */
case class StorageNodeDatabaseId(underlying: Long) extends MusitId

object StorageNodeDatabaseId {

  implicit val reads: Reads[StorageNodeDatabaseId] =
    __.read[Long].map(StorageNodeDatabaseId.apply)

  implicit val writes: Writes[StorageNodeDatabaseId] =
    Writes(id => JsNumber(id.underlying))

  implicit def fromLong(l: Long): StorageNodeDatabaseId = StorageNodeDatabaseId(l)

  implicit def toLong(id: StorageNodeDatabaseId): Long = id.underlying

  implicit def fromOptLong(l: Option[Long]): Option[StorageNodeDatabaseId] =
    l.map(fromLong)

  implicit def toOptLong(id: Option[StorageNodeDatabaseId]): Option[Long] =
    id.map(toLong)

}
