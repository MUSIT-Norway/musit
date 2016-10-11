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

package no.uio.musit.microservice.geoLocation.service

import com.google.inject.Inject
import no.uio.musit.microservice.geoLocation.domain.GeoNorwayAddress
import play.api.Configuration
import play.api.libs.json._
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GeoLocationService @Inject() (config: Configuration, ws: WSClient) {

  def searchGeoNorway(expression: String): Future[Seq[GeoNorwayAddress]] = {
    val hitsPerResult = config.getInt("musit.geoLocation.geoNorway.hitsPerResult").getOrElse(10)
    val searchUrl = s"http://ws.geonorge.no/AdresseWS/adresse/sok?sokestreng=$expression&antPerSide=$hitsPerResult"

    ws.url(searchUrl).get.map { response =>
      val json = Json.parse(response.body)
      val addresses = (json \ "adresser").as[List[Map[String, String]]]
      addresses.map(address => {
        GeoNorwayAddress(
          street = address.getOrElse("adressenavn", ""),
          streetNo = address.getOrElse("husnr", ""),
          place = address.getOrElse("poststed", ""),
          zip = address.getOrElse("postnr", "")
        )
      })
    }
  }

}
