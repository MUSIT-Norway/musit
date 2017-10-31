package controllers

import com.google.inject.Inject
import no.uio.musit.security.Authenticator
import no.uio.musit.service.MusitController
import play.api.Configuration
import play.api.http.Status
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc._

import scala.concurrent.Future

case class ResultValue(
    playServerHttpAddress: String,
    httpAddress: String,
    elasticSearchHost: String
)

object ResultValue {

  implicit val format = Json.format[ResultValue]
}

class SystemInfoController @Inject()(
    val controllerComponents: ControllerComponents,
    val conf: Configuration,
    val authService: Authenticator,
    ws: WSClient
) extends MusitController {

  def getEsUrl() = {
    val host = conf.underlying.getString("musit.elasticsearch.host")
    val port = conf.underlying.getInt("musit.elasticsearch.port")
    s"http://$host:$port"
  }

  def doEsCall = Action.async { implicit request =>
    val esHost = getEsUrl()

    val res = ws.url(s"$esHost/_stats").get().map(_.body)
    res.map(Ok(_))
  }

  def getMainInfo = Action.async { implicit request =>
    val playServerHttpAddress = conf
      .get[Option[String]]("play.server.http.address")
      .getOrElse("Finner ikke play.server.http.address")
    val httpAddress = conf.get[String]("http.address")
    val esHost      = getEsUrl()
    //.get[Option[String]]("musit.elasticsearch.host")
    //.getOrElse("finner ikke musit.elasticsearch.host")

    val res = ResultValue(
      playServerHttpAddress = playServerHttpAddress,
      httpAddress = httpAddress,
      elasticSearchHost = esHost
    )
    Future.successful(Ok(Json.toJson(res)))
  }
}
