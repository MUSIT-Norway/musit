package controllers.delphi

import no.uio.musit.security.BearerToken
import no.uio.musit.test.{FakeUsers, MusitSpecWithServerPerSuite}
import play.api.libs.json.{JsArray, JsNumber, Json}
import play.api.test.Helpers._
import utils.testdata.NodeTestData

class DelphiBridgeControllerIntegrationSpec
    extends MusitSpecWithServerPerSuite
    with NodeTestData {

  val fakeToken = BearerToken(FakeUsers.testUserToken)

  val archeologyCollection  = "a4d768c8-2bf8-4a8f-8d7e-bc824b52b575"
  val numismaticsCollection = "8ea5fa45-b331-47ee-a583-33cd0ca92c82"

  "The DelphiBridgeController" when {

    "finding the current location for an old object ID and schema" should {

      "return the objects current nodeId and path location" in {
        val expectedLocation = "Utviklingsmuseet, Utviklingsmuseet Org, " +
          "Forskningens hus, Naturværelset"
        val res = wsUrl(s"/delphi/objects/111")
          .withHttpHeaders(fakeToken.asHeader)
          .withQueryStringParameters("schemaName" -> "USD_ARK_GJENSTAND_O")
          .get()
          .futureValue

        res.status mustBe OK
        (res.json \ "nodeId").as[String] mustBe nodeId6.asString
        (res.json \ "currentLocation").as[String] mustBe expectedLocation
      }

    }

    "listing all external nodes" should {
      "return a list of all nodes below the RootLoan node sorted by name" in {
        val expectedNames = List(
          "British museum",
          "Death Star gallery",
          "FooBar of History",
          "The Louvre",
          "Utenfor 2",
          "Utenfor museet"
        )

        val res = wsUrl(s"/delphi/museum/99/nodes/external")
          .withHttpHeaders(fakeToken.asHeader)
          .get()
          .futureValue

        res.status mustBe OK
        val rl = res.json.as[JsArray].value.toList
        rl.size mustBe 6
        rl.map(js => (js \ "name").as[String]) mustBe expectedNames

      }
    }

    "translating old objectIds in a schema" should {
      "return a list of the new ObjectIds" in {
        val expected = (11 to 20).toList

        val in = Json.obj(
          "schemaName"   -> "USD_ARK_GJENSTAND_O",
          "oldObjectIds" -> JsArray((110 to 120).map(i => JsNumber(i)))
        )

        val res = wsUrl(s"/delphi/objects/tranlsate_old_ids")
          .withHttpHeaders(fakeToken.asHeader)
          .put(in)
          .futureValue

        res.status mustBe OK
        val rl = res.json.as[Seq[Long]].toList
        rl.size mustBe 10
        rl mustBe expected
      }
    }

  }

}
