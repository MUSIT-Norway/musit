package services.elasticsearch.client

import akka.NotUsed
import akka.stream.scaladsl.Source
import no.uio.musit.MusitResults
import play.api.libs.json.JsValue
import services.elasticsearch.client.models.BulkActions.BulkAction
import services.elasticsearch.client.models.RefreshIndex.{NoRefresh, Refresh}
import services.elasticsearch.client.models.{BulkResponse, IndexResponse}

import scala.concurrent.Future

trait ElasticsearchClient {

  /**
   * Upsert a document to Elasticsearch.
   */
  def index(
      index: String,
      tpy: String,
      id: String,
      document: JsValue,
      refresh: Refresh = NoRefresh
  ): Future[MusitResults.MusitResult[JsValue]]

  /**
   * Status for all the indices in Elasticsearch.
   */
  def indices: Future[MusitResults.MusitResult[Seq[IndexResponse]]]

  /**
   * Get a document from Elasticsearch.
   */
  def get(
      index: String,
      tpy: String,
      id: String
  ): Future[MusitResults.MusitResult[Option[JsValue]]]

  /**
   * Delete a document in Elasticsearch.
   */
  def delete(
      index: String,
      tpy: String,
      id: String,
      refresh: Refresh = NoRefresh
  ): Future[MusitResults.MusitResult[JsValue]]

  /**
   * Delete an index including all the documents.
   */
  def deleteIndex(index: String): Future[MusitResults.MusitResult[JsValue]]

  /**
   * Execute a search in Elasticsearch.
   */
  def search(
      query: String,
      index: Option[String] = None,
      typ: Option[String] = None
  ): Future[MusitResults.MusitResult[JsValue]]

  /**
   * Send multiple actions to Elasticsearch. The source have be a finite stream since
   * the client will try to send all document in the source to ES in one request.
   */
  def bulkAction(
      source: Source[BulkAction, NotUsed],
      refresh: Refresh = NoRefresh
  ): Future[MusitResults.MusitResult[BulkResponse]]

}
