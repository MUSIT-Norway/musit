package services.elasticsearch.client

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.google.inject.ImplementedBy
import no.uio.musit.MusitResults.MusitResult
import play.api.libs.json.JsValue
import services.elasticsearch.client.models.AliasActions.AliasAction
import services.elasticsearch.client.models.BulkActions.BulkAction
import services.elasticsearch.client.models.RefreshIndex.{NoRefresh, Refresh}
import services.elasticsearch.client.models.{
  Aliases,
  BulkResponse,
  ElasticsearchConfig,
  IndexResponse
}

import scala.concurrent.Future

@ImplementedBy(classOf[ElasticsearchHttpClient])
trait ElasticsearchClient
    extends ElasticsearchIndicesApi
    with ElasticsearchAliasApi
    with ElasticsearchSearchApi
    with ElasticsearchConfigApi

@ImplementedBy(classOf[ElasticsearchClient])
trait ElasticsearchSearchApi {

  /**
   * Execute a search in Elasticsearch.
   */
  def search(
      query: String,
      index: Option[String] = None,
      typ: Option[String] = None
  ): Future[MusitResult[JsValue]]

}

@ImplementedBy(classOf[ElasticsearchClient])
trait ElasticsearchIndicesApi {

  /**
   * Upsert a document to Elasticsearch.
   */
  def index(
      index: String,
      tpy: String,
      id: String,
      document: JsValue,
      refresh: Refresh = NoRefresh
  ): Future[MusitResult[JsValue]]

  /**
   * Status for all the indices in Elasticsearch.
   */
  def indices: Future[MusitResult[Seq[IndexResponse]]]

  /**
   * Get a document from Elasticsearch.
   */
  def get(
      index: String,
      tpy: String,
      id: String
  ): Future[MusitResult[Option[JsValue]]]

  /**
   * Delete a document in Elasticsearch.
   */
  def delete(
      index: String,
      tpy: String,
      id: String,
      refresh: Refresh = NoRefresh
  ): Future[MusitResult[JsValue]]

  /**
   * Delete an index including all the documents.
   */
  def deleteIndex(index: String): Future[MusitResult[JsValue]]

  /**
   * Send multiple actions to Elasticsearch. The source have be a finite stream since
   * the client will try to send all document in the source to ES in one request.
   */
  def bulkAction(
      source: Source[BulkAction, NotUsed],
      refresh: Refresh = NoRefresh
  ): Future[MusitResult[BulkResponse]]

}

@ImplementedBy(classOf[ElasticsearchClient])
trait ElasticsearchAliasApi {

  /**
   * Operations to add and remove aliases and remove indices.
   */
  def aliases(actions: Seq[AliasAction]): Future[MusitResult[Unit]]

  /**
   * List out the aliases related to the indices.
   */
  def aliases: Future[MusitResult[Seq[Aliases]]]

}

@ImplementedBy(classOf[ElasticsearchClient])
trait ElasticsearchConfigApi {

  def config(index: String, mapping: ElasticsearchConfig): Future[MusitResult[Unit]]

}
