package services.elasticsearch.elastic4s

import com.sksamuel.elastic4s.http.search.{SearchBodyBuilderFn, SearchResponse}
import com.sksamuel.elastic4s.http.{HttpExecutable, IndicesOptionsParams, ResponseHandler}
import com.sksamuel.elastic4s.searches.SearchDefinition
import org.apache.http.entity.{ContentType, StringEntity}
import org.elasticsearch.client.RestClient
import services.elasticsearch.elastic4s.MusitResponseHandler.default

import scala.concurrent.Future

/**
 * This class should be the same as SearchHttpExecutable for elastic4s. The only
 * modification is that we can use a custom response handler. When upgrading to
 * version 6 of elastic4s this should not be needed since we can then configure the
 * response handler in the client.
 *
 * TODO: Remove this when upgrading to elastic4s version 6.x.x
 */
class MusitSearchHttpExecutable[U](
    val handler: ResponseHandler[U] = ResponseHandler.default
) extends HttpExecutable[SearchDefinition, U] {

  override def execute(client: RestClient, request: SearchDefinition): Future[U] = {

    val endpoint =
      if (request.indexesTypes.indexes.isEmpty && request.indexesTypes.types.isEmpty)
        "/_search"
      else if (request.indexesTypes.indexes.isEmpty)
        "/_all/" + request.indexesTypes.types.mkString(",") + "/_search"
      else if (request.indexesTypes.types.isEmpty)
        "/" + request.indexesTypes.indexes.mkString(",") + "/_search"
      else
        "/" + request.indexesTypes.indexes
          .mkString(",") + "/" + request.indexesTypes.types.mkString(",") + "/_search"

    val params = scala.collection.mutable.Map.empty[String, String]
    request.keepAlive.foreach(params.put("scroll", _))
    request.pref.foreach(params.put("preference", _))
    request.requestCache.map(_.toString).foreach(params.put("request_cache", _))
    request.routing.foreach(params.put("routing", _))
    request.searchType.map(_.toString).foreach(params.put("search_type", _))
    request.terminateAfter.map(_.toString).foreach(params.put("terminate_after", _))
    request.timeout.map(_.toMillis + "ms").foreach(params.put("timeout", _))
    request.version.map(_.toString).foreach(params.put("version", _))
    request.indicesOptions.foreach { opts =>
      IndicesOptionsParams(opts).foreach { case (key, value) => params.put(key, value) }
    }

    val builder = SearchBodyBuilderFn(request)
    logger.debug("Executing search request: " + builder.string)

    val body   = builder.string()
    val entity = new StringEntity(body, ContentType.APPLICATION_JSON)

    client.async("POST", endpoint, params.toMap, entity, handler)
  }
}

object MusitSearchHttpExecutable {
  implicit val musitSearchHttpExecutable
    : MusitSearchHttpExecutable[MusitESResponse[SearchResponse]] =
    new MusitSearchHttpExecutable[MusitESResponse[SearchResponse]](
      default[SearchResponse]
    )
}
