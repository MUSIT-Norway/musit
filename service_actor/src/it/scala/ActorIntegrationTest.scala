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
import no.uio.musit.microservice.actor.dao.ActorDao
import no.uio.musit.microservice.actor.domain.Actor
import no.uio.musit.microservices.common.PlayTestDefaults
import no.uio.musit.microservices.common.linking.LinkService
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json._
import play.api.libs.ws.WS

/**
 * add your integration spec here.
 * An integration test will fire up a whole play application in a real (or headless) browser
 */
class ActorIntegrationTest extends PlaySpec with OneServerPerSuite  with ScalaFutures {

  val timeout = PlayTestDefaults.timeout
  override lazy val port: Int = 19002
  implicit override lazy val app = new GuiceApplicationBuilder().configure(PlayTestDefaults.inMemoryDatabaseConfig()).build()

  "Actorintegration " must {
    "get by id" in {
      val future = WS.url(s"http://localhost:$port/v1/1").get()
      whenReady(future, timeout) { response =>
        val json = Json.parse(response.body)
        assert((json \ "id").getOrElse(JsString("0")).toString() == "1")
      }
    }
  }


  "Actor dao" must {
    import ActorDao._

    val largeNumber = 6386363673636335366L

    "getById_kjempeTall" in {
      val svar = getById(largeNumber)
      whenReady(svar, timeout) { thing =>
        assert (thing.isEmpty )
      }
    }

    "getById__Riktig" in {
      val svar = getById(1)
      whenReady(svar, timeout) { thing =>
        assert (thing == Some(Actor(1,"And, Arne1", Seq(LinkService.self("/v1/1")))))
      }
    }

    "getById__TalletNull" in {
      val svar = getById(0)
      whenReady(svar, timeout) { thing =>
        assert (thing == None)
      }
    }
  }
}
