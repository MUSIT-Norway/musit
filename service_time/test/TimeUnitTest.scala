/**
  * Created by ellenjo on 4/15/16.
  */

import no.uio.musit.microservice.time.domain._
import no.uio.musit.microservice.time.service.TimeService
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent.duration._

class TimeUnitTest extends PlaySpec with OneAppPerSuite with ScalaFutures with TimeService {

  val additionalConfiguration: Map[String, String] = Map.apply(
    ("slick.dbs.default.driver", "slick.driver.H2Driver$"),
    ("slick.dbs.default.db.driver", "org.h2.Driver"),
    ("slick.dbs.default.db.url", "jdbc:h2:mem:play-test"),
    ("evolutionplugin", "enabled")
  )
  val timeout = PatienceConfiguration.Timeout(1 seconds)
  implicit override lazy val app = new GuiceApplicationBuilder().configure(additionalConfiguration).build()

  "Testing method actionGetNow" must {


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
