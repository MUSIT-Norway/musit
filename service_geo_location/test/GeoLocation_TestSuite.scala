/**
  * Created by ellenjo on 4/15/16.
  */


import no.uio.musit.microservice.geoLocation.service.GeoLocationService
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent.duration._

class GeoLocation_TestSuite extends PlaySpec with OneAppPerSuite with ScalaFutures with GeoLocationService{

  val additionalConfiguration:Map[String, String] = Map.apply (
    ("slick.dbs.default.driver", "slick.driver.H2Driver$"),
    ("slick.dbs.default.db.driver" , "org.h2.Driver"),
    ("slick.dbs.default.db.url" , "jdbc:h2:mem:play-test"),
    ("evolutionplugin" , "enabled")
  )
  val timeout = PatienceConfiguration.Timeout(1 seconds)
  implicit override lazy val app = new GuiceApplicationBuilder().configure(additionalConfiguration).build()

  "GeoLocationService rest integration" must {

    "searchAddress" in {
      val svar = this.searchGeoNorway("paal bergs vei 56, RYKKINN")
      whenReady(svar, timeout) { geoAddresses =>
        assert (geoAddresses.length > 0)
      }
    }

  }

}
