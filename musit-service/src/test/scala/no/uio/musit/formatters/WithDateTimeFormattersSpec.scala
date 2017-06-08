package no.uio.musit.formatters

import no.uio.musit.time
import no.uio.musit.time.DefaultTimezone
import org.joda.time.DateTime
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.{Format, JsNumber, JsString, Json}

class WithDateTimeFormattersSpec extends WordSpec with MustMatchers {

  import DateTimeFormatters._

  "WithDateTimeFormatters" when {
    val now = time.dateTimeNow

    "using raw values" should {
      def toUtc(dt: DateTime) = dt.toDateTime(DefaultTimezone)

      "parse default iso format without millis" in {
        val nowStr = now.toString(defaultDateTimePattern)
        val parsed = JsString(nowStr).asOpt[DateTime]

        parsed.map(toUtc) mustBe Some(now.withMillisOfSecond(0))
      }

      "parse iso format with millis" in {
        val nowStr = now.toString(readDateTimeMillisPattern)
        val parsed = JsString(nowStr).asOpt[DateTime]

        parsed.map(toUtc) mustBe Some(now)
      }

      "parse epoch time in millis" in {
        val epochMillis = now.getMillis
        val parsed      = JsNumber(epochMillis).asOpt[DateTime]

        parsed.map(toUtc) mustBe Some(now)
      }
    }

    "using case class" should {

      def fooInUtc(f: Foo): Foo = f.copy(bar = f.bar.toDateTime(DefaultTimezone))

      "parse default iso format without millis" in {
        val nowStr = now.toString(defaultDateTimePattern)

        val fooJson = Json.obj("bar" -> JsString(nowStr))

        fooJson.asOpt[Foo].map(fooInUtc) mustBe Some(Foo(now.withMillisOfSecond(0)))
      }

      "parse iso format with millis" in {
        val nowStr  = now.toString(readDateTimeMillisPattern)
        val fooJson = Json.obj("bar" -> JsString(nowStr))

        fooJson.asOpt[Foo].map(fooInUtc) mustBe Some(Foo(now))
      }

      "parse epoch time in millis" in {
        val fooJson = Json.obj("bar" -> JsNumber(now.getMillis))

        fooJson.asOpt[Foo].map(fooInUtc) mustBe Some(Foo(now))
      }
    }
  }

}

case class Foo(bar: DateTime)
object Foo extends WithDateTimeFormatters {
  implicit val format: Format[Foo] = Json.format[Foo]
}
