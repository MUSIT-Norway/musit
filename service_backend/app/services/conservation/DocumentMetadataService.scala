package services.conservation

import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import no.uio.musit.security.{AuthenticatedUser, BearerToken}
import akka.event.slf4j.Logger
import com.google.inject.Inject
import no.uio.musit.MusitResults.{MusitHttpError, MusitResult, MusitSuccess}
import no.uio.musit.functional.FutureMusitResult
import no.uio.musit.models.MuseumId
import play.api.libs.ws.WSClient
import no.uio.musit.ws.ViaProxy.viaProxy
import play.api.libs.json.{JsArray, JsValue, Json}

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.ws.ahc.AhcWSClient
import play.api.Configuration
import play.api.http.Status.NOT_FOUND

//class Client @Inject()(ws: WSClient, configuration: Configuration) {
//
//  def getResponse() = {
//    val baseUrl = configuration.getString("key.to.baseUrl")
//    // do something using ws object and baseUrl
//  }
//}

class DocumentMetadataService @Inject()(
    implicit
    val ws: WSClient,
    val ec: ExecutionContext,
    val config: Configuration
) {

  def getFilename3(
      mid: MuseumId,
      fileId: String,
      currUser: AuthenticatedUser
  ): Future[MusitResult[String]] = {
    println("Reached DocumentMetadataService...")
    Future(MusitSuccess("Reached DocumentMetadataService"))

  }

  def getFilename(
      mid: MuseumId,
      fileId: String,
      currUser: AuthenticatedUser
  ): Future[MusitResult[String]] = {

    val logger = Logger(classOf[DocumentMetadataService], "musit")
    val env    = config.getOptional[String]("musit.env")
    println("env: " + env.getOrElse("ingen env"))
    val baseUrl = env match {
      case Some("dev") => "http://localhost"
//      case Some("dev") => "http://musit-test:8888"
      case _ => config.get[String]("musit.baseUrl")
    }
//    val baseUrl = ""
//    val baseUrl  = config.get[String]("musit.baseUrl")

    val endpoint =
      s"$baseUrl/api/document/museum/${mid.underlying}/files/$fileId"
//      s"http://musit-test:8888/api/document/museum/${mid.underlying}/files/$fileId"
//    s"http://musit-test:8888/api/document/museum/${mid.underlying}/files/$fileId"
//      s"https://musit-utv.uio.no/api/document/museum/${mid.underlying}/files/096b554a-a3e6-439c-b46d-638021cb9aee"
//      s"https://musit-utv.uio.no/api/document/museum/4/files/096b554a-a3e6-439c-b46d-638021cb9aee"

    logger.debug("getFilename: endpoint: " + endpoint)
    println("getFilename: endpoint: " + endpoint)
    //musit.http.filters

//    val token = currUser.session.oauthToken match {
//      case Some(t) => t.asHeader
//      case None =>
//    }
    println("sessionuuid: " + currUser.session.uuid.asBearerToken)
//    val token       = BearerToken("9632dc57-3157-40c2-bd84-a86918b68a22") //currUser.session.oauthToken.get
    val bearerToken = currUser.session.uuid.asBearerToken
    //val token = currUser.session.oauthToken.get
    println("getFilename: token: " + bearerToken.asHeader.toString())
    println(
      "play.filters.https.RedirectHttpsConfiguration: " + play.filters.https.RedirectHttpsConfiguration
        .toString()
    )

    val req = ws.url(endpoint).viaProxy.withHttpHeaders(bearerToken.asHeader)
//      .withRequestTimeout(Duration.Inf)
    logger.debug("getFilename: getting request")
    println("headers: " + req.headers)
    val res = req.get()
    logger.debug("getFilename: got request")
    logger.debug(s"DocumentFilename: request body: ${req.body}")

    res.map(r => {
      println("getFilename: working with request")
      logger.debug("getFilename: working with request")

      //      logger.trace(s"DocumentFilename: body: ${r.body}")
      logger.debug(s"DocumentFilename: body: ${r.headers}")
      logger.debug(s"DocumentFilename: body: ${r.body}")

      val v = r.status match {
        case 200 => {
          logger.debug(s"getFilename: Status: 200")
          println(s"getFilename: Status: 200")
          val filename = (r.json \ "title").as[String]
          println(filename)

//          logger.trace(s"getObjNameByUUID: objUuid: $jsonUuid")

          MusitSuccess(filename)
        }
        case NOT_FOUND => {
          logger.debug("not found")
          println("not found")
          MusitSuccess("not found")
        }
        case _ => {
          logger.debug(s"getFilename: Status: ${r.status}")
          println(s"getFilename: Status: ${r.status}")
          MusitHttpError(
            status = r.status,
            message = Option(r.body).getOrElse(r.statusText)
          )
        }
      }
//      logger.trace("getObjNameByUUID: done")
      v
    })

  }
}
