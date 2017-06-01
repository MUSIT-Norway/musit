package controllers

import no.uio.musit.service.BuildInfo
import no.uio.musit.test.MusitSpecWithServerPerSuite
import play.api.test.Helpers._

class ApplicationIntegrationSpec extends MusitSpecWithServerPerSuite {

  "Calling services in the Application controller" should {

    "return the build info" in {
      val res = wsUrl("/buildinfo").get().futureValue
      res.status mustBe OK
      (res.json \ "name").as[String] mustBe BuildInfo.name
    }

  }

}
