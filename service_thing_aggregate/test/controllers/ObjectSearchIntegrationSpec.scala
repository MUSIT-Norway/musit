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

import no.uio.musit.security.{BearerToken, FakeAuthenticator}
import no.uio.musit.test.MusitSpecWithServerPerSuite
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.libs.json.JsArray
import play.api.test.Helpers._

import scala.language.postfixOps

class ObjectSearchIntegrationSpec extends MusitSpecWithServerPerSuite {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  val fakeToken = BearerToken(FakeAuthenticator.fakeAccessTokenPrefix + "musitTestUser")

  var url = (mid: Int) => s"/museum/$mid/objects/search"

  "ObjectSearch" must {

    "find an object that exist with a specific museumNo" in {

      val res = wsUrl(url(99)).withHeaders(fakeToken.asHeader).withQueryString(
        "museumNo" -> "C666",
        "subNo" -> "",
        "term" -> "",
        "page" -> "1",
        "limit" -> "3"
      ).get().futureValue

      res.status mustBe OK

      val json = res.json

      val entries = (json \ "matches").as[JsArray].value

      entries.size mustBe 3

      val first = entries.head
      (first \ "museumNo").as[String] mustBe "C666"
      (first \ "subNo").as[String] mustBe "31"
      (first \ "term").as[String] mustBe "Sverd"
      (first \ "currentLocationId").as[Long] mustBe 3
      (first \ "path").as[String] mustBe ",1,2,3,"
      val firstPnames = (first \ "pathNames").as[JsArray].value
      (firstPnames.head \ "nodeId").as[Long] mustBe 1
      (firstPnames.head \ "name").as[String] mustBe "Utviklingsmuseet"
      (firstPnames.tail.head \ "nodeId").as[Long] mustBe 2
      (firstPnames.tail.head \ "name").as[String] mustBe "Utviklingsmuseet Org"
      (firstPnames.last \ "nodeId").as[Long] mustBe 3
      (firstPnames.last \ "name").as[String] mustBe "Forskningens hus"

      val second = entries.tail.head
      (second \ "museumNo").as[String] mustBe "C666"
      (second \ "subNo").as[String] mustBe "34"
      (second \ "term").as[String] mustBe "Øks"
      (second \ "currentLocationId").as[Long] mustBe 3
      (second \ "path").as[String] mustBe ",1,2,3,"
      val secondPnames = (first \ "pathNames").as[JsArray].value
      (secondPnames.head \ "nodeId").as[Long] mustBe 1
      (secondPnames.head \ "name").as[String] mustBe "Utviklingsmuseet"
      (secondPnames.tail.head \ "nodeId").as[Long] mustBe 2
      (secondPnames.tail.head \ "name").as[String] mustBe "Utviklingsmuseet Org"
      (secondPnames.last \ "nodeId").as[Long] mustBe 3
      (secondPnames.last \ "name").as[String] mustBe "Forskningens hus"

      val third = entries.last
      (third \ "museumNo").as[String] mustBe "C666"
      (third \ "subNo").as[String] mustBe "38"
      (third \ "term").as[String] mustBe "Sommerfugl"
      (third \ "currentLocationId").as[Long] mustBe 3
      (third \ "path").as[String] mustBe ",1,2,3,"
      val thirdPnames = (first \ "pathNames").as[JsArray].value
      (thirdPnames.head \ "nodeId").as[Long] mustBe 1
      (thirdPnames.head \ "name").as[String] mustBe "Utviklingsmuseet"
      (thirdPnames.tail.head \ "nodeId").as[Long] mustBe 2
      (thirdPnames.tail.head \ "name").as[String] mustBe "Utviklingsmuseet Org"
      (thirdPnames.last \ "nodeId").as[Long] mustBe 3
      (thirdPnames.last \ "name").as[String] mustBe "Forskningens hus"
    }

    "not allow searching for objects if user doesn't have read access" in {
      val res = wsUrl(url(6)).withHeaders(fakeToken.asHeader).withQueryString(
        "museumNo" -> "FOO6565",
        "subNo" -> "",
        "term" -> "",
        "page" -> "1",
        "limit" -> "3"
      ).get().futureValue.status mustBe FORBIDDEN
    }

    // TODO: There needs to be _loads_ more tests here!
  }

}

