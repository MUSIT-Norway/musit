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
  * Created by jstabel on 4/26/16.
  */


import no.uio.musit.security._
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}

import play.api.Play.current
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent.duration._

class CachedSecuritySuite extends PlaySpec with ScalaFutures with OneAppPerSuite {

  val additionalConfiguration: Map[String, String] = Map.apply(
    ("slick.dbs.default.driver", "slick.driver.H2Driver$"),
    ("slick.dbs.default.db.driver", "org.h2.Driver"),
    ("slick.dbs.default.db.url", "jdbc:h2:mem:play-test"),
    ("evolutionplugin", "enabled")
  )
  implicit override lazy val app = new GuiceApplicationBuilder().configure(additionalConfiguration).build()

  val timeout = PatienceConfiguration.Timeout(1 seconds)

  val groups = List("Admin", "EtnoSkriv", "EtnoLes")


  "CachedSecuritySuite" must {
    "main test" in {
      play.api.Play.current.configuration.getInt("security.musit.bla")
      val accessToken = "Jarle"

      whenReady(FakeSecurity.createHardcoded(accessToken, groups, true), timeout) { sec =>

        val cacheReader = sec.infoProvider.asInstanceOf[SecurityCacheReader]


        assert(cacheReader.accessTokenToUserIdFromCache(accessToken).isDefined)
        assert(cacheReader.accessTokenToUserIdFromCache(accessToken).get == "Jarle")

        val userInfo = cacheReader.accessTokenToUserInfoFromCache(accessToken)
        val userInfoMustBe = Some(UserInfo("Jarle", "Jarle"))
        assert(userInfo == userInfoMustBe)

        val groupIds = cacheReader.accessTokenToGroupIds(accessToken)
        assert(groupIds == Some(groups))

      }
    }
  }
}
