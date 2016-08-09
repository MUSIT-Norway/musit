/**
 * Created by ellenjo on 4/15/16.
 */


import no.uio.musit.microservice.geoLocation.domain.GeoNorwayAddress
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.{JsError, JsSuccess, Json}

import scala.concurrent.duration._
import scala.language.postfixOps

class GeoLocation_TestSuite extends PlaySpec with OneServerPerSuite with ScalaFutures {

  // Extra long timeout since the integration call calls an external service
  val timeout = Timeout(10 seconds)

  "GeoLocation integration" must {
    "search for address" in {
      val future = wsUrl("/v1/address?search=Paal Bergsvei 56, Rykkinn").get()
      whenReady(future, timeout) { response =>
        Json.parse(response.body).validate[Seq[GeoNorwayAddress]] match {
          case JsSuccess(addresses, _) =>
            assert(addresses.head.street == "Paal Bergs vei")
          case JsError(err) =>
            fail(err.toString)
        }
      }
    }
  }

}
