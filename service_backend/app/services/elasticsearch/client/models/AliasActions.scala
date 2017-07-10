package services.elasticsearch.client.models

import play.api.libs.json.{Json, Writes}

object AliasActions {

  sealed trait AliasAction {
    def index: String
  }

  /**
   * Add an alias to an index
   */
  final case class AddAlias(index: String, alias: String) extends AliasAction

  /**
   * Remove an alias to an index
   */
  final case class RemoveAlias(index: String, alias: String) extends AliasAction

  /**
   * Delete an index
   */
  final case class DeleteIndex(index: String) extends AliasAction

  object AliasAction {
    implicit val write = Writes[AliasAction] {
      case a: AddAlias    => AddAlias.write.writes(a)
      case r: RemoveAlias => RemoveAlias.write.writes(r)
      case d: DeleteIndex => DeleteIndex.write.writes(d)
    }
  }

  object AddAlias {
    implicit val write = Writes[AddAlias] { a =>
      Json.obj(
        "add" -> Json.obj(
          "index" -> a.index,
          "alias" -> a.alias
        )
      )
    }
  }

  object RemoveAlias {
    implicit val write = Writes[RemoveAlias] { r =>
      Json.obj(
        "remove" -> Json.obj(
          "index" -> r.index,
          "alias" -> r.alias
        )
      )
    }
  }

  object DeleteIndex {
    implicit val write = Writes[DeleteIndex] { i =>
      Json.obj(
        "remove_index" -> Json.obj(
          "index" -> i.index
        )
      )
    }
  }
}
