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

import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import play.api.test.WsTestClient
import play.api.{Configuration, Environment}

import scala.concurrent.duration._
import scala.language.postfixOps

class GeoLocationServiceSpec extends PlaySpec with ScalaFutures {

  val timeout = Timeout(10 seconds)

  val config = Configuration.load(Environment.simple())

  "GeoLocationService rest integration" must {

    "searchAddress" in {
      WsTestClient.withClient { client =>
        val service = new GeoLocationService(config, client)

        val response = service.searchGeoNorway("paal bergs vei 56, RYKKINN")
        whenReady(response, timeout) { geoAddresses =>
          geoAddresses must not be empty
        }
      }
    }

  }

}
