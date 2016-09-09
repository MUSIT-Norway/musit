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

import no.uio.musit.microservices.common.PlayTestDefaults
import no.uio.musit.microservices.common.extensions.PlayExtensions.{MusitAuthFailed, MusitBadRequest}
import no.uio.musit.security.{Groups, SecurityConnection}
import no.uio.musit.security.Dataporten
import org.scalatest.Ignore
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}

@Ignore
class DataportenSuite extends PlaySpec with ScalaFutures with OneAppPerSuite {
  val expiredToken = "59197195-bf27-4ab1-bf57-b460ed85edab"
  // TODO: Dynamic token, find a way to have a permanent test token with Dataporten
  val token = "fe6f7e0a-735b-4a0a-a565-ce33e55c5bc1"

  val timeout = PlayTestDefaults.timeout

  def runTestWhenReady(token: String) (block: SecurityConnection=>Unit): Unit = {
      whenReady(Dataporten.createSecurityConnection(token), timeout) { sec => block(sec)}}


  def runTestWhenReadyWithTokenAndException(token: String, block: Throwable => Unit): Unit = {
    whenReady(Dataporten.createSecurityConnection(token).failed, timeout) { ex => block(ex) }
  }

  "getUserInfo should return something" in {
    runTestWhenReady(token) { sec =>
      val userName = sec.userName
      userName mustBe "Jarle Stabell"
      sec.userEmail mustBe Some("jarle.stabell@usit.uio.no")
      assert(userName.length > 0)
    }
  }

  "Authorize for DS" in {
    runTestWhenReady(token) { sec =>
      assert(sec.authorize(Seq(Groups.DS)) {}.isSuccess)
    }
  }


  "Authorize for DS and MusitKonservatorLes" in {
    runTestWhenReady(token) { sec =>
      assert(sec.authorize(Seq(Groups.DS, Groups.MusitStorageRead)) {}.isSuccess)
    }
  }

  "Authorize for invalid group" in {
    runTestWhenReady(token) { sec =>
      assert(sec.authorize(Seq("invalid groupid")) {}.isFailure)
    }
  }

  "Structurally invalid Context/token should fail give bad request" in {
    runTestWhenReadyWithTokenAndException("tullballtoken", e => e mustBe a[MusitBadRequest])
  }

  "Invalid Context/token should fail give auth error" in {
    runTestWhenReadyWithTokenAndException("59197195-bf27-4ab1-bf57-b460ed85abba", e => e mustBe a[MusitAuthFailed])
  }
}