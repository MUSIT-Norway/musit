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

import java.util.UUID

import no.uio.musit.models.{MuseumNo, ObjectId, SubNo}
import no.uio.musit.security.BearerToken
import no.uio.musit.test.{FakeUsers, MusitSpecWithServerPerSuite}
import play.api.libs.json.JsArray
import play.api.test.Helpers._

class ObjectControllerIntegrationSpec extends MusitSpecWithServerPerSuite {

  val fakeToken = BearerToken(FakeUsers.testUserToken)

  val archeologyCollection = "a4d768c8-2bf8-4a8f-8d7e-bc824b52b575"
  val numismaticsCollection = "8ea5fa45-b331-47ee-a583-33cd0ca92c82"

  var url = (mid: Int) => s"/museum/$mid/objects/search"

  "The ObjectController" must {

    "searching for objects" when {

      "find objects in the archeology collection with a specific museumNo" in {

        val res = wsUrl(url(99)).withHeaders(fakeToken.asHeader).withQueryString(
          "collectionIds" -> archeologyCollection,
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
        (first \ "currentLocationId").as[Long] mustBe 4
        (first \ "path").as[String] mustBe ",1,3,4,"
        val firstPnames = (first \ "pathNames").as[JsArray].value
        (firstPnames.head \ "nodeId").as[Long] mustBe 1
        (firstPnames.head \ "name").as[String] mustBe "Utviklingsmuseet"
        (firstPnames.tail.head \ "nodeId").as[Long] mustBe 3
        (firstPnames.tail.head \ "name").as[String] mustBe "Utviklingsmuseet Org"
        (firstPnames.last \ "nodeId").as[Long] mustBe 4
        (firstPnames.last \ "name").as[String] mustBe "Forskningens hus"

        val second = entries.tail.head
        (second \ "museumNo").as[String] mustBe "C666"
        (second \ "subNo").as[String] mustBe "34"
        (second \ "term").as[String] mustBe "Øks"
        (second \ "currentLocationId").as[Long] mustBe 4
        (second \ "path").as[String] mustBe ",1,3,4,"
        val secondPnames = (first \ "pathNames").as[JsArray].value
        (secondPnames.head \ "nodeId").as[Long] mustBe 1
        (secondPnames.head \ "name").as[String] mustBe "Utviklingsmuseet"
        (secondPnames.tail.head \ "nodeId").as[Long] mustBe 3
        (secondPnames.tail.head \ "name").as[String] mustBe "Utviklingsmuseet Org"
        (secondPnames.last \ "nodeId").as[Long] mustBe 4
        (secondPnames.last \ "name").as[String] mustBe "Forskningens hus"

        val third = entries.last
        (third \ "museumNo").as[String] mustBe "C666"
        (third \ "subNo").as[String] mustBe "38"
        (third \ "term").as[String] mustBe "Sommerfugl"
        (third \ "currentLocationId").as[Long] mustBe 4
        (third \ "path").as[String] mustBe ",1,3,4,"
        val thirdPnames = (first \ "pathNames").as[JsArray].value
        (thirdPnames.head \ "nodeId").as[Long] mustBe 1
        (thirdPnames.head \ "name").as[String] mustBe "Utviklingsmuseet"
        (thirdPnames.tail.head \ "nodeId").as[Long] mustBe 3
        (thirdPnames.tail.head \ "name").as[String] mustBe "Utviklingsmuseet Org"
        (thirdPnames.last \ "nodeId").as[Long] mustBe 4
        (thirdPnames.last \ "name").as[String] mustBe "Forskningens hus"
      }

      "find objects for archeology and numismatics with a similar museumNo" in {
        val res = wsUrl(url(99)).withHeaders(fakeToken.asHeader).withQueryString(
          "collectionIds" -> s"$archeologyCollection,$numismaticsCollection",
          "museumNo" -> "555",
          "subNo" -> "",
          "term" -> "",
          "page" -> "1",
          "limit" -> "10"
        ).get().futureValue

        res.status mustBe OK

        val json = res.json

        val entries = (json \ "matches").as[JsArray].value

        entries.size mustBe 7

        entries.exists { js =>
          // Taking a shortcut and explicitly checking for the object in Numismatics
          (js \ "museumNo").as[String] == "F555"
        } mustBe true
      }

      "not allow searching for objects if user doesn't have read access" in {
        val res = wsUrl(url(6)).withHeaders(fakeToken.asHeader).withQueryString(
          "collectionIds" -> "a4d768c8-2bf8-4a8f-8d7e-bc824b52b575",
          "museumNo" -> "FOO6565",
          "subNo" -> "",
          "term" -> "",
          "page" -> "1",
          "limit" -> "3"
        ).get().futureValue.status mustBe FORBIDDEN
      }

      "not allow searching in a collection without access" in {
        val res = wsUrl(url(99)).withHeaders(fakeToken.asHeader).withQueryString(
          "collectionIds" -> s"$numismaticsCollection,${UUID.randomUUID().toString}",
          "museumNo" -> "L234",
          "subNo" -> "",
          "term" -> "",
          "page" -> "1",
          "limit" -> "10"
        ).get().futureValue

        res.status mustBe FORBIDDEN
      }
    }

    "getting objects for a nodeId" should {

      "return objects for nodeId that exists" in {
        val nodeId = 4
        val mid = 99
        val response = wsUrl(s"/museum/$mid/node/$nodeId/objects")
          .withHeaders(fakeToken.asHeader)
          .withQueryString("collectionIds" -> archeologyCollection)
          .get().futureValue
        response.status mustBe OK

        val matches = response.json
        val obj = (matches \ "matches").as[JsArray].value.head

        (obj \ "id").as[ObjectId] mustBe ObjectId(2)
        (obj \ "term").as[String] mustBe "Sverd"
        (obj \ "museumNo").as[MuseumNo] mustBe MuseumNo("C666")
        (obj \ "subNo").as[SubNo] mustBe SubNo("31")
      }

      "return objects for nodeId that has mainObjectId" in {
        val nodeId = 7
        val mid = 99
        val response = wsUrl(s"/museum/$mid/node/$nodeId/objects")
          .withHeaders(fakeToken.asHeader)
          .withQueryString("collectionIds" -> archeologyCollection)
          .get().futureValue
        response.status mustBe OK

        val objects = (response.json \ "matches").as[JsArray].value
        objects.size mustBe 3
        objects.foreach { obj =>
          (obj \ "museumNo").as[MuseumNo] mustBe MuseumNo("K123")
          (obj \ "mainObjectId").as[Long] mustBe 12
        }
      }

      "return the number of results per page specified in the limit argument" in {
        val nodeId = 6
        val mid = 99
        val response = wsUrl(s"/museum/$mid/node/$nodeId/objects")
          .withHeaders(fakeToken.asHeader)
          .withQueryString(
            "collectionIds" -> archeologyCollection,
            "page" -> "1",
            "limit" -> "5"
          )
          .get().futureValue
        response.status mustBe OK

        (response.json \ "matches").as[JsArray].value.size mustBe 5
        (response.json \ "totalMatches").as[Int] mustBe 32
      }

      "return the last page of objects with a specified limit and page size" in {
        val nodeId = 6
        val mid = 99
        val response = wsUrl(s"/museum/$mid/node/$nodeId/objects")
          .withHeaders(fakeToken.asHeader)
          .withQueryString(
            "collectionIds" -> archeologyCollection,
            "page" -> "4",
            "limit" -> "10"
          )
          .get().futureValue
        response.status mustBe OK

        (response.json \ "matches").as[JsArray].value.size mustBe 2
        (response.json \ "totalMatches").as[Int] mustBe 32
      }

      "respond with 404 for nodeId that does not exist" in {
        val nodeId = 99999
        val mid = 99
        val response = wsUrl(s"/museum/$mid/node/$nodeId/objects")
          .withHeaders(fakeToken.asHeader)
          .withQueryString("collectionIds" -> archeologyCollection)
          .get().futureValue
        response.status mustBe NOT_FOUND
        (response.json \ "message").as[String] must endWith(s"$nodeId")
      }

      "respond with 400 if the request URI is missing nodeId " in {
        val nodeId = None
        val mid = 99
        val response = wsUrl(s"/museum/$mid/node/$nodeId/objects")
          .withHeaders(fakeToken.asHeader)
          .withQueryString("collectionIds" -> archeologyCollection)
          .get().futureValue
        response.status mustBe BAD_REQUEST
      }

      "respond with 400 if the museumId is invalid" in {
        val nodeId = 99999
        val mid = 555
        val response = wsUrl(s"/museum/$mid/node/$nodeId/objects")
          .withHeaders(fakeToken.asHeader)
          .withQueryString("collectionIds" -> archeologyCollection)
          .get().futureValue
        response.status mustBe BAD_REQUEST
        (response.json \ "message").as[String] must include(s"$mid")
      }

      "respond with 400 if the museumId is missing from the request URI" in {
        val nodeId = 3
        val mid = None
        val response = wsUrl(s"/museum/$mid/node/$nodeId/objects")
          .withHeaders(fakeToken.asHeader)
          .withQueryString("collectionIds" -> archeologyCollection)
          .get().futureValue
        response.status mustBe BAD_REQUEST
      }

      "respond with 400 if the museumId isn't a valid number" in {
        val nodeId = 3
        val mid = "blæBlæBlæ"
        val response = wsUrl(s"/museum/$mid/node/$nodeId/objects")
          .withHeaders(fakeToken.asHeader)
          .withQueryString("collectionIds" -> archeologyCollection)
          .get().futureValue
        response.status mustBe BAD_REQUEST
      }

      "respond with 403 if the user doesn't have read access to the museum" in {
        val nodeId = 99999
        val mid = 6
        val response = wsUrl(s"/museum/$mid/node/$nodeId/objects")
          .withHeaders(fakeToken.asHeader)
          .withQueryString("collectionIds" -> archeologyCollection)
          .get().futureValue
        response.status mustBe FORBIDDEN
      }

      "respond with 403 if the user doesn't have access to the collection" in {
        val nodeId = 345
        val mid = 99
        val collection = UUID.randomUUID().toString
        val response = wsUrl(s"/museum/$mid/node/$nodeId/objects")
          .withHeaders(fakeToken.asHeader)
          .withQueryString("collectionIds" -> collection)
          .get().futureValue
        response.status mustBe FORBIDDEN
      }
    }

    "scanning barcodes" should {
      "find an object when calling the scan service with an old barcode" in {
        val oldBarcode = "1111111111"
        val mid = 99
        val res = wsUrl(s"/museum/$mid/scan")
          .withHeaders(fakeToken.asHeader)
          .withQueryString("oldBarcode" -> oldBarcode)
          .withQueryString("collectionIds" -> archeologyCollection)
          .get().futureValue

        res.status mustBe OK
        val objects = res.json.as[JsArray].value
        objects.size mustBe 1
        (objects.head \ "term").as[String] mustBe "Øks"
        (objects.head \ "museumNo").as[String] mustBe "C666"
        (objects.head \ "subNo").as[String] mustBe "34"
      }
    }
  }

}

