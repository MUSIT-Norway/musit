package models.elasticsearch

object DocumentIndexerStatuses {

  /**
   * Statuses for the index. The ready flag indicate that we can
   * call reindex/updateIndex
   */
  sealed abstract class DocumentIndexerStatus(val ready: Boolean)
  object NotExecuted  extends DocumentIndexerStatus(true)
  object Executing    extends DocumentIndexerStatus(false)
  object IndexSuccess extends DocumentIndexerStatus(true)
  object IndexFailed  extends DocumentIndexerStatus(false)
}
