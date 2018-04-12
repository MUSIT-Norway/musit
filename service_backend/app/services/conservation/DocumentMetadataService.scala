package services.conservation

import scala.concurrent.ExecutionContext.Implicits.global
import no.uio.musit.security.{AuthenticatedUser, BearerToken}
import akka.event.slf4j.Logger
import com.google.inject.Inject
import no.uio.musit.MusitResults.{MusitHttpError, MusitResult, MusitSuccess}
import no.uio.musit.models.MuseumId
import play.api.libs.ws.WSClient
import no.uio.musit.ws.ViaProxy.viaProxy

import scala.concurrent.{ExecutionContext, Future}
import play.api.Configuration
import play.api.http.Status.NOT_FOUND
import play.api.libs.json.JsArray

import scala.util.{Success, Try}
import scala.util.control.NonFatal

class DocumentMetadataService @Inject()(
    implicit
    val ws: WSClient,
    val ec: ExecutionContext,
    val config: Configuration
) {

  def getFilenames(
      mid: MuseumId,
      fileIds: Seq[String],
      currUser: AuthenticatedUser
  ): Future[MusitResult[Seq[String]]] = {

    val logger = Logger(classOf[DocumentMetadataService], "musit")
//    println("DocumentMetadataService.getFilenames")
    val env = config.getOptional[String]("musit.env")
    val baseUrl = env match {
      case Some("dev") => "http://localhost"
      case _           => config.get[String]("musit.baseUrl")
    }

    val endpoint =
      s"$baseUrl/api/document/museum/${mid.underlying}/collectionManagement/attachments"

    logger.debug("getFilenames: endpoint: " + endpoint)

//    println("files: " + fileIds.mkString(","))
    val bearerToken = currUser.session.uuid.asBearerToken
//    println(bearerToken.asHeader.toString())

    val req = ws
      .url(endpoint)
      .viaProxy
      .withHttpHeaders(bearerToken.asHeader)
      .withQueryStringParameters("fileIds" -> fileIds.mkString(","))
//      .withQueryStringParameters("fileIds" -> testFileIdsUtv)

    logger.debug("getFilenames: getting request")

    val res = req.get()

    logger.debug("getFilenames: got request")
//    println("getFilenames: got request")

    res.map(r => {
      logger.debug("getFilenames: working with request")
//      println(s"getFilenames: Status: ${r.status}")
      val v = r.status match {
        case 200 => {
          logger.debug(s"getFilenames: Status: 200")
//          println(s"getFilenames: Status: 200")
          val filenamesArr = (r.json).as[JsArray].value
          val filenames    = filenamesArr.map(x => (x \ "title").as[String]).toSeq

          logger.debug(s"Read filenames from document: ${filenames.mkString(",")}")
//          println(s"Read filenames from document: ${filenames.mkString(",")}")

          MusitSuccess(filenames)
        }
        case NOT_FOUND => {
          logger.debug("filename not found")
          MusitSuccess(Seq("(filename not found)"))
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

//

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
