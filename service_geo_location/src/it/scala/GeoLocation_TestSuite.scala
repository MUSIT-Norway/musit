/**
  * Created by ellenjo on 4/15/16.
  */


import no.uio.musit.microservice.geoLocation.domain.GeoNorwayAddress
import no.uio.musit.microservice.geoLocation.service.GeoLocationService
import no.uio.musit.microservices.common.PlayTestDefaults
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.{OneAppPerSuite, OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.libs.ws.WS

import scala.concurrent.duration._

class GeoLocation_TestSuite extends PlaySpec with OneServerPerSuite with ScalaFutures with GeoLocationService {

  // Extra long timeout since the integration call calls an external service
  val timeout = PlayTestDefaults.timeout.copy(value = 10 seconds)

  override lazy val port: Int = 19004

  "GeoLocationService rest integration" must {

    "searchAddress" in {
      val svar = this.searchGeoNorway("paal bergs vei 56, RYKKINN")
      whenReady(svar, timeout) { geoAddresses =>
        assert (geoAddresses.length > 0)
      }
    }

  }

  "GeoLocation integration " must {
    "get by id" in {
      val future = WS.url(s"http://localhost:$port/v1/address?search=Paal Bergsvei 56, Rykkinn").get()
      whenReady(future, timeout) { response =>
        val addresses = Json.parse(response.body).validate[Seq[GeoNorwayAddress]].get
        assert(addresses.head.street == "Paal Bergs vei")
      }
    }
  }

}
