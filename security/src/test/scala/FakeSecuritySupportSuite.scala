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

import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.security._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, FunSuite, Matchers}
import org.scalatestplus.play.PlaySpec
import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global

class FakeSecuritySupportSuite extends PlaySpec with ScalaFutures {

  val groups = List("Admin", "EtnoSkriv", "EtnoLes")

  "running hardcoded fake-security tests" must {
  FakeSecurity.createHardcoded("Kalle Kanin", groups).map { sec =>

    "should execute if has groups" in {
      assert(sec.authorize(Seq("Admin")) {
        Logger.debug("Authorized: should execute if has groups")
      }.isSuccess)
    }


    "should fail if has deniedGroups" in {
      {
        assert(sec.authorize(Seq("Admin"), Seq("EtnoLes")) {
          Logger.debug("Denne skal ikke synes!!!")
        }.isFailure)
      }
    }

  }.awaitInSeconds(5)}


  "running semi-hardcoded (in-memory) fake-security tests" must {
  FakeSecurity.createInMemory("jarle").map { sec =>

    "should execute if has groups" in {
      assert(sec.authorize(Seq(FakeSecurityUsersAndGroups.etnoLesGroupName)) {
        Logger.debug("Authorized: should execute if has groups")
      }.isSuccess)
    }


    "should fail if has deniedGroups" in {
      {
        assert(sec.authorize(Seq(FakeSecurityUsersAndGroups.fotoLesGroupName), Seq(FakeSecurityUsersAndGroups.etnoLesGroupName)) {
          Logger.debug("Denne skal ikke synes!!!")
        }.isFailure)
      }
    }

    "in-memory test" in {
      FakeSecurity.createInMemory("jarle").map { sec =>
        assert(sec.hasGroup("FotoLes"))
        assert(!sec.hasGroup("tullballgroup"))
        Logger.debug("in-memory test")
      }
    }
    "Should fail to user unknown user" in {
      val fut = FakeSecurity.createInMemory("ukjentbruker")
      ScalaFutures.whenReady(fut.failed) { e => e mustBe a[Exception] }
    }


  }.awaitInSeconds(5)}

}