package controllers

// common imports
import no.uio.musit.microservices.time.service.TimeService
import play.api.test.FakeRequest
import play.api.libs.json.Json
import org.scalatest.ParallelTestExecution
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._

// test imports
import no.uio.musit.microservices.common.domain.{ MusitError, MusitFilter }
import no.uio.musit.microservices.time.resource.TimeResource
import no.uio.musit.microservices.time.domain.MusitTime

class TimeControllerSpec extends PlaySpec with ScalaFutures with ParallelTestExecution {

  def createTimeResource = new TimeResource(new TimeService)

  "TimeController" must {
    "give date and time when provided a datetime filter" in {
      val futureResult = createTimeResource.now(Some(MusitFilter(List("date", "time"))), None).apply(FakeRequest())
      status(futureResult) mustBe OK
      val json = contentAsString(futureResult)
      val now = Json.parse(json).validate[MusitTime].get
      now.time must not be None
      now.date must not be None
    }

    "give date but not time when provided a date filter" in {
      val futureResult = createTimeResource.now(Some(MusitFilter(List("date"))), None)(FakeRequest())
      status(futureResult) mustBe OK
      val json = contentAsString(futureResult)
      val now = Json.parse(json).validate[MusitTime].get
      now.time mustBe None
      now.date must not be None
    }

    "give time but not date when provided a time filter" in {
      val futureResult = createTimeResource.now(Some(MusitFilter(List("time"))), None)(FakeRequest())
      status(futureResult) mustBe OK
      val json = contentAsString(futureResult)
      val now = Json.parse(json).validate[MusitTime].get
      now.date mustBe None
      now.time must not be None
    }

    "give date and time when provided no filter" in {
      val futureResult = createTimeResource.now(None, None)(FakeRequest())
      status(futureResult) mustBe OK
      val json = contentAsString(futureResult)
      val now = Json.parse(json).validate[MusitTime].get
      now.date must not be None
      now.time must not be None
    }

    "give error message when provided invalid filter" in {
      val futureResult = createTimeResource.now(Some(MusitFilter(List("uglepose"))), None)(FakeRequest())
      status(futureResult) mustBe BAD_REQUEST
      val json = contentAsString(futureResult)
      val now = Json.parse(json).validate[MusitError].get
      now.message mustBe "Only supports empty filter or filter on time, date or time and date"
    }
  }
}