import no.uio.musit.microservice.time.resource.TimeResource_V1
import play.api.test.{FakeRequest, PlaySpecification}

import scala.util.{Failure, Success}
import play.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global

class TimeResourceSpec extends PlaySpecification {
  "TimeResource controller" should {
    "give OK when provided a datetime filter" in {
      val futureResult = new TimeResource_V1().actionGetNow(FakeRequest("GET", "someurl?filter=datetime"))
      val result = await(futureResult)
      result.header.status must equalTo(OK)
    }

    "give OK when provided a date filter" in {
      val futureResult = new TimeResource_V1().actionGetNow(FakeRequest("GET", "someurl?filter=date"))
      val result = await(futureResult)
      result.header.status must equalTo(OK)
    }

    "give OK when provided a time filter" in {
      val futureResult = new TimeResource_V1().actionGetNow(FakeRequest("GET", "someurl?filter=time"))
      val result = await(futureResult)
      result.header.status must equalTo(OK)
    }

    "give BadRequest when provided no filter" in {
      val futureResult = new TimeResource_V1().actionGetNow(FakeRequest("GET", "someurl"))
      val result = await(futureResult)
      result.header.status must equalTo(BAD_REQUEST)
    }
  }
}