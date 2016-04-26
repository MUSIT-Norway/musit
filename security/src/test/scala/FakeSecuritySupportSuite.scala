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

import no.uio.musit.microservices.common.PlayDatabaseTest
import no.uio.musit.microservices.common.extensions.PlayExtensions.MusitAuthFailed
import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.security._
import org.scalatest.FunSuite
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global


class FakeSecuritySupportSuite extends PlayDatabaseTest with ScalaFutures {


  val groups = List("Admin", "EtnoSkriv", "EtnoLes")

  FakeSecurity.createHardcoded("Kalle Kanin", groups).map { sec =>

    test("should execute if has groups") {
      sec.authorize(Seq("Admin")) {
        Future(println("Authorized: should execute if has groups"))
      }
      println("Ferdig: should execute if has groups")
    }


    test("should fail if has deniedGroups") {
 {
      sec.authorize(Seq("Admin"), Seq("EtnoLes")) {
        println("Denne skal ikke synes!!!")
      }.failed

        /*
              intercept[Exception] {
      sec.authorize(Seq("Admin"), Seq("EtnoLes")) {
        println("Denne skal ikke synes!!!")
      }

         */
      }
    }


    test("in-memory test") {
      FakeSecurity.createInMemory("jarle").map { sec =>
        assert(sec.hasGroup("FotoLes"))
        assert(!sec.hasGroup("tullballgroup"))
        println("in-memory test")
      }



    }
    test("Should fail to user unknown user") {
      val fut=FakeSecurity.createInMemory("ukjentbruker")
      ScalaFutures.whenReady(fut.failed) {e => e shouldBe a [Exception]  }
    }

  }.awaitInSeconds(5)
}
