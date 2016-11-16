package no.uio.musit.service

import akka.stream.Materializer
import no.uio.musit.test.MusitSpecWithAppPerSuite
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class MusitFiltersSpec extends MusitSpecWithAppPerSuite {
  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  implicit lazy val materializer: Materializer = app.materializer

  "MusitFilters" when {

    "configured properly" should {

      "return a Cache-Control header" in {
        val filter = new NoCacheFilter {}
        val result = filter(request => Future.successful(Ok))(FakeRequest())
        val headers = result.futureValue.header.headers
        headers.get(CACHE_CONTROL) mustBe Some("no-cache,no-store,max-age=0")
      }
    }
  }
}
