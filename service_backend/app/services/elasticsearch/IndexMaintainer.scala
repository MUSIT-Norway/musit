package services.elasticsearch

import com.google.inject.Inject
import com.sksamuel.elastic4s.http.ElasticDsl.{catAliases, _}
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.cat.CatAlias
import com.sksamuel.elastic4s.http.index.admin.DeleteIndexResponse
import play.api.Logger
import services.elasticsearch.IndexMaintainer._

import scala.concurrent.{ExecutionContext, Future}

class IndexMaintainer @Inject()(
    client: HttpClient
) {

  val logger = Logger(classOf[IndexMaintainer])

  def activateIndex(index: String, aliasName: String)(
      implicit ec: ExecutionContext
  ): Future[Unit] = {
    val res = for {
      allAliases <- client.execute(catAliases())

      move   = findAliasesToMove(allAliases, aliasName)
      delete = findIndicesToDelete(allAliases, aliasName)

      aliasResponse <- client.execute(
                        aliases(
                          (addAlias(aliasName) on index)
                            +: move.map(a => addAlias(a) on index).toList
                        )
                      )

      indexResponse <- if (delete.nonEmpty)
                        client.execute(deleteIndex(delete))
                      else
                        Future.successful(DeleteIndexResponse(true))

    } yield (aliasResponse, indexResponse, move, delete)

    res.map {
      case (aliasResponse, indexResponse, movedAliases, deletedIndices) =>
        if (aliasResponse.acknowledged && indexResponse.acknowledged) {
          logger.info(
            s"Updated alias '$aliasName' and related indices. Moved aliases $movedAliases, deleted indices: $deletedIndices"
          )
        } else {
          logger.warn(
            s"Failed to update indices and aliases for index: $index with" +
              s" alias: $aliasName." +
              s"Alias response $aliasResponse, $movedAliases. " +
              s"Indices response $indexResponse, $deletedIndices."
          )
        }
    }
  }

  def indexNameForAlias(
      alias: String
  )(implicit ec: ExecutionContext): Future[Option[String]] =
    client.execute(catAliases()).map {
      _.find(ca => ca.alias == alias && ca.index.startsWith(alias)).map(_.index)
    }

}

object IndexMaintainer {

  def findIndicesToDelete(alias: Seq[CatAlias], aliasName: String): Set[String] = {
    alias
      .filter(_.index.startsWith(aliasName))
      .filter(_.alias == aliasName)
      .map(_.index)
      .toSet
  }

  def findAliasesToMove(alias: Seq[CatAlias], aliasName: String): Set[String] =
    alias
      .filterNot(_.alias == aliasName)
      .filter(_.index.startsWith(aliasName))
      .map(_.alias)
      .toSet

}
