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
    def toJson(ba: BulkAction) =
      Json.obj(
        "_index" -> JsString(ba.index),
        "_type"  -> JsString(ba.typ),
        "_id"    -> JsString(ba.id)
      )

    implicit val write: Writes[BulkAction] = Writes {
      case ba: IndexAction  => IndexAction.write.writes(ba)
      case ba: CreateAction => CreateAction.write.writes(ba)
      case ba: UpdateAction => UpdateAction.write.writes(ba)
      case ba: DeleteAction => DeleteAction.write.writes(ba)
    }
  }

  object IndexAction {
    implicit val write: Writes[IndexAction] = Writes[IndexAction] { obj =>
      Json.obj("index" -> BulkAction.toJson(obj))
    }
  }

  object CreateAction {
    implicit val write: Writes[CreateAction] = Writes[CreateAction] { obj =>
      Json.obj("create" -> BulkAction.toJson(obj))
    }
  }

  object UpdateAction {
    implicit val write: Writes[UpdateAction] = Writes[UpdateAction] { obj =>
      Json.obj("update" -> BulkAction.toJson(obj))
    }
  }

  object DeleteAction {
    implicit val write: Writes[DeleteAction] = Writes[DeleteAction] { obj =>
      Json.obj("delete" -> BulkAction.toJson(obj))
    }
  }

}
