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

package no.uio.musit.microservices.common.extensions

import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.Logger
import play.api.libs.ws.{WS, WSRequest}
import no.uio.musit.microservices.common.extensions.PlayExtensions._

/**
  * Created by jstabel on 5/11/16.
  */
class PlayExtensionsTest extends PlaySpec with OneAppPerSuite with ScalaFutures{
  "running PlayExtensions test" must {

    "should extract Bearer token" in {
      val myToken="myFineToken"
      val req = WS.url("http://tullball.no") // .type WSRequest()
      val finalReq = req.withBearerToken(myToken)
      val extractedToken = finalReq.getBearerToken
      assert(Some(myToken)==extractedToken)
    }
  }
}

