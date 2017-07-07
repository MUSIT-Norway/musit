package services.elasticsearch.client

import com.google.inject.Inject
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import no.uio.musit.MusitResults
import no.uio.musit.MusitResults.{MusitHttpError, MusitSuccess}
import play.api.http.{ContentTypes, HeaderNames, Status}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient
import play.api.{Configuration, Logger}
import services.elasticsearch.client.RefreshIndex.{NoRefresh, Refresh}
import services.elasticsearch.client.models.BulkActions.BulkAction
import services.elasticsearch.client.models.{BulkResponse, IndexResponse}

import scala.concurrent.Future

case class ElasticSearchClientConfig(url: String)

class ElasticSearchClient @Inject()(ws: WSClient)(implicit config: Configuration) {
  val clientConfig =
    config.underlying.as[ElasticSearchClientConfig]("musit.elasticsearch")

  val logger = Logger(classOf[ElasticSearchClient])

  def baseClient(parts: String*) = {
    val path = s"${clientConfig.url}/${parts.mkString("/")}"
    logger.debug(s"calling path $path parts: $parts")
    ws.url(path).withHeaders(HeaderNames.ACCEPT -> ContentTypes.JSON)
  }

  def jsonClient(parts: String*) = {
    baseClient(parts: _*).withHeaders(HeaderNames.CONTENT_TYPE -> ContentTypes.JSON)
  }

  def index(
      index: String,
      tpy: String,
      id: String,
      document: JsValue,
      refresh: Refresh = NoRefresh
  ) =
    jsonClient(index, tpy, id)
      .withQueryString("refresh" -> refresh.underlying)
      .put(document)
      .map { response =>
        response.status match {
          case Status.OK      => MusitSuccess(response.json)
          case Status.CREATED => MusitSuccess(response.json)
          case httpCode       => MusitHttpError(httpCode, response.body)
        }
      }

  def indices: Future[MusitResults.MusitResult[Seq[IndexResponse]]] =
    jsonClient("_cat", "indices").get().map { response =>
      response.status match {
        case Status.OK => MusitSuccess(response.json.as[Seq[IndexResponse]])
        case httpCode  => MusitHttpError(httpCode, response.body)
      }
    }

  def get(index: String, tpy: String, id: String) =
    jsonClient(index, tpy, id).get().map { response =>
      response.status match {
        case Status.OK        => MusitSuccess(Some(response.json))
        case Status.NOT_FOUND => MusitSuccess(None)
        case httpCode         => MusitHttpError(httpCode, response.body)
      }
    }

  def delete(index: String, tpy: String, id: String, refresh: Refresh = NoRefresh) =
    jsonClient(index, tpy, id)
      .withQueryString("refresh" -> refresh.underlying)
      .delete()
      .map { response =>
        response.status match {
          case Status.OK => MusitSuccess(response.json)
          case httpCode  => MusitHttpError(httpCode, response.body)
        }
      }

  def deleteIndex(index: String) =
    jsonClient(index).delete().map { response =>
      response.status match {
        case Status.OK => MusitSuccess(response.json)
        case httpCode  => MusitHttpError(httpCode, response.body)
      }
    }

  def search(query: String, index: Option[String] = None, typ: Option[String] = None) = {
    val parts = index.map {
      List(_) ++ typ.map(List(_)).getOrElse(Nil)
    }.getOrElse(Nil) :+ "_search"
    jsonClient(parts: _*).withQueryString("q" -> query).get().map { response =>
      response.status match {
        case Status.OK => MusitSuccess(response.json)
        case httpCode  => MusitHttpError(httpCode, response.body)
      }
    }
  }

  def bulkAction(actions: Seq[BulkAction], refresh: Refresh = NoRefresh) = {
    val content = actions.flatMap { action =>
      action.source match {
        case Some(source) => Json.toJson(action) :: source :: Nil
        case None         => Json.toJson(action) :: Nil
      }
    }.map(Json.stringify).mkString("", "\n", "\n")

    baseClient("_bulk")
      .withHeaders(HeaderNames.CONTENT_TYPE -> "application/x-ndjson")
      .withQueryString("refresh" -> refresh.underlying)
      .post(content)
      .map { response =>
        response.status match {
          case Status.OK => MusitSuccess(response.json.as[BulkResponse])
          case httpCode  => MusitHttpError(httpCode, response.body)
        }
      }
  }

}
