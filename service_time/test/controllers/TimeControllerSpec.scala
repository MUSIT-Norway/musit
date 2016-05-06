package controllers

import play.api.test.{FakeRequest, PlaySpecification}
import play.api.libs.json.Json
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import no.uio.musit.microservices.time.resource.TimeResource
import no.uio.musit.microservices.time.domain.MusitTime
import no.uio.musit.microservices.time.domain.MusitError
import no.uio.musit.microservices.time.domain.MusitFilter

@RunWith(classOf[JUnitRunner])
class TimeControllerSpec extends PlaySpecification {
  "TimeController" should {
    "give date and time when provided a datetime filter" in {
      val futureResult = new TimeResource().now(Some(MusitFilter(List("date", "time"))), None).apply(FakeRequest())
      status(futureResult) must equalTo(OK)
      val json = contentAsString(futureResult)
      val now = Json.parse(json).validate[MusitTime].get
      now.time must not be None
      now.date must not be None
    }

    "give date but not time when provided a date filter" in {
      val futureResult = new TimeResource().now(Some(MusitFilter(List("date"))), None)(FakeRequest())
      status(futureResult) must equalTo(OK)
      val json = contentAsString(futureResult)
      val now = Json.parse(json).validate[MusitTime].get
      now.time must beNone
      now.date must not be None
    }

    "give time but not date when provided a time filter" in {
      val futureResult = new TimeResource().now(Some(MusitFilter(List("time"))), None)(FakeRequest())
      status(futureResult) must equalTo(OK)
      val json = contentAsString(futureResult)
      val now = Json.parse(json).validate[MusitTime].get
      now.date must beNone
      now.time must not be None
    }

    "give date and time when provided no filter" in {
      val futureResult = new TimeResource().now(None, None)(FakeRequest())
      status(futureResult) must equalTo(OK)
      val json = contentAsString(futureResult)
      val now = Json.parse(json).validate[MusitTime].get
      now.date must not be None
      now.time must not be None
    }

    "give error message when provided invalid filter" in {
      val futureResult = new TimeResource().now(Some(MusitFilter(List("uglepose"))), None)(FakeRequest())
      status(futureResult) must equalTo(BAD_REQUEST)
      val json = contentAsString(futureResult)
      val now = Json.parse(json).validate[MusitError].get
      now.message must equalTo("Only supports empty filter or filter on time, date or time and date")
    }
  }
}