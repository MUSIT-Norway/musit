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
import no.uio.musit.microservice.time.domain.{Date, DateTime, Time}
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json._
import play.api.libs.ws.WS

import scala.concurrent.duration._

/**
 * add your integration spec here.
 * An integration test will fire up a whole play application in a real (or headless) browser
 */
class TimeIntegrationTest extends PlaySpec with OneServerPerSuite with ScalaFutures {
  implicit override lazy val app = new GuiceApplicationBuilder().build()
  val timeout = PatienceConfiguration.Timeout(1 seconds)

  "Time integration " must {
    "action get now (none)" in {
      val future = WS.url(s"http://localhost:$port/v1/now").get()
      whenReady(future, timeout) { response =>
        val now = Json.parse(response.body).validate[DateTime].get
        assert(now.date != null)
        assert(now.time != null)
      }
    }

    "action get now (time)" in {
      val future = WS.url(s"http://localhost:$port/v1/now?filter=[time]").get()
      whenReady(future, timeout) { response =>
        val now = Json.parse(response.body).validate[Time].get
        assert(now.time != null)
      }
    }

    "action get now (date)" in {
      val future = WS.url(s"http://localhost:$port/v1/now?filter=[date]").get()
      whenReady(future, timeout) { response =>
        val now = Json.parse(response.body).validate[Date].get
        assert(now.date != null)
      }
    }

    "action get now (datetime)" in {
      val future = WS.url(s"http://localhost:$port/v1/now?filter=[date,time]").get()
      whenReady(future, timeout) { response =>
        val now = Json.parse(response.body).validate[DateTime].get
        assert(now.date != null)
        assert(now.time != null)
      }
    }

    "action get now (svada) fails" in {
      val thrown = intercept[Exception] {
        val future = WS.url(s"http://localhost:$port/v1/now?filter=[svada]").get()
        whenReady(future, timeout) { response =>
          val now = Json.parse(response.body).validate[DateTime].get
        }
      }
      assert(thrown != null)
    }
  }
}
