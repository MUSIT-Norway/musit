import com.google.inject.Inject
import no.uio.musit.test.MusitSpecWithServerPerSuite
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.libs.json.JsArray
import play.api.libs.ws.WSResponse
import play.utils.UriEncoding

class ObjectSearchIntegrationSpec @Inject()(
                                           ) extends MusitSpecWithServerPerSuite {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = Span(15, Seconds),
    interval = Span(50, Millis)
  )

  def doSearch(
                museumId: Int,
                museumNo: String,
                subNo: String,
                term: String,
                page: Int = 1,
                limit: Int = 25
              ): WSResponse = {
    val url = s"""/museum/$museumId/objects/search"""
    wsUrl(url).withQueryString(
      "museumNo" -> museumNo,
      "subNo" -> subNo,
      "term" -> term,
      "page" -> s"$page",
      "limit" -> s"$limit"
    ).get().futureValue
  }


  "ObjectSearch" must {
    "find an object which exists, via museumNo" in {

      val res = doSearch(1, "C666", "", "")
      res.status mustBe 200
      res.body must include("C666")
      res.body must include("Øks")
    }

    "find an object which exists, via museumNo, subNr and term" in {

      val res = doSearch(1, "C666", "34", "Øks")
      res.status mustBe 200
      res.body must include("C666")
      res.body must include("Øks")
    }

    "Do not find objects which doesn't exist" in {

      val res = doSearch(1, "ThisIsNotAMuseumNr", "NotASubNr", "NotATerm")
      res.status mustBe 200
      res.json mustBe JsArray(Seq.empty)
    }


    "find an object which exists, via museumNo, subNr and term even with invalid limit and pageno" in {

      //Arbitrarily use negative (invalid) values for limit (and it should still work as if the values were correct)
      val res = doSearch(1, "C666", "34", "Øks", -1000, -1000)
      res.status mustBe 200
      res.body must include("C666")
      res.body must include("Øks")
    }

  }


}

