package no.uio.musit.models

trait MusitId {
  val underlying: Long
  override def toString(): String = underlying.toString();
}

object MusitId {

  implicit def asStorageNodeId(m: MusitId): StorageNodeDatabaseId =
    StorageNodeDatabaseId(m.underlying)

  implicit def asEventId(m: MusitId): EventId =
    EventId(m.underlying)

  implicit def asObjectId(m: MusitId): ObjectId =
    ObjectId(m.underlying)

}
