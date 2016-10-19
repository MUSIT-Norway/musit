/*
 * MUSIT is a museum database to archive natural and cultural history data.
 * Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package controllers

import com.google.inject.Inject
import no.uio.musit.test.MusitSpecWithServerPerSuite
import org.scalatest.time.{Millis, Seconds, Span}

import scala.language.postfixOps

class ObjectSearchIntegrationSpec @Inject() () extends MusitSpecWithServerPerSuite {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  var url = (mid: Int) => s"/museum/$mid/objects/search"

  "ObjectSearch" must {

    "find an object that exist with a specific museumNo" in {

      val res = wsUrl(url(1)).withQueryString(
        "museumNo" -> "C666",
        "subNo" -> "",
        "term" -> "",
        "page" -> "1",
        "limit" -> "3"
      ).get().futureValue

      res.status mustBe 200
      res.body must include("C666")
      res.body must include("Øks")
    }

    // TODO: There needs to be _loads_ more tests here!
  }

}

