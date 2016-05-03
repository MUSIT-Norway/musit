/**
  * Created by ellenjo on 4/15/16.
  */


import no.uio.musit.microservice.geoLocation.service.GeoLocationService
import no.uio.musit.microservices.common.PlayTestDefaults
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent.duration._

class GeoLocation_TestSuite extends PlaySpec with OneAppPerSuite with ScalaFutures with GeoLocationService{

  val timeout = PlayTestDefaults.timeout
  implicit override lazy val app = new GuiceApplicationBuilder().configure(PlayTestDefaults.inMemoryDatabaseConfig).build()

  "GeoLocationService rest integration" must {

    "searchAddress" in {
      val svar = this.searchGeoNorway("paal bergs vei 56, RYKKINN")
      whenReady(svar, timeout) { geoAddresses =>
        assert (geoAddresses.length > 0)
      }
    }

  }

}
