/**
 * Created by jarle on 23.08.16.
 */
import java.sql.{ Date, Timestamp }

import no.uio.musit.microservice.event.domain.EventOrderingUtils
import no.uio.musit.microservice.event.domain.EventOrderingUtils._
import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
class DateOrderingTest extends PlaySpec {

  "testDateOrdering" must {
    "some sql-date tests, explicit specifying the ordering" in {

      val today = new java.sql.Date(DateTime.now().getMillis)
      val yesterday = new java.sql.Date(DateTime.now().getMillis - 24 * 60 * 60 * 1000)

      val optYesterday = Some(yesterday)
      val optToday = Some(today)

      val ordering = EventOrderingUtils.optionOrdering(sqlDateOrdering)
      ordering.compare(optYesterday, optToday) mustBe -1
      ordering.compare(optToday, optYesterday) mustBe 1
      ordering.compare(None, None) mustBe 0
      ordering.compare(None, optYesterday) mustBe -1
    }

    "some sql-date tests, less explicit with specifying the ordering" in {

      val today = new java.sql.Date(DateTime.now().getMillis)
      val yesterday = new java.sql.Date(DateTime.now().getMillis - 24 * 60 * 60 * 1000)

      val optYesterday = Some(yesterday)
      val optToday = Some(today)

      val ordering2 = EventOrderingUtils.optionOrdering[Date]
      ordering2.compare(optYesterday, optToday) mustBe -1
      ordering2.compare(optToday, optYesterday) mustBe 1
      ordering2.compare(None, None) mustBe 0
      ordering2.compare(None, optYesterday) mustBe -1
    }

    "some timestamp tests" in {

      val today = new Timestamp(DateTime.now().getMillis)
      val yesterday = new Timestamp(DateTime.now().getMillis - 24 * 60 * 60 * 1000)

      val optYesterday = Some(yesterday)
      val optToday = Some(today)

      val ordering = EventOrderingUtils.optionOrdering[Timestamp]
      ordering.compare(optYesterday, optToday) mustBe -1
      ordering.compare(optYesterday, optToday) mustBe -1
      ordering.compare(optToday, optYesterday) mustBe 1
      ordering.compare(None, None) mustBe 0
      ordering.compare(None, optYesterday) mustBe -1
    }

  }
}
