/**
 * Created by jarle on 23.08.16.
 */
import java.sql.Timestamp
import java.sql.Date

import no.uio.musit.microservice.event.domain.BaseEventDto
import no.uio.musit.microservice.event.service.EventOrderingUtils
import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
class DateOrderingTest extends PlaySpec {

  "testDateOrdering" must {
    "some date tests must succeed" in {

      val today = new java.sql.Date(DateTime.now().getMillis)
      val yesterday = new java.sql.Date(DateTime.now().getMillis - 24 * 60 * 60 * 1000)

      val optYesterday = Some(yesterday)
      val optToday = Some(today)

      val ordering = EventOrderingUtils.dateOrdering[Option[Date]]
      ordering.compare(optYesterday, optToday) mustBe -1
      ordering.compare(optYesterday, optToday) mustBe -1
      ordering.compare(optToday, optYesterday) mustBe 1
      ordering.compare(None, None) mustBe 0
      ordering.compare(None, optYesterday) mustBe -1
    }

    "some timestamp tests must succeed" in {

      val today = new Timestamp(DateTime.now().getMillis)
      val yesterday = new Timestamp(DateTime.now().getMillis - 24 * 60 * 60 * 1000)

      val optYesterday = Some(yesterday)
      val optToday = Some(today)

      val ordering = EventOrderingUtils.timestampOrdering[Option[Timestamp]]
      ordering.compare(optYesterday, optToday) mustBe -1
      ordering.compare(optYesterday, optToday) mustBe -1
      ordering.compare(optToday, optYesterday) mustBe 1
      ordering.compare(None, None) mustBe 0
      ordering.compare(None, optYesterday) mustBe -1
    }

  }
}
