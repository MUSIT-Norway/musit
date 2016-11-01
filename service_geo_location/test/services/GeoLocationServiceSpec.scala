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

import no.uio.musit.test.MusitSpec
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.test.WsTestClient
import play.api.{Configuration, Environment}

import scala.language.postfixOps

class GeoLocationServiceSpec extends MusitSpec {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  val config = Configuration.load(Environment.simple())

  "GeoLocationService" when {

    "searching for addresses" should {

      "return a list of results that match the query" in {
        WsTestClient.withClient { client =>
          val service = new GeoLocationService(config, client)

          val res = service.searchGeoNorway("paal bergs vei 56, RYKKINN").futureValue
          res must not be empty
          res.head.street mustBe "Paal Bergs vei"
          res.head.streetNo mustBe "56"
          res.head.place mustBe "RYKKINN"
          res.head.zip mustBe "1348"
        }
      }

      "return a list of results that contains street number with house letter" in {
        WsTestClient.withClient { client =>
          val service = new GeoLocationService(config, client)

          val res = service.searchGeoNorway("Kirkegata 11, Hønefoss").futureValue

          res must not be empty
          res.head.street mustBe "Kirkegata"
          res.head.streetNo mustBe "11 E"
          res.head.place mustBe "HØNEFOSS"
          res.head.zip mustBe "3510"
        }
      }

    }
  }

}
