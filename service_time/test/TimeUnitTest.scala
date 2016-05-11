/**
  * Created by ellenjo on 4/15/16.
  */

import no.uio.musit.microservice.time.domain._
import no.uio.musit.microservice.time.service.TimeService
import org.scalatestplus.play.PlaySpec

class TimeUnitTest extends PlaySpec with TimeService {

  "Testing TimeService" must {


    "get now Date" in {
      val now = getNow(Some(MusitDateFilter()))
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
      val now = getNow(Some(MusitDateTimeFilter()))
      assert(now.isInstanceOf[DateTime])
      assert(now.asInstanceOf[DateTime].date != null)
      assert(now.asInstanceOf[DateTime].time != null)
    }


    "get now Time" in {
      val now = getNow(Some(MusitTimeFilter()))
      assert(now.isInstanceOf[Time])
      assert(now.asInstanceOf[Time].time != null)
    }

    "get now Some(null)" in {
      val thrown = intercept[IllegalArgumentException] {
        val now = getNow(Some(null))
        assert(now.isInstanceOf[DateTime])
        assert(now.asInstanceOf[DateTime].date != null)
        assert(now.asInstanceOf[DateTime].time != null)
      }
      assert(thrown != null)
    }

    "get now null" in {
      val thrown = intercept[IllegalArgumentException] {
        val now = getNow(null)
        assert(now.isInstanceOf[DateTime])
        assert(now.asInstanceOf[DateTime].date != null)
        assert(now.asInstanceOf[DateTime].time != null)
      }
      assert(thrown != null)
    }



  }

}
