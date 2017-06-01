package no.uio.musit.service

import akka.stream.Materializer
import no.uio.musit.test.MusitSpecWithAppPerSuite
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future

class NoCacheFilterSpec extends MusitSpecWithAppPerSuite {

  implicit lazy val materializer: Materializer = app.materializer

  "The NoCacheFilter" should {

    "set the Cache-Control header" in {
      val filter  = new NoCacheFilter()
      val result  = filter(request => Future.successful(Ok))(FakeRequest())
      val headers = result.futureValue.header.headers
      headers.get(CACHE_CONTROL) mustBe Some("no-cache,no-store,max-age=0")
    }
  }

}
