package controllers.reporting

import no.uio.musit.security.BearerToken
import no.uio.musit.test.{FakeUsers, MusitSpecWithServerPerSuite}
import play.api.test.Helpers._
import utils.testdata.NodeTestData

class StatsControllerSpec extends MusitSpecWithServerPerSuite with NodeTestData {

  val fakeToken1 = BearerToken(FakeUsers.testUserToken)
  val fakeToken2 = BearerToken(FakeUsers.superUserToken)

  "Calling the stats endpoint" should {
    "return stats for a node including objects per collection" in {

      val res = wsUrl(s"/museum/99/storagenodes/${nodeId4.asString}/stats")
        .withHeaders(fakeToken1.asHeader)
        .get()
        .futureValue

      res.status mustBe OK

      (res.json \ "numNodes").as[Int] mustBe 11
      (res.json \ "numObjects").as[Int] mustBe 4
      (res.json \ "totalObjects").as[Int] mustBe 52 // 2 are deleted
    }
  }

}
