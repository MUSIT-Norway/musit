package services.conservation

import scala.concurrent.ExecutionContext.Implicits.global
import no.uio.musit.security.{AuthenticatedUser}
import akka.event.slf4j.Logger
import com.google.inject.Inject
import no.uio.musit.MusitResults.{MusitHttpError, MusitResult, MusitSuccess}
import no.uio.musit.models.MuseumId
import play.api.libs.ws.WSClient
import no.uio.musit.ws.ViaProxy.viaProxy
import scala.concurrent.{ExecutionContext, Future}
import play.api.Configuration
import play.api.http.Status.NOT_FOUND

class DocumentMetadataService @Inject()(
    implicit
    val ws: WSClient,
    val ec: ExecutionContext,
    val config: Configuration
) {

  def getFilename(
      mid: MuseumId,
      fileId: String,
      currUser: AuthenticatedUser
  ): Future[MusitResult[String]] = {

    val logger = Logger(classOf[DocumentMetadataService], "musit")
    val env    = config.getOptional[String]("musit.env")
    val baseUrl = env match {
      case Some("dev") => "http://localhost"
      case _           => config.get[String]("musit.baseUrl")
    }

    val endpoint =
      s"$baseUrl/api/document/museum/${mid.underlying}/files/$fileId"

    logger.debug("getFilename: endpoint: " + endpoint)

    val bearerToken = currUser.session.uuid.asBearerToken

    val req = ws.url(endpoint).viaProxy.withHttpHeaders(bearerToken.asHeader)
    logger.debug("getFilename: getting request")
    val res = req.get()
    logger.debug("getFilename: got request")
    logger.debug(s"DocumentFilename: request body: ${req.body}")

    res.map(r => {
      logger.debug("getFilename: working with request")
      val v = r.status match {
        case 200 => {
          logger.debug(s"getFilename: Status: 200")
          println(s"getFilename: Status: 200")
          val filename = (r.json \ "title").as[String]
          logger.debug(s"Read filename from document: $filename")

          MusitSuccess(filename)
        }
        case NOT_FOUND => {
          logger.debug("filename not found")
          MusitSuccess("not found")
        }
        case _ => {
          logger.debug(s"getFilename: Status: ${r.status}")
          MusitHttpError(
            status = r.status,
            message = Option(r.body).getOrElse(r.statusText)
          )
        }
      }
      v
    })

  }
}
