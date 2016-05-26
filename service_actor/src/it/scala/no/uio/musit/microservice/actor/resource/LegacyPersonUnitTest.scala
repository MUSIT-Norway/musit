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

import no.uio.musit.microservice.actor.dao.ActorDao
import no.uio.musit.microservice.actor.domain.Person
import no.uio.musit.microservices.common.PlayTestDefaults
import no.uio.musit.microservices.common.linking.LinkService
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.inject.guice.GuiceApplicationBuilder

class LegacyPersonUnitTest extends PlaySpec with OneAppPerSuite with ScalaFutures {

  val timeout = PlayTestDefaults.timeout
  implicit override lazy val app = new GuiceApplicationBuilder().configure(PlayTestDefaults.inMemoryDatabaseConfig()).build()

  "Actor dao" must {
    import ActorDao._


    "getById_kjempeTall" in {
      val svar = getPersonLegacyById(6386363673636335366L)
      whenReady(svar, timeout) { thing =>
        assert (thing == None)
      }
    }

    "getById__Riktig" in {
      val svar = getPersonLegacyById(1)
      whenReady(svar, timeout) { thing =>
        assert (thing == Some(Person(1, "And, Arne1", links = Seq(LinkService.self("/v1/person/1")))))
      }
    }

    "getById__TalletNull" in {
      val svar = getPersonLegacyById(0)
      whenReady(svar, timeout) { thing =>
        assert (thing == None)
      }
    }
  }
}
