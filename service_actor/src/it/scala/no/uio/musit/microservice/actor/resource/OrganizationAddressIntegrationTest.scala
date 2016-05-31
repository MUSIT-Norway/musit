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
import no.uio.musit.microservice.actor.domain.{Organization, OrganizationAddress, Person}
import no.uio.musit.microservices.common.PlayTestDefaults
import no.uio.musit.microservices.common.domain.{MusitError, MusitStatusMessage}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json._
import play.api.libs.ws.WS

/**
 * add your integration spec here.
 * An integration test will fire up a whole play application in a real (or headless) browser
 */
class OrganizationAddressIntegrationTest extends PlaySpec with OneServerPerSuite  with ScalaFutures {
  val timeout = PlayTestDefaults.timeout
  override lazy val port: Int = 19006
  implicit override lazy val app = new GuiceApplicationBuilder().configure(PlayTestDefaults.inMemoryDatabaseConfig()).build()

  "OrganizationAddressIntegration " must {
    "get by id" in {
      val future = WS.url(s"http://localhost:$port/v1/organization/1/address/1").get()
      whenReady(future, timeout) { response =>
        val addr = Json.parse(response.body).validate[OrganizationAddress].get
        addr.id mustBe 1
        addr.organizationId mustBe 1
      }
    }
    "negative get by id" in {
      val future = WS.url(s"http://localhost:$port/v1/organization/1/address/9999").get()
      whenReady(future, timeout) { response =>
        val error = Json.parse(response.body).validate[MusitError].get
        error.message mustBe "Did not find object with id: 9999"
      }
    }
    "get all addresses for an organization" in {
      val future = WS.url(s"http://localhost:$port/v1/organization/1/address").get()
      whenReady(future, timeout) { response =>
        val orgs = Json.parse(response.body).validate[Seq[OrganizationAddress]].get
        orgs.length mustBe 1
      }
    }
    "create address" in {
      val future = WS.url(s"http://localhost:$port/v1/organization/1/address").post(Json.toJson(OrganizationAddress(2, 1, "TEST", "Foo street 2", "Bar place", "0001", "Norway", 0.0, 0.0, Seq.empty)))
      whenReady(future, timeout) { response =>
        val addr = Json.parse(response.body).validate[OrganizationAddress].get
        addr.id mustBe 2
        addr.organizationId mustBe 1
        addr.addressType mustBe "TEST"
        addr.streetAddress mustBe "Foo street 2"
      }
    }
    "update address" in {
      val future = WS.url(s"http://localhost:$port/v1/organization/1/address/2").put(Json.toJson(OrganizationAddress(2, 1, "TEST", "Foo street 3", "Bar place", "0001", "Norway", 0.0, 0.0, Seq.empty)))
      whenReady(future, timeout) { response =>
        val status = Json.parse(response.body).validate[MusitStatusMessage].get
        status.message mustBe "Record was updated!"
      }
    }
    "delete address" in {
      val future = WS.url(s"http://localhost:$port/v1/organization/1/address/2").delete()
      whenReady(future, timeout) { response =>
        val msm = Json.parse(response.body).validate[MusitStatusMessage].get
        msm.message mustBe "Deleted 1 record(s)."
      }
    }
  }

}
