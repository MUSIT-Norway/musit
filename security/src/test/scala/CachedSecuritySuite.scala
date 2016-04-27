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
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, FunSuite, Matchers}
import org.scalatestplus.play.PlaySpec
import play.Play
import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current
import play.api.cache.Cache //{Cache}

class CachedSecuritySuite extends PlaySpec with ScalaFutures {

/*
  def getCache = {
  val cacheManager = Play.application.plugin[EhCachePlugin].getOrElse(throw new RuntimeException("EhCachePlugin not loaded")).manager
    cacheManager.getCache("play")


  }
}
*/

  val groups = List("Admin", "EtnoSkriv", "EtnoLes")

  "running with application" must {
    "test" in {

      val c = Cache.get("feide.user.<key>")


    }

  }



    }
