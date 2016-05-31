package no.uio.musit.microservice.actor.resource

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
import no.uio.musit.microservice.actor.domain.Person
import no.uio.musit.microservices.common.PlayTestDefaults
import no.uio.musit.microservices.common.domain.MusitError
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json._
import play.api.libs.ws.WS

/**
 * add your integration spec here.
 * An integration test will fire up a whole play application in a real (or headless) browser
 */
class PersonIntegrationTest extends PlaySpec with OneServerPerSuite  with ScalaFutures {
  val timeout = PlayTestDefaults.timeout
  override lazy val port: Int = 19008
  implicit override lazy val app = new GuiceApplicationBuilder().configure(PlayTestDefaults.inMemoryDatabaseConfig()).build()

  "LegacyPersonIntegration " must {
    "get by id" in {
      val future = WS.url(s"http://localhost:$port/v1/person/1").get()
      whenReady(future, timeout) { response =>
        val person = Json.parse(response.body).validate[Person].get
        person.id mustBe 1
      }
    }
    "negative get by id" in {
      val future = WS.url(s"http://localhost:$port/v1/person/9999").get()
      whenReady(future, timeout) { response =>
        val error = Json.parse(response.body).validate[MusitError].get
        error.message mustBe "Did not find object with id: 9999"
      }
    }
    "search on person" in {
      val future = WS.url(s"http://localhost:$port/v1/person?search=[And]").get()
      whenReady(future, timeout) { response =>
        val persons = Json.parse(response.body).validate[Seq[Person]].get
        persons.length mustBe 1
        persons.head.fn mustBe "And, Arne1"
      }
    }
  }

}
