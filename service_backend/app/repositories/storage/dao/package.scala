package repositories.storage

package object dao {

  val SchemaName = Some("MUSARK_STORAGE")

  val StorageEventTable = "NEW_EVENT"
  val OrganisationTable = "ORGANISATION"
  val BuildingTable     = "BUILDING"
  val RoomTable         = "ROOM"
  val StorageNodeTable  = "STORAGE_NODE"

}
