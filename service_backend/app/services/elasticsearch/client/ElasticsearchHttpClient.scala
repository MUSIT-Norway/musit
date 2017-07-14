package services.elasticsearch.client

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import akka.util.ByteString
import com.google.inject.Inject
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import no.uio.musit.MusitResults.{MusitHttpError, MusitResult, MusitSuccess}
import play.api.http.{ContentTypes, HeaderNames, Status}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.{StreamedBody, WSClient}
import play.api.{Configuration, Logger}
import services.elasticsearch.client.models.BulkActions.BulkAction
import services.elasticsearch.client.models.RefreshIndex.{NoRefresh, Refresh}
import services.elasticsearch.client.models._

import scala.concurrent.Future

case class ElasticsearchClientConfig(url: String)

class ElasticsearchHttpClient @Inject()(ws: WSClient)(implicit config: Configuration)
    extends ElasticsearchClient {

  val clientConfig =
    config.underlying.as[ElasticsearchClientConfig]("musit.elasticsearch")

  val logger = Logger(classOf[ElasticsearchHttpClient])

  def baseClient(parts: String*) = {
    val path = s"${clientConfig.url}/${parts.mkString("/")}"
    logger.debug(s"creating client with path $path")
    ws.url(path).withHeaders(HeaderNames.ACCEPT -> ContentTypes.JSON)
  }

  def jsonClient(parts: String*) = {
    baseClient(parts: _*).withHeaders(HeaderNames.CONTENT_TYPE -> ContentTypes.JSON)
  }

  override def index(
      index: String,
      tpy: String,
      id: String,
      document: JsValue,
      refresh: Refresh = NoRefresh
  ): Future[MusitResult[JsValue]] =
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

  override def indices: Future[MusitResult[Seq[IndexResponse]]] =
    jsonClient("_cat", "indices").get().map { response =>
      response.status match {
        case Status.OK => MusitSuccess(response.json.as[Seq[IndexResponse]])
        case httpCode  => MusitHttpError(httpCode, response.body)
      }
    }

  override def get(
      index: String,
      tpy: String,
      id: String
  ): Future[MusitResult[Option[JsValue]]] =
    jsonClient(index, tpy, id).get().map { response =>
      response.status match {
        case Status.OK        => MusitSuccess(Some(response.json))
        case Status.NOT_FOUND => MusitSuccess(None)
        case httpCode         => MusitHttpError(httpCode, response.body)
      }
    }

  override def delete(
      index: String,
      tpy: String,
      id: String,
      refresh: Refresh = NoRefresh
  ): Future[MusitResult[JsValue]] =
    jsonClient(index, tpy, id)
      .withQueryString("refresh" -> refresh.underlying)
      .delete()
      .map { response =>
        response.status match {
          case Status.OK => MusitSuccess(response.json)
          case httpCode  => MusitHttpError(httpCode, response.body)
        }
      }

  override def deleteIndex(index: String): Future[MusitResult[JsValue]] =
    jsonClient(index).delete().map { response =>
      response.status match {
        case Status.OK => MusitSuccess(response.json)
        case httpCode  => MusitHttpError(httpCode, response.body)
      }
    }

  override def search(
      query: String,
      index: Option[String] = None,
      typ: Option[String] = None
  ): Future[MusitResult[JsValue]] = {
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

  override def bulkAction(
      source: Source[BulkAction, NotUsed],
      refresh: Refresh = NoRefresh
  ): Future[MusitResult[BulkResponse]] = {
    def toByteString(jsValue: JsValue): ByteString =
      ByteString(Json.stringify(jsValue) + "\n")

    val actionFlow = Flow[BulkAction].map { action =>
      action.source match {
        case Some(document) =>
          toByteString(Json.toJson(action)) ++ toByteString(document)
        case None => toByteString(Json.toJson(action))
      }
    }

    baseClient("_bulk")
      .withHeaders(HeaderNames.CONTENT_TYPE -> "application/x-ndjson")
      .withQueryString("refresh" -> refresh.underlying)
      .withBody(StreamedBody(source.via(actionFlow)))
      .execute("POST")
      .map { response =>
        response.status match {
          case Status.OK => MusitSuccess(response.json.as[BulkResponse])
          case httpCode  => MusitHttpError(httpCode, response.body)
        }
      }
  }

  override def aliases(actions: Seq[AliasActions.AliasAction]) = {
    logger.info(s"Aliases actions $actions")
    jsonClient("_aliases").post(Json.obj("actions" -> Json.toJson(actions))).map {
      response =>
        response.status match {
          case Status.OK => MusitSuccess(())
          case httpCode  => MusitHttpError(httpCode, response.body)
        }
    }
  }

  def aliases: Future[MusitResult[Seq[Aliases]]] = {
    jsonClient("_all", "_alias").get().map { response =>
      response.status match {
        case Status.OK =>
          MusitSuccess(response.json match {
            case ob: JsObject =>
              ob.keys.toSeq.map { index =>
                val aliases: Seq[String] =
                  (ob \ index \ "aliases").getOrElse(JsObject(Seq())) match {
                    case a: JsObject => a.keys.toSeq
                    case _           => Seq()
                  }
                Aliases(index, aliases)
              }
            case _ => Seq()
          })
        case httpCode => MusitHttpError(httpCode, response.body)
      }
    }
  }

  override def config(index: String, mappings: ElasticsearchConfig) = {
    jsonClient(index).put(Json.toJson(mappings)).map { response =>
      response.status match {
        case Status.OK =>
          println(response.body)
          MusitSuccess(())
        case httpCode => MusitHttpError(httpCode, response.body)
      }
    }
  }
}
