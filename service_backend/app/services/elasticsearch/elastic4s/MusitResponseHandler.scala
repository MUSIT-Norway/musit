package services.elasticsearch.elastic4s

import java.nio.charset.Charset

import com.sksamuel.elastic4s.http.{JacksonSupport, ResponseHandler}
import org.apache.http.HttpEntity
import org.elasticsearch.client.{Response, ResponseException}
import play.api.libs.json.{JsValue, Json}

import scala.io.{Codec, Source}
import scala.util.{Failure, Try}

/**
 * This is a custom response handler for musit. It enables us to preserve the origin
 * response from elasticsearch parsed to JsValue.
 */
object MusitResponseHandler {
  def parse[U: Manifest](entity: HttpEntity): MusitESResponse[U] = {
    val charset =
      Option(entity.getContentEncoding).map(_.getValue).getOrElse("UTF-8")
    implicit val codec: Codec = Codec(Charset.forName(charset))

    val body = Source.fromInputStream(entity.getContent).mkString
    MusitESResponse(JacksonSupport.mapper.readValue[U](body), Json.parse(body))
  }

  def default[U: Manifest] = new MusitResponseHandler[U]

  def failure404[U: Manifest] = new Musit404ResponseHandler[U]
}

class MusitResponseHandler[U: Manifest] extends ResponseHandler[MusitESResponse[U]] {

  override def onResponse(response: Response): Try[MusitESResponse[U]] =
    Try(MusitResponseHandler.parse[U](response.getEntity))

}

class Musit404ResponseHandler[U: Manifest] extends MusitResponseHandler[U] {
  override def onError(e: Exception): Try[MusitESResponse[U]] = e match {
    case re: ResponseException if re.getResponse.getStatusLine.getStatusCode == 404 =>
      Try(MusitResponseHandler.parse[U](re.getResponse.getEntity))
    case _ => Failure(e)
  }

}

/**
 * The parsed response from elastic4s and the raw response pared with play-json.
 */
case class MusitESResponse[R](response: R, raw: JsValue)
