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
  * Created by jstabel on 3/31/16.
  */

import no.uio.musit.microservices.common.PlayDatabaseTest
import no.uio.musit.microservices.common.extensions.PlayExtensions.{MusitAuthFailed, MusitBadRequest}

import scala.concurrent.Future
import scala.concurrent.duration._

class DataportenSuite extends PlayDatabaseTest with ScalaFutures {
  val expiredToken = "59197195-bf27-4ab1-bf57-b460ed85edab"
  // TODO: Dynamic token, find a way to have a permanent test token with Dataporten
  val token = "259239f9-4012-4c62-8de5-a1753931445e"
  var fut: Future[SecurityConnection] = null

  def timeout = PatienceConfiguration.Timeout(1 seconds)

  override protected def beforeAll(): Unit = {
    super.beforeAll()

    //This can't be in the constructor, it has to be after the setup because createSecurityConnection accesses the WS object.
    fut = Dataporten.createSecurityConnection(token)
  }

  test("getUserInfo should return something") {
    whenReady(fut, timeout) { sec =>
      val userName = sec.userName
      assert(userName == "Jarle Stabell")
      assert(userName.length > 0)
    }
  }

  test("Authorize for DS") {
    whenReady(fut, timeout) { sec =>
      assert(sec.authorize(Seq(Groups.DS)) {}.isSuccess)
    }
  }

  test("Authorize for DS and MusitKonservatorLes") {
    whenReady(fut, timeout) { sec =>
      assert(sec.authorize(Seq(Groups.DS, Groups.MusitKonservatorLes)) {}.isSuccess)
    }
  }

  test("Authorize for invalid group") {
    whenReady(fut, timeout) { sec =>
      assert(sec.authorize(Seq(Groups.DS, "invalid groupid")) {}.isFailure)
    }
  }

  test("Structurally invalid Context/token should fail give bad request") {
    val f = Dataporten.createSecurityConnection("tullballtoken")
    whenReady(f.failed, timeout) { e => e shouldBe a[MusitBadRequest] }
  }

  test("Invalid Context/token should fail give auth error") {
    val f = Dataporten.createSecurityConnection("59197195-bf27-4ab1-bf57-b460ed85abba")
    whenReady(f.failed, timeout) { e => e shouldBe a[MusitAuthFailed] }
  }
}