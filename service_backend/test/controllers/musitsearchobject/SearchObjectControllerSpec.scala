package controllers.musitsearchobject

import java.util.UUID

import no.uio.musit.models._
import no.uio.musit.security.BearerToken
import no.uio.musit.test.{FakeUsers, MusitSpecWithServerPerSuite}
import play.api.libs.json.{JsArray, JsNumber}
import play.api.libs.ws.EmptyBody
import play.api.test.Helpers._

//Hint, to run only this test, type:
//test-only controllers.musitsearchobject.SearchObjectControllerSpec

class SearchObjectControllerSpec extends MusitSpecWithServerPerSuite {

  val fakeToken = BearerToken(FakeUsers.testUserToken)

  override def beforeTests(): Unit = {
    println("<beforeTests, building search table>")
    val reIndexUrl = "/admin/reindex/searchDb"
    val res =
      wsUrl(reIndexUrl).withHttpHeaders(fakeToken.asHeader).post(EmptyBody).futureValue
    println("</beforeTests, building search table>")
  }

  val archeologyCollection =
    MuseumCollections.Archeology.uuid.asString //  "2e4f2455-1b3b-4a04-80a1-ba92715ff613"
  val numismaticsCollection =
    MuseumCollections.Numismatics.uuid.asString //  "8bbdf9b3-56d1-479a-9509-2ea82842e8f8"
  val ethnoCollection =
    MuseumCollections.Ethnography.uuid.asString // "88b35138-24b5-4e62-bae4-de80fae7df82"

  "The SearchObjectController" when {
    val url = (mid: Int) => s"/museum/$mid/objects/searchDb"

    "searching for objects" should {
      "find objects in the archeology collection with a specific museumNo" in {

        val res = wsUrl(url(99))
          .withHttpHeaders(fakeToken.asHeader)
          .withQueryStringParameters(
            "collectionIds" -> archeologyCollection,
            "museumNo"      -> "C666",
            "subNo"         -> "",
            "term"          -> "",
            "from"          -> "0",
            "limit"         -> "3"
          )
          .get()
          .futureValue

//        print(s"res:${res.body}")
        res.status mustBe OK

        val json       = res.json
        val totalCount = (json \ "totalMatches").as[JsNumber].value.intValue()

        totalCount must be > 0

        val entries = (json \ "matches").as[JsArray].value

        entries.size mustBe 3

        val first = entries.head
        (first \ "museumNo").as[String] mustBe "C666"
        (first \ "subNo").as[String] mustBe "31"
        (first \ "term").as[String] mustBe "Sverd"
        val second = entries.tail.head
        (second \ "museumNo").as[String] mustBe "C666"
        (second \ "subNo").as[String] mustBe "34"
        (second \ "term").as[String] mustBe "Øks"
        val third = entries.last
        (third \ "museumNo").as[String] mustBe "C666"
        (third \ "subNo").as[String] mustBe "38"
        (third \ "term").as[String] mustBe "Sommerfugl"
      }

      "find objects in the archeology collection with a specific museumNoAsNumber" in {

        val res = wsUrl(url(99))
          .withHttpHeaders(fakeToken.asHeader)
          .withQueryStringParameters(
            "collectionIds"     -> archeologyCollection,
            "museumNoAsANumber" -> "666",
            "subNo"             -> "",
            "term"              -> "",
            "from"              -> "0",
            "limit"             -> "3"
          )
          .get()
          .futureValue

        //        print(s"res:${res.body}")
        res.status mustBe OK

        val json       = res.json
        val totalCount = (json \ "totalMatches").as[JsNumber].value.intValue()

        totalCount must be > 0

        val entries = (json \ "matches").as[JsArray].value

        entries.size mustBe 3

        val first = entries.head
        (first \ "museumNo").as[String] mustBe "C666"
        (first \ "subNo").as[String] mustBe "31"
        (first \ "term").as[String] mustBe "Sverd"
        val second = entries.tail.head
        (second \ "museumNo").as[String] mustBe "C666"
        (second \ "subNo").as[String] mustBe "34"
        (second \ "term").as[String] mustBe "Øks"
        val third = entries.last
        (third \ "museumNo").as[String] mustBe "C666"
        (third \ "subNo").as[String] mustBe "38"
        (third \ "term").as[String] mustBe "Sommerfugl"
      }

      "find objects for archeology and numismatics with a similar museumNo" in {
        val res = wsUrl(url(99))
          .withHttpHeaders(fakeToken.asHeader)
          .withQueryStringParameters(
            "collectionIds" -> s"$archeologyCollection,$numismaticsCollection",
            "museumNo"      -> "555",
            "subNo"         -> "",
            "term"          -> "",
            "page"          -> "0",
            "limit"         -> "10"
          )
          .get()
          .futureValue

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
        val res = wsUrl(url(6))
          .withHttpHeaders(fakeToken.asHeader)
          .withQueryStringParameters(
            "collectionIds" -> "a4d768c8-2bf8-4a8f-8d7e-bc824b52b575",
            "museumNo"      -> "FOO6565",
            "subNo"         -> "",
            "term"          -> "",
            "page"          -> "1",
            "limit"         -> "3"
          )
          .get()
          .futureValue
          .status mustBe FORBIDDEN
      }

      "not allow searching in a collection without access" in {
        val res = wsUrl(url(99))
          .withHttpHeaders(fakeToken.asHeader)
          .withQueryStringParameters(
            "collectionIds" -> s"$numismaticsCollection,${UUID.randomUUID().toString}",
            "museumNo"      -> "L234",
            "subNo"         -> "",
            "term"          -> "",
            "page"          -> "1",
            "limit"         -> "10"
          )
          .get()
          .futureValue

        res.status mustBe FORBIDDEN
      }
    }

  }

}
