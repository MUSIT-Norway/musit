package no.uio.musit.time

import java.time.{ZoneId, ZoneOffset}

import no.uio.musit.time.Implicits._
import org.scalatest.{MustMatchers, WordSpec}

class DateTimeImplicitsSpec extends WordSpec with MustMatchers {

  "Converting between DateTime and java.sql.Timezone" should {
    "result in correct values" in {

      val dt1 = dateTimeNow
      val ts  = dateTimeToJTimestamp(dt1)

      val zdt = ts.toInstant.atZone(ZoneId.ofOffset("UTC", ZoneOffset.ofHours(1)))

      val dt2 = jSqlTimestampToDateTime(ts)

      dt2 mustBe dt1
      zdt.getDayOfYear mustBe dt1.getDayOfYear
      zdt.getHour mustBe dt1.hourOfDay().get() + 1
    }
  }

}
