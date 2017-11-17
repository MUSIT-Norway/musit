package models.elasticsearch

trait Searchable {

  /**
   * The document id used to store the document
   */
  def docId: String

  /**
   * Optional parent id. Note, the id must be defined in the mapping for and the parent
   * object need to stored in the same index but in another type.
   */
  def docParentId: Option[String]

}
