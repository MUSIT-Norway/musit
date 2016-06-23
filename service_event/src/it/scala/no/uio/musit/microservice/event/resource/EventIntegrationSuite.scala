/*
 *   MUSIT is a cooperation between the university museums of Norway.
 *   Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License,
 *   or any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License along
 *   with this program; if not, write to the Free Software Foundation, Inc.,
 *   51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package no.uio.musit.microservice.event.resource

import no.uio.musit.microservice.event.domain.MoveEvent
import no.uio.musit.microservices.common.PlayTestDefaults
import no.uio.musit.microservices.common.PlayTestDefaults._
import no.uio.musit.microservices.common.extensions.PlayExtensions._
import no.uio.musit.microservices.common.utils.Misc._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WS

/**
  * Created by jstabel on 6/10/16.
  */


class EventIntegrationSuite extends PlaySpec with OneServerPerSuite with ScalaFutures {
  val timeout = PlayTestDefaults.timeout
  override lazy val port: Int = 19010
  implicit override lazy val app = new GuiceApplicationBuilder().configure(PlayTestDefaults.inMemoryDatabaseConfig()).build()


  def createEvent(json: String) = {
    WS.url(s"http://localhost:$port/v1/event").postJsonString(json) |> waitFutureValue
  }


  def getEvent(id: Long) = {
    WS.url(s"http://localhost:$port/v1/event/$id").get |> waitFutureValue
  }


  "EventIntegrationSuite " must {
    "post" in {
      val json =
        """
  {
   "eventType": "move",
   "eventData": {"note": "Dette er et viktig notat!"},
   "links": [{"rel": "actor", "href": "actor/12"}]}"""

      val response = createEvent(json)
      response.status mustBe 201
      println(s"Create: ${response.body}")


      val responseGet = getEvent(1)
      responseGet.status mustBe 200
      println(s"Get: ${responseGet.body}")

    }


    "postWithoutLinks" in {
      val json = MoveEvent

      val response = createEvent(json)
      response.status mustBe 201

      val responseGet = getEvent(2)
      responseGet.status mustBe 200
      println(responseGet.body)

    }

    "postWithWrongEvent" in {
      val json =
        """
  {
   "eventType": "hurra",
   "eventData": {"note": "Dette er IKKE viktig notat!"}}"""

      val response = createEvent(json)
      response.status mustBe 400
    }


  }

}

