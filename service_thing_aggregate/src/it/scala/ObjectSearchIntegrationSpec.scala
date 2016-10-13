/**
 * Created by jarle on 05.10.16.
 */
import com.google.inject.Inject
import dao.ObjectSearchDao
import models.{MuseumIdentifier, MusitThing, ObjectAggregation, ObjectId}
import no.uio.musit.test.MusitSpecWithServerPerSuite
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.libs.json.Json
import play.utils.UriEncoding

import scala.language.postfixOps

class ObjectSearchIntegrationSpec @Inject() ( //    val dao: ObjectSearchDao
) extends MusitSpecWithServerPerSuite {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  def doSearch(museumId: Int, museumNo: String, subNo: String, term: String, page: Int = 1, limit: Int = 25) = {
    def encode(str: String) = UriEncoding.encodePathSegment(str, "utf-8")
    var url =
      s"""/museum/$museumId/objects/search?museumNo=${encode(museumNo)}&subNo=${encode(subNo)}""" +
        s"""&term=${encode(term)}&page=$page&limit=$limit"""
    wsUrl(url).get().futureValue
  }

  "ObjectSearch" must {
    "find an object which exists, via museumNo" in {

      val res = doSearch(1, "C666", "", "")
      res.status mustBe 200
      res.body must include("C666")
      res.body must include("Øks")
    }
  }

}

