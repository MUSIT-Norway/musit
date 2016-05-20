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

import no.uio.musit.microservice.geoLocation.domain.GeoNorwayAddress
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.ws.WS

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait GeoLocationService {
  def searchGeoNorway(expression: String): Future[Seq[GeoNorwayAddress]] = {
    val hitsPerResult = play.api.Play.current.configuration.getInt("musit.geoLocation.geoNorway.hitsPerResult").getOrElse(10)
    val responseFuture = WS.url(s"http://ws.geonorge.no/AdresseWS/adresse/sok?sokestreng=$expression&antPerSide=$hitsPerResult").get
    responseFuture.map(response => {
      val json = Json.parse(response.body)
      val adresser = (json \ "adresser").as[List[Map[String, String]]]
      adresser.map(adresse => {
        GeoNorwayAddress(
          street = adresse.get("adressenavn").getOrElse(""),
          streetNo = adresse.get("husnr").getOrElse(""),
          place = adresse.get("poststed").getOrElse(""),
          zip = adresse.get("postnr").getOrElse("")
        )
      })
    })
  }

}

/* val future = WS.url(s"http://localhost:$port/v1/1").get()
      whenReady(future, timeout) { response =>
        val json = Json.parse(response.body)
        assert((json \ "id").get.toString() == "1")*/ 