package services.elasticsearch.client

/**
 * Controls the ?refresh query param.
 *
 * See documentation for details:
 * https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-refresh.html
 */
object RefreshIndex {
  sealed abstract class Refresh(val underlying: String)

  /**
   * Refresh the relevant primary and replica shards (not the whole index) immediately
   * after the operation occurs, so that the updated document appears in search results
   * immediately. This should ONLY be done after careful thought and verification that
   * it does not lead to poor performance, both from an indexing and a search standpoint.
   */
  case object Immediately extends Refresh("true")

  /**
   * Wait for the changes made by the request to be made visible by a refresh before
   * replying. This doesn’t force an immediate refresh, rather, it waits for a refresh
   * to happen. Elasticsearch automatically refreshes shards that have changed every
   * index.refresh_interval which defaults to one second. That setting is dynamic.
   * Calling the Refresh API or setting refresh to true on any of the APIs that support
   * it will also cause a refresh, in turn causing already running requests with
   * refresh=wait_for to return.
   */
  case object WaitFor extends Refresh("wait_for")

  /**
   * Take no refresh related actions. The changes made by this request will be made
   * visible at some point after the request returns.
   *
   * Default value
   */
  case object NoRefresh extends Refresh("false")

}
