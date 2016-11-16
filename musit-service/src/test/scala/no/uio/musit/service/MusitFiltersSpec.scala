package no.uio.musit.service

import akka.stream.Materializer
import no.uio.musit.test.MusitSpecWithAppPerSuite
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.test.FakeRequest
import play.api.mvc.Results._

import scala.concurrent.Future

class MusitFiltersSpec extends MusitSpecWithAppPerSuite {
  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  implicit lazy val materializer: Materializer = app.materializer

  "MusitFilters" when {

    "configured properly" should {

      "return a Cache-Control header" in {
        import scala.concurrent.ExecutionContext.Implicits.global
        val filter = new NoCacheFilter {}
        val result = filter(request => Future.successful(Ok))(FakeRequest())
        val headers = result.futureValue.header.headers
        headers.get("Cache-Control") mustBe Some("no-cache,no-store,max-age=0")
      }
    }
  }
}
