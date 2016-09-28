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

/**
  * Created by jstabel on 4/4/16.
  */

import no.uio.musit.security._
import org.scalatest.Ignore
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.Logger
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent.duration._

@Ignore
class FakeSecuritySupportSuite extends PlaySpec with ScalaFutures with OneAppPerSuite {


  val additionalConfiguration: Map[String, String] = Map.apply(
    ("slick.dbs.default.driver", "slick.driver.H2Driver$"),
    ("slick.dbs.default.db.driver", "org.h2.Driver"),
    ("slick.dbs.default.db.url", "jdbc:h2:mem:play-test"),
    ("evolutionplugin", "enabled")
  )
  implicit override lazy val app = new GuiceApplicationBuilder().configure(additionalConfiguration).build()

  val timeout = PatienceConfiguration.Timeout(1 seconds)

  def runTestWhenReadyHardcoded(block: AuthenticatedUser => Unit): Unit = {
    whenReady(FakeSecurity.createHardcoded("Kalle Kanin", groups, false), timeout) { sec => block(sec) }
  }

  def runTestWhenReadyInMemory(userName: String)(block: AuthenticatedUser => Unit): Unit = {
    whenReady(FakeSecurity.createInMemory(userName, true), timeout) { sec => block(sec) }
  }


  val groups = List("Admin", "EtnoSkriv", "EtnoLes")


  "running hardcoded fake-security tests" must {

    "should execute if has groups" in {
      runTestWhenReadyHardcoded { sec =>
        assert(sec.authorize(Seq("Admin")) {
          Logger.debug("Authorized: should execute if has groups")
        }.isSuccess)
      }
    }

    "should fail if has deniedGroups" in {
      runTestWhenReadyHardcoded { sec => {
        assert(sec.authorize(Seq("Admin"), Seq("EtnoLes")) {
          Logger.debug("Denne skal ikke synes!!!")
        }.isFailure)
      }
      }
    }
  }


  "running semi-hardcoded (in-memory) fake-security tests" must {
    "should execute if has groups" in {

      runTestWhenReadyInMemory("jarle") { sec =>
        assert(sec.authorize(Seq(FakeSecurityUsersAndGroups.etnoLesGroupName)) {
          Logger.debug("Authorized: should execute if has groups")
        }.isSuccess)
      }
    }


    "should fail if has deniedGroups" in {
      runTestWhenReadyInMemory("jarle") { sec => {
        assert(sec.authorize(Seq(FakeSecurityUsersAndGroups.fotoLesGroupName), Seq(FakeSecurityUsersAndGroups.etnoLesGroupName)) {
          Logger.debug("Denne skal ikke synes!!!")
        }.isFailure)
      }
      }
    }

    "in-memory test" in {
      runTestWhenReadyInMemory("jarle") { sec =>
        assert(sec.hasGroup("FotoLes"))
        assert(!sec.hasGroup("tullballgroup"))
        Logger.debug("in-memory test")
      }
    }
    "in-memory test ny fake bruker" in {
            runTestWhenReadyInMemory("LineArildSjo") { sec =>
                assert(!sec.hasGroup("tullballgroup"))
              }
          }

    "Should fail to user unknown user" in {
      val fut = FakeSecurity.createInMemory("ukjentbruker", true)
      ScalaFutures.whenReady(fut.failed) { e => e mustBe a[Exception] }
    }
  }
}