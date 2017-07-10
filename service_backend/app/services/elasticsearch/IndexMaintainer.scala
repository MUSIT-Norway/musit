package services.elasticsearch

import com.google.inject.Inject
import no.uio.musit.MusitResults.{MusitHttpError, MusitSuccess}
import no.uio.musit.functional.Implicits._
import no.uio.musit.functional.MonadTransformers.MusitResultT
import play.api.Logger
import services.elasticsearch.client.ElasticsearchClient
import services.elasticsearch.client.models.AliasActions.{AddAlias, DeleteIndex}
import services.elasticsearch.client.models.Aliases

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, ExecutionContext}

class IndexMaintainer @Inject()(
    esClient: ElasticsearchClient
)(implicit ec: ExecutionContext) {

  val logger = Logger(classOf[IndexMaintainer])

  def activateIndex(index: String, aliasName: String) = {
    val res = Await.result(
      (for {
        allAliases <- MusitResultT(esClient.aliases)

        aliasesToRemove = allAliases.filter { alias =>
          alias.index.startsWith(aliasName) &&
          alias.aliases.contains(aliasName)
        }

        aliasActions = toAliasAction(index, aliasName, aliasesToRemove)

        _ <- MusitResultT(esClient.aliases(aliasActions))
      } yield aliasActions).value,
      30 seconds
    )
    res match {
      case MusitSuccess(actions) =>
        logger.info(s"Updated indices and aliases: $actions")
      case MusitHttpError(code, msg) =>
        logger.error(
          s"Unable to setup index and alias for index: $index, alias: $aliasName, msg: $msg"
        )
      case err =>
        logger.error(
          s"Unable to setup index and alias for index: $index and alias: $aliasName"
        )
    }
  }

  private def toAliasAction(
      index: String,
      aliasName: String,
      aliasesToRemove: Seq[Aliases]
  ) = {
    List(AddAlias(index, aliasName)) ++ aliasesToRemove.flatMap { alias =>
      alias.aliases.filter(_ != aliasName).map(AddAlias(index, _)) ++
        List(DeleteIndex(alias.index))
    }
  }
}
