/*
 *  MUSIT is a museum database to archive natural and cultural history data.
 *  Copyright (C) 2017  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License,
 *  or any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package repository

import no.uio.musit.MusitResults.MusitSuccess
import no.uio.musit.test.MusitSpecWithAppPerSuite
import org.scalatest.BeforeAndAfter
import play.api.libs.json._

class ElasticsearchClientSpec extends MusitSpecWithAppPerSuite with BeforeAndAfter {
  implicit val format = Json.format[TestUser]
  val client = fromInstanceCache[ElasticsearchClient]
  val index = "es-spec"

  "ElasticsearchClient" should {
    "insert document into index" in {
      val doc = TestUser("Ola", " Nordmann", 42)

      val result = client.insertDocument(index, "test", "1", Json.toJson(doc)).futureValue

      result mustBe a[MusitSuccess[_]]
      (result.get \ "_version").as[Int] mustBe 1
    }

    "update existing document into index" in {
      val docV1 = TestUser("Ola", "Nordmann", 42)
      val docV2 = TestUser("Ola", "Nordmann", 43)

      val firstResult = client.insertDocument(index, "test", "1", Json.toJson(docV1))
        .futureValue
      firstResult mustBe a[MusitSuccess[_]]

      val secondResult = client.insertDocument(index, "test", "1", Json.toJson(docV2))
        .futureValue

      secondResult mustBe a[MusitSuccess[_]]
      (secondResult.get \ "_version").as[Int] mustBe 2
    }

    "retrieve inserted document" in {
      val doc = TestUser("Ola", "Nordmann", 42)

      val result = client.insertDocument(index, "test", "1", Json.toJson(doc)).futureValue
      result mustBe a[MusitSuccess[_]]

      val document = client.getDocument(index, "test", "1").futureValue
      document mustBe a[MusitSuccess[_]]

      (document.get.get \ "_source").get.as[TestUser] mustBe doc
    }

    "retrieve non existing document" in {
      val document = client.getDocument(index, "test", "42").futureValue
      document mustBe a[MusitSuccess[_]]

      document.get mustBe empty
    }

    "search for documents" in {
      client.insertDocument(index, "test", "1",
        Json.toJson(TestUser("Ola", "Nordmann", 42)), true).futureValue
      client.insertDocument(index, "test", "2",
        Json.toJson(TestUser("Kari", "Nordmann", 32)), true).futureValue
      client.insertDocument(index, "test", "3",
        Json.toJson(TestUser("Pal", "Svendsen", 45)), true).futureValue

      val result = client.doSearch("lastName:Nordmann", None, None).futureValue

      result mustBe a[MusitSuccess[_]]
      println(Json.prettyPrint(result.get)) //todo improve asserts
    }

  }

  before {
    val createIndex: JsValue = JsObject(
      Map("settings" -> JsObject(
        Map("index" -> JsObject(
          Map(
            "number_of_shards" -> JsNumber(2),
            "number_of_replicas" -> JsNumber(1)
          )
        ))
      ))
    )
    client.client(index).put(createIndex).futureValue
  }

  after {
    client.client("_all").delete().futureValue
  }
}

case class TestUser(
  firstName: String,
  lastName: String,
  age: Int
)
