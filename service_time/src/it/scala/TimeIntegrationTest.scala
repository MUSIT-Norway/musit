/*
 *  MUSIT is a museum database to archive natural and cultural history data.
 *  Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License,
 *  or any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
import domain.{MusitError, MusitTime}
import no.uio.musit.microservices.common.PlayTestDefaults
import org.scalatest.concurrent.ScalaFutures
import play.api.test.Helpers._
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.libs.ws.WS

/**
 * add your integration spec here.
 * An integration test will fire up a whole play application in a real (or headless) browser
 */
class TimeIntegrationTest extends PlaySpec with OneServerPerSuite with ScalaFutures {

  val timeout = PlayTestDefaults.timeout

  "Time integration " must {
    "action get now (none)" in {
      val response = await(WS.url(s"http://localhost:$port/v1/now").get())
      response.status mustBe OK
      val now = Json.parse(response.body).validate[MusitTime].get
      now.time must not be None
      now.date must not be None
    }

    "action get now (time)" in {
      val response = await(WS.url(s"http://localhost:$port/v1/now?filter=[time]").get())
      response.status mustEqual 200
      val now = Json.parse(response.body).validate[MusitTime].get
      now.time must not be None
      now.date mustEqual None
    }

    "action get now (date)" in {
      val response = await(WS.url(s"http://localhost:$port/v1/now?filter=[date]").get())
      response.status mustEqual 200
      val now = Json.parse(response.body).validate[MusitTime].get
      now.date must not be None
      now.time mustEqual None
    }

    "action get now (datetime)" in {
      val response = await(WS.url(s"http://localhost:$port/v1/now?filter=[date,time]").get())
      response.status mustEqual 200
      val now = Json.parse(response.body).validate[MusitTime].get
      now.date must not be None
      now.time must not be None
    }

    "action get now (svada) fails" in {
      val response = await(WS.url(s"http://localhost:$port/v1/now?filter=[svada]").get())
      response.status mustEqual 400
      val err = Json.parse(response.body).validate[MusitError].get
      err.message mustEqual "Only supports empty filter or filter on time, date or time and date"
    }
  }
}
