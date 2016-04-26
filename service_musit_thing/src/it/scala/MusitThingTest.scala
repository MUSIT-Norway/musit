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
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.{FunSuite, Matchers}
import org.scalatestplus.play.{OneAppPerSuite, OneServerPerSuite, PlaySpec}
import play.api.libs.ws.WS
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json._

import scala.concurrent.duration._

/**
 * add your integration spec here.
 * An integration test will fire up a whole play application in a real (or headless) browser
 */
class MusitThingTest extends PlaySpec with OneServerPerSuite with ScalaFutures {
  implicit override lazy val app = new GuiceApplicationBuilder().build()
  val timeout = PatienceConfiguration.Timeout(1 seconds)

  "MusitThing integration " must {
    "get by id" in {
      val future = WS.url(s"http://localhost:$port/v1/1").get()
      whenReady(future, timeout) { response =>
        val json = Json.parse(response.body)
        assert((json \ "id").get.toString() == "1")
      }
    }
  }
}
