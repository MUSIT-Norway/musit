package models.document

object ArchiveStatuses {

  sealed abstract class ArchiveStatus(val code: String)

  case object Open extends ArchiveStatus("open")

  case object Closed extends ArchiveStatus("closed")

}
