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



import no.uio.musit.microservices.common.extensions.FutureExtensions._
import no.uio.musit.security._
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.{FlatSpec, FunSuite, Matchers}
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.Play
import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global
//import play.api.Play.current
import play.api.cache.Cache
import play.api.cache.EhCacheModule
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent.Future

class CachedSecuritySuite extends PlaySpec with ScalaFutures with OneAppPerSuite {

  val additionalConfiguration:Map[String, String] = Map.apply (
    ("slick.dbs.default.driver", "slick.driver.H2Driver$"),
    ("slick.dbs.default.db.driver" , "org.h2.Driver"),
    ("slick.dbs.default.db.url" , "jdbc:h2:mem:play-test"),
    ("evolutionplugin" , "enabled")
  )
  implicit override lazy val app = new GuiceApplicationBuilder().configure(additionalConfiguration).build()

  val groups = List("Admin", "EtnoSkriv", "EtnoLes")
  //val cacheImp = new SecurityCacheImp
  "running with application" must {
    "test" in {
/*
      val v = cacheImp.cache.getAs[String]("token1")
      assert(!v.isDefined)
      val fut = cacheImp.accessTokenToUserId("token1", {acc=> Future(s"userId$acc")})
      whenReady(fut) {v=> assert(v=="userIdtoken1")

        val v2 = cacheImp.cachedAccessTokenToUserId("token1")
        assert(v2.isDefined)
        assert(v2==Some("userIdtoken1"))
*/
      }


  }



    }
