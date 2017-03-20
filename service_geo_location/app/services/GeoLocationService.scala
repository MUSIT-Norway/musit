/*
 * MUSIT is a museum database to archive natural and cultural history data.
 * Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package services

import com.google.inject.Inject
import models.{Address, GeoNorwayAddress}
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.{Configuration, Logger}

import scala.concurrent.Future

case class GeoLocationConfig(url: String, hitsPerResult: Int)

class GeoLocationService @Inject()(config: Configuration, ws: WSClient) {

  val logger = Logger(classOf[GeoLocationService])

  val geoLocationConfig =
    config.underlying.as[GeoLocationConfig]("musit.geoLocation.geoNorway")

  def searchGeoNorway(expr: String): Future[Seq[Address]] = {

    ws.url(geoLocationConfig.url)
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
