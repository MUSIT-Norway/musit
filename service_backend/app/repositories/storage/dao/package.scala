package repositories.storage

package object dao {

  val SchemaName    = "MUSARK_STORAGE"
  val SchemaNameOpt = Some(SchemaName)

  val StorageEventTable = "NEW_EVENT"
  val OrganisationTable = "ORGANISATION"
  val BuildingTable     = "BUILDING"
  val RoomTable         = "ROOM"
  val StorageNodeTable  = "STORAGE_NODE"

}
