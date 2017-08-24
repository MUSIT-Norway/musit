package services.geolocation

import com.google.inject.Inject
import models.geolocation.{Address, GeoNorwayAddress}
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import no.uio.musit.MusitResults.{MusitHttpError, MusitResult, MusitSuccess}
import no.uio.musit.ws.ViaProxy.viaProxy
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.{Configuration, Logger}
import play.api.http.Status._

import scala.concurrent.{ExecutionContext, Future}

case class GeoLocationConfig(url: String, hitsPerResult: Int)

class GeoLocationService @Inject()(
    implicit
    val ws: WSClient,
    val ec: ExecutionContext,
    val config: Configuration
) {

  val logger = Logger(classOf[GeoLocationService])

  val geoLocationConfig =
    config.underlying.as[GeoLocationConfig]("musit.geoLocation.geoNorway")

  def searchGeoNorway(expr: String): Future[MusitResult[Seq[Address]]] = {

    ws.url(geoLocationConfig.url)
      .viaProxy
      .withQueryStringParameters(
        "sokestreng" -> expr,
        "antPerSide" -> s"${geoLocationConfig.hitsPerResult}"
      )
      .get()
      .map { response =>
        response.status match {
          case OK =>
            logger.debug(s"Got response from geonorge:\n${Json.stringify(response.json)}")
            val res =
              (response.json \ "totaltAntallTreff").asOpt[String].map(_.toInt) match {
                case Some(numRes) if numRes > 0 =>
                  logger.debug(s"Got $numRes address results.")
                  val jsArr = (response.json \ "adresser").as[JsArray].value
                  jsArr.foldLeft(List.empty[Address]) { (state, ajs) =>
                    Json
                      .fromJson[GeoNorwayAddress](ajs)
                      .asOpt
                      .map { gna =>
                        state :+ GeoNorwayAddress.asAddress(gna)
                      }
                      .getOrElse(state)
                  }

                case _ =>
                  logger.debug("Search did not return any results")
                  Seq.empty
              }

            MusitSuccess(res)

          case NOT_FOUND =>
            MusitSuccess(Seq.empty)

          case _ =>
            MusitHttpError(
              status = response.status,
              message = Option(response.body).getOrElse(response.statusText)
            )
        }
      }
  }

}
