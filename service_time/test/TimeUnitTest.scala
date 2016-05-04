/**
  * Created by ellenjo on 4/15/16.
  */

import no.uio.musit.microservice.time.domain._
import no.uio.musit.microservice.time.service.TimeService
import org.scalatestplus.play.PlaySpec

class TimeUnitTest extends PlaySpec with TimeService {

  "Testing TimeService" must {

    "get musit datetime filter" in {
      val filter = getMusitFilter("datetime")
      assert(filter.isDefined)
      assert(filter.get == MusitDateTimeFilter)
    }

    "get musit date filter" in {
      val filter = getMusitFilter("date")
      assert(filter.isDefined)
      assert(filter.get == MusitDateFilter)
    }

    "get musit time filter" in {
      val filter = getMusitFilter("time")
      assert(filter.isDefined)
      assert(filter.get == MusitTimeFilter)
    }

    "get musit null filter" in {
      // FIXME this is silly, really, but shows that the method is resilient. Should be removed when we agree to stop passing around nulls
      val filter = getMusitFilter(null)
      assert(filter.isEmpty)
    }

    "get musit bad filter" in {
      val thrown = intercept[IllegalArgumentException] {
        getMusitFilter("uglepose")
      }
      assert(thrown != null)
    }

    "get sorted datetime filters" in {
      val sortedFilters = getSortedFilters("datetime")
      assert(sortedFilters != null)
      assert(sortedFilters.length == 2)
      assert(sortedFilters.head == "date")
      assert(sortedFilters.last == "time")
    }

    "get sorted date filters" in {
      val sortedFilters = getSortedFilters("date")
      assert(sortedFilters != null)
      assert(sortedFilters.length == 1)
      assert(sortedFilters.head == "date")
    }

    "get sorted time filters" in {
      val sortedFilters = getSortedFilters("time")
      assert(sortedFilters != null)
      assert(sortedFilters.length == 1)
      assert(sortedFilters.head == "time")
    }

    "get sorted null filters" in {
      val sortedFilters = getSortedFilters(null)
      assert(sortedFilters != null)
      assert(sortedFilters.length == 0)
    }

    "get now Date" in {
      val now = getNow(Some(MusitDateFilter))
      assert(now.isInstanceOf[Date])
      assert(now.asInstanceOf[Date].date != null)
    }

    "get now None" in {
      val now = getNow(None)
      assert(now.isInstanceOf[DateTime])
      assert(now.asInstanceOf[DateTime].date != null)
      assert(now.asInstanceOf[DateTime].time != null)
    }

    "get now DateTime" in {
      val now = getNow(Some(MusitDateTimeFilter))
      assert(now.isInstanceOf[DateTime])
      assert(now.asInstanceOf[DateTime].date != null)
      assert(now.asInstanceOf[DateTime].time != null)
    }

    "get now Time" in {
      val now = getNow(Some(MusitTimeFilter))
      assert(now.isInstanceOf[Time])
      assert(now.asInstanceOf[Time].time != null)
    }
  }

}
