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

import no.uio.musit.MusitResults.MusitHttpError
import no.uio.musit.test.MusitSpecWithAppPerSuite
import no.uio.musit.test.matchers.MusitResultValues
import org.scalatest.Inside
import play.api.Configuration
import play.api.http.Status
import play.api.mvc.{Action, Results}
import play.api.routing.sird._
import play.api.test.WsTestClient
import play.core.server.Server

class GeoLocationServiceSpec
    extends MusitSpecWithAppPerSuite
    with MusitResultValues
    with Inside {

  val service: GeoLocationService = fromInstanceCache[GeoLocationService]

  "GeoLocationService" when {

    "searching for addresses" should {

      "return a list of results that match the query" in {
        val res = service.searchGeoNorway("paal bergs vei 56, RYKKINN").futureValue
        res.isSuccess mustBe true
        res.successValue.toList match {
          case theHead :: tail =>
            theHead.street mustBe "Paal Bergs vei"
            theHead.streetNo mustBe "56"
            theHead.place mustBe "RYKKINN"
            theHead.zip mustBe "1348"

          case _ =>
            fail(s"The list contained something wrong.")
        }

      }

      "return a list of results that contains street number with house letter" in {
        val res = service.searchGeoNorway("Kirkegata 11, Hønefoss").futureValue

        res.isSuccess mustBe true
        res.successValue.toList.find { a =>
          a.street == "Kirkegata" &&
          a.streetNo == "11 E" &&
          a.place == "HØNEFOSS" &&
          a.zip == "3510"
        } must not be None

      }

      "return 503 if service not available" in {
        implicit val conf = Configuration.from(
          Map(
            "musit.geoLocation.geoNorway.url"           -> "/geo",
            "musit.geoLocation.geoNorway.hitsPerResult" -> "4"
          )
        )
        val expMsg = "error message from GeoNorway"
        Server.withRouter() {
          case GET(p"/geo") =>
            Action {
              Results.ServiceUnavailable(expMsg)
            }
        } { implicit port =>
          WsTestClient.withClient { client =>
            val address = new GeoLocationService(client).searchGeoNorway("").futureValue
            address.isFailure mustBe true
            inside(address) {
              case MusitHttpError(status, msg) =>
                status mustBe Status.SERVICE_UNAVAILABLE
                msg mustBe expMsg
            }
          }
        }
      }
    }
  }
}
