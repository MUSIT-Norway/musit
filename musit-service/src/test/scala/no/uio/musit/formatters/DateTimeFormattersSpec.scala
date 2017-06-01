package no.uio.musit.formatters

import no.uio.musit.formatters.DateTimeFormatters.dateTimeFormatter
import org.joda.time.DateTime
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json._

class DateTimeFormattersSpec extends WordSpec with MustMatchers with OptionValues {

  val dateString = "2017-01-13T10:46:32+00:00"
  val dateJson   = JsString("2017-01-13T10:46:32+00:00")
  val dateTime   = new DateTime(2017, 1, 13, 10, 46, 32, 0)

  val badDateJson = JsString("13-01-2017T10:46:32+00:00")

  "Formatting dates" should {

    "successfully generate a JSON date" in {
      Json.toJson(dateTime)(dateTimeFormatter).as[String] mustBe dateString
    }

    "parse ISO-8601 dates correctly" in {
      val jsd = Json.fromJson(dateJson)(dateTimeFormatter).asOpt
      jsd must not be None
      jsd.value mustBe dateTime
    }

    "fail when trying to parse an incorrect date JS String" in {
      Json.fromJson(badDateJson)(dateTimeFormatter).isError mustBe true
    }

  }
}
