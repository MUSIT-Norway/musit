package no.uio.musit.service

import akka.stream.Materializer
import no.uio.musit.test.MusitSpecWithAppPerSuite
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest

import scala.concurrent.{ExecutionContext, Future}

class AccessLogFilterSpec extends MusitSpecWithAppPerSuite {

  implicit lazy val materializer: Materializer = app.materializer
  implicit lazy val ec: ExecutionContext       = app.actorSystem.dispatcher

  "The AccessLogFilter" should {

    """should set a "Processing-Time" header""" in {
      val filter  = new AccessLogFilter()
      val result  = filter(request => Future.successful(Ok))(FakeRequest())
      val headers = result.futureValue.header.headers
      headers.get("Processing-Time") must not be None
    }

  }

}
