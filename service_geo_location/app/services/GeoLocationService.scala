package services

import com.google.inject.Inject
import models.{Address, GeoNorwayAddress}
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import no.uio.musit.ws.ViaProxy.viaProxy
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.{Configuration, Logger}

import scala.concurrent.Future

case class GeoLocationConfig(url: String, hitsPerResult: Int)

class GeoLocationService @Inject()(ws: WSClient)(implicit config: Configuration) {

  val logger = Logger(classOf[GeoLocationService])

  val geoLocationConfig =
    config.underlying.as[GeoLocationConfig]("musit.geoLocation.geoNorway")

  def searchGeoNorway(expr: String): Future[Seq[Address]] = {

    ws.url(geoLocationConfig.url)
      .viaProxy
      .withQueryString(
        "sokestreng" -> expr,
        "antPerSide" -> s"${geoLocationConfig.hitsPerResult}"
      )
      .get()
      .map { response =>
        logger.debug(s"Got response from geonorge:\n${response.body}")
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
      }
  }

}
