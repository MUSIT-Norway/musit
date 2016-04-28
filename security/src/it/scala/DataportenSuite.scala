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
import no.uio.musit.security.{Groups, SecurityConnection}
import no.uio.musit.security.dataporten.{Dataporten}
import org.scalatest.FunSuite
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import play.api.test.TestServer
import play.api.test.Helpers._
import org.scalatest._
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent.Future
import scala.concurrent.duration._

class DataportenSuite extends PlaySpec with ScalaFutures with OneAppPerSuite {
  val expiredToken = "59197195-bf27-4ab1-bf57-b460ed85edab"
  // TODO: Dynamic token, find a way to have a permanent test token with Dataporten
  val token = "d49bba16-f2f0-47cc-aab3-ff316af8c61a"
  //var fut: Future[SecurityConnection] = null


  val additionalConfiguration:Map[String, String] = Map.apply (
    ("slick.dbs.default.driver", "slick.driver.H2Driver$"),
    ("slick.dbs.default.db.driver" , "org.h2.Driver"),
    ("slick.dbs.default.db.url" , "jdbc:h2:mem:play-test"),
    ("evolutionplugin" , "enabled")
  )
  val timeout = PatienceConfiguration.Timeout(1 seconds)
  implicit override lazy val app = new GuiceApplicationBuilder().configure(additionalConfiguration).build()



  /*
  override protected def beforeAll(): Unit = {
    super.beforeAll()

    //This can't be in the constructor, it has to be after the setup because createSecurityConnection accesses the WS object.
    fut = Dataporten.createSecurityConnection(token)
  }
  */

  def runTestWhenReady(token: String) (block: SecurityConnection=>Unit): Unit = {
      whenReady(Dataporten.createSecurityConnection(token), timeout) { sec => block(sec)}}


  def runTestWhenReadyWithTokenAndException(token: String, block: Throwable=>Unit): Unit = {
      whenReady(Dataporten.createSecurityConnection(token).failed, timeout) { ex => block(ex)}}


  "getUserInfo should return something" in {
    runTestWhenReady(token) { sec =>
      val userName = sec.userName
      assert(userName == "Jarle Stabell")
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
      assert(sec.authorize(Seq(Groups.DS, Groups.MusitKonservatorLes)) {}.isSuccess)
    }
  }

  "Authorize for invalid group" in {
    runTestWhenReady(token) { sec =>
      assert(sec.authorize(Seq(Groups.DS, "invalid groupid")) {}.isFailure)
    }
  }

  "Structurally invalid Context/token should fail give bad request" in {
    runTestWhenReadyWithTokenAndException("tullballtoken", e => e mustBe a[MusitBadRequest])
  }

  "Invalid Context/token should fail give auth error" in {
    runTestWhenReadyWithTokenAndException("59197195-bf27-4ab1-bf57-b460ed85abba", e => e mustBe a[MusitAuthFailed])
  }
}