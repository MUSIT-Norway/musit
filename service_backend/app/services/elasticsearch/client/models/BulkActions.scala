package services.elasticsearch.client.models

import play.api.libs.json._

/**
 * Bulk actions are actions that can be bundled up and sent to elasticsearch.
 */
object BulkActions {

  sealed trait BulkAction {
    def source: Option[JsValue]
    def index: String
    def typ: String
    def id: String
  }

  /**
   * Inserts a document into elastic search. Will override the current document if it
   * exists.
   */
  case class IndexAction(index: String, typ: String, id: String, document: JsValue)
      extends BulkAction {
    val source = Some(document)
  }

  /**
   * Inserts a document into elastic search. Will fail if a document with the same
   * index, type and id exists.
   */
  case class CreateAction(index: String, typ: String, id: String, document: JsValue)
      extends BulkAction {
    val source = Some(document)
  }

  /**
   * Update an existing document.
   * Since the document is optional we need to wrap it into a doc field.
   */
  case class UpdateAction(
      index: String,
      typ: String,
      id: String,
      document: Option[JsValue]
  ) extends BulkAction {
    override def source = document.map(d => Json.obj("doc" -> d))
  }

  /**
   * Delete an existing document.
   */
  case class DeleteAction(index: String, typ: String, id: String) extends BulkAction {
    val source = None
  }

  object BulkAction {

    private[this] def toActionName[A >: BulkAction](ba: A) = ba match {
      case _: IndexAction  => "index"
      case _: CreateAction => "create"
      case _: UpdateAction => "update"
      case _: DeleteAction => "delete"
    }

    implicit val writes: Writes[BulkAction] = Writes { action =>
      Json.obj(
        toActionName(action) -> Json.obj(
          "_index" -> JsString(action.index),
          "_type"  -> JsString(action.typ),
          "_id"    -> JsString(action.id)
        )
      )
    }
  }

}
